/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.fineract.cn.individuallending.internal.service;

import org.apache.fineract.cn.individuallending.api.v1.domain.caseinstance.ChargeName;
import org.apache.fineract.cn.individuallending.api.v1.domain.caseinstance.PlannedPayment;
import org.apache.fineract.cn.individuallending.api.v1.domain.caseinstance.PlannedPaymentPage;
import org.apache.fineract.cn.individuallending.api.v1.domain.product.AccountDesignators;
import org.apache.fineract.cn.individuallending.api.v1.domain.workflow.Action;
import org.apache.fineract.cn.individuallending.internal.service.costcomponent.CostComponentService;
import org.apache.fineract.cn.individuallending.internal.service.costcomponent.PaymentBuilder;
import org.apache.fineract.cn.individuallending.internal.service.costcomponent.SimulatedRunningBalances;
import org.apache.fineract.cn.individuallending.internal.service.schedule.Period;
import org.apache.fineract.cn.individuallending.internal.service.schedule.ScheduledAction;
import org.apache.fineract.cn.individuallending.internal.service.schedule.ScheduledActionHelpers;
import org.apache.fineract.cn.individuallending.internal.service.schedule.ScheduledCharge;
import org.apache.fineract.cn.individuallending.internal.service.schedule.ScheduledChargeComparator;
import org.apache.fineract.cn.individuallending.internal.service.schedule.ScheduledChargesService;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import org.apache.fineract.cn.lang.ServiceException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * @author Myrle Krantz
 */
@Service
public class IndividualLoanService {
  private final ScheduledChargesService scheduledChargesService;

  public static class PlannedPaymentWindow {
    final int pageIndex;
    final int size;
    @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
    final Optional<LocalDate> requestedInitialDisbursalDate;

    public PlannedPaymentWindow(
        final int pageIndex,
        final int size,
        @SuppressWarnings("OptionalUsedAsFieldOrParameterType") final Optional<LocalDate> requestedInitialDisbursalDate) {
      this.pageIndex = pageIndex;
      this.size = size;
      this.requestedInitialDisbursalDate = requestedInitialDisbursalDate;
    }
  }

  @Autowired
  public IndividualLoanService(final ScheduledChargesService scheduledChargesService) {
    this.scheduledChargesService = scheduledChargesService;
  }

  public PlannedPaymentPage getPlannedPaymentsPage(
      final DataContextOfAction dataContextOfAction,
      final PlannedPaymentWindow plannedPaymentWindow) {
    final int minorCurrencyUnitDigits = dataContextOfAction.getProductEntity().getMinorCurrencyUnitDigits();
    final LocalDate initialDisbursalDate = plannedPaymentWindow.requestedInitialDisbursalDate
        .orElse(Optional.ofNullable(dataContextOfAction.getCustomerCaseEntity().getStartOfTerm()).map(LocalDateTime::toLocalDate)
            .orElseGet(() -> LocalDate.now(ZoneId.of("UTC"))));

    final List<ScheduledAction> scheduledActions = ScheduledActionHelpers.getHypotheticalScheduledActions(initialDisbursalDate, dataContextOfAction.getCaseParameters());

    final Set<Action> actionsScheduled = scheduledActions.stream().map(ScheduledAction::getAction).collect(Collectors.toSet());

    final List<ScheduledCharge> scheduledCharges = scheduledChargesService.getScheduledCharges(dataContextOfAction.getProductEntity().getIdentifier(), scheduledActions);

    final Optional<BigDecimal> persistedPaymentSize = dataContextOfAction.getPaymentSize();

    final BigDecimal loanPaymentSize = persistedPaymentSize.orElseGet(() ->
        CostComponentService.getLoanPaymentSize(
            dataContextOfAction.getCaseParametersEntity().getBalanceRangeMaximum(),
            dataContextOfAction.getCaseParametersEntity().getBalanceRangeMaximum(),
            dataContextOfAction.getInterest(),
            minorCurrencyUnitDigits,
            scheduledCharges));

    final List<PlannedPayment> plannedPaymentsElements = getPlannedPaymentsElements(
        dataContextOfAction.getCaseParametersEntity().getBalanceRangeMaximum(),
        minorCurrencyUnitDigits,
        actionsScheduled,
        scheduledCharges,
        loanPaymentSize,
        dataContextOfAction.getInterest());

    final Set<ChargeName> chargeNames = scheduledCharges.stream()
            .map(IndividualLoanService::chargeNameFromChargeDefinition)
            .collect(Collectors.toSet());

    return constructPage(plannedPaymentWindow.pageIndex, plannedPaymentWindow.size, plannedPaymentsElements, chargeNames);
  }

  private static PlannedPaymentPage constructPage(
          final int pageIndex,
          final int size,
          final List<PlannedPayment> plannedPaymentsElements,
          final Set<ChargeName> chargeNames) {
    final int fromIndex = size*pageIndex;
    final int toIndex = Math.min(size*(pageIndex+1), plannedPaymentsElements.size());
    if (toIndex < fromIndex)
      throw ServiceException.badRequest("Page number ''{0}'' out of range.", pageIndex);
    final List<PlannedPayment> elements = plannedPaymentsElements.subList(fromIndex, toIndex);

    final PlannedPaymentPage ret = new PlannedPaymentPage();
    ret.setElements(elements);
    ret.setChargeNames(chargeNames);
    ret.setTotalElements((long) plannedPaymentsElements.size());
    final int partialPage = Math.floorMod(plannedPaymentsElements.size(), size) == 0 ? 0 : 1;
    ret.setTotalPages(Math.floorDiv(plannedPaymentsElements.size(), size)+ partialPage);

    return ret;
  }

  private static ChargeName chargeNameFromChargeDefinition(final ScheduledCharge scheduledCharge) {
    return new ChargeName(scheduledCharge.getChargeDefinition().getIdentifier(), scheduledCharge.getChargeDefinition().getName());
  }

  static private List<PlannedPayment> getPlannedPaymentsElements(
      final BigDecimal initialBalance,
      final int minorCurrencyUnitDigits,
      final Set<Action> actionsScheduled,
      final List<ScheduledCharge> scheduledCharges,
      final BigDecimal loanPaymentSize,
      final BigDecimal interest) {
    final Map<Period, SortedSet<ScheduledCharge>> orderedScheduledChargesGroupedByPeriod
        = scheduledCharges.stream()
        .filter(scheduledCharge -> chargeIsNotAccruedOrAccruesAtActionScheduled(actionsScheduled, scheduledCharge))
        .collect(Collectors.groupingBy(IndividualLoanService::getPeriodFromScheduledCharge,
            Collectors.mapping(x -> x,
                Collector.of(
                    () -> new TreeSet<>(new ScheduledChargeComparator()),
                    SortedSet::add,
                    (left, right) -> { left.addAll(right); return left; }))));

    final List<Period> sortedRepaymentPeriods
        = orderedScheduledChargesGroupedByPeriod.keySet().stream()
        .sorted()
        .collect(Collector.of(ArrayList::new, List::add, (left, right) -> { left.addAll(right); return left; }));

    final SimulatedRunningBalances balances = new SimulatedRunningBalances();
    final List<PlannedPayment> plannedPayments = new ArrayList<>();
    for (int i = 0; i < sortedRepaymentPeriods.size(); i++)
    {
      final Period repaymentPeriod = sortedRepaymentPeriods.get(i);
      final BigDecimal requestedRepayment;
      final BigDecimal requestedDisbursal;
      if (i == 0)
      { //First "period" is actually just the OPEN/APPROVE/DISBURSAL action set.
        requestedRepayment = BigDecimal.ZERO;
        requestedDisbursal = initialBalance.setScale(minorCurrencyUnitDigits, BigDecimal.ROUND_HALF_EVEN);
      }
      else if (i == sortedRepaymentPeriods.size() - 1)
      { //Last repayment period: Fill the proposed payment out to the remaining balance of the loan.
        requestedRepayment = loanPaymentSize.multiply(BigDecimal.valueOf(2));
        requestedDisbursal = BigDecimal.ZERO;
      }
      else {
        requestedRepayment = loanPaymentSize;
        requestedDisbursal = BigDecimal.ZERO;
      }

      balances.adjustBalance(AccountDesignators.ENTRY, requestedRepayment);

      final SortedSet<ScheduledCharge> scheduledChargesInPeriod = orderedScheduledChargesGroupedByPeriod.get(repaymentPeriod);
      final PaymentBuilder paymentBuilder =
              CostComponentService.getCostComponentsForScheduledCharges(
                  scheduledChargesInPeriod,
                  initialBalance,
                  balances,
                  loanPaymentSize,
                  requestedDisbursal,
                  requestedRepayment,
                  interest,
                  minorCurrencyUnitDigits,
                  true);

      plannedPayments.add(paymentBuilder.accumulatePlannedPayment(balances, repaymentPeriod.getEndDate()));
    }
    return plannedPayments;
  }

  private static boolean chargeIsNotAccruedOrAccruesAtActionScheduled(
      final Set<Action> actionsScheduled,
      final ScheduledCharge scheduledCharge) {
    // For example to prevent late charges from showing up on planned payments.
    return scheduledCharge.getChargeDefinition().getAccrueAction() == null ||
        actionsScheduled.contains(Action.valueOf(scheduledCharge.getChargeDefinition().getAccrueAction()));
  }

  private static Period getPeriodFromScheduledCharge(final ScheduledCharge scheduledCharge) {
    final ScheduledAction scheduledAction = scheduledCharge.getScheduledAction();
    if (ScheduledActionHelpers.actionHasNoActionPeriod(scheduledAction.getAction()))
      return new Period(null, null);
    else
      return scheduledAction.getRepaymentPeriod();
  }
}