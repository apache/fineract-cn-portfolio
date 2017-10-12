/*
 * Copyright 2017 The Mifos Initiative.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.mifos.individuallending.internal.service;

import io.mifos.core.lang.ServiceException;
import io.mifos.individuallending.api.v1.domain.caseinstance.ChargeName;
import io.mifos.individuallending.api.v1.domain.caseinstance.PlannedPayment;
import io.mifos.individuallending.api.v1.domain.caseinstance.PlannedPaymentPage;
import io.mifos.individuallending.api.v1.domain.product.AccountDesignators;
import io.mifos.individuallending.api.v1.domain.workflow.Action;
import io.mifos.individuallending.internal.service.costcomponent.CostComponentService;
import io.mifos.individuallending.internal.service.costcomponent.PaymentBuilder;
import io.mifos.individuallending.internal.service.costcomponent.SimulatedRunningBalances;
import io.mifos.individuallending.internal.service.schedule.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.Nonnull;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collector;
import java.util.stream.Collectors;

/**
 * @author Myrle Krantz
 */
@Service
public class IndividualLoanService {
  private final ScheduledChargesService scheduledChargesService;

  @Autowired
  public IndividualLoanService(final ScheduledChargesService scheduledChargesService) {
    this.scheduledChargesService = scheduledChargesService;
  }

  public PlannedPaymentPage getPlannedPaymentsPage(
      final DataContextOfAction dataContextOfAction,
      final int pageIndex,
      final int size,
      final @Nonnull LocalDate initialDisbursalDate) {
    final int minorCurrencyUnitDigits = dataContextOfAction.getProductEntity().getMinorCurrencyUnitDigits();

    final List<ScheduledAction> scheduledActions = ScheduledActionHelpers.getHypotheticalScheduledActions(initialDisbursalDate, dataContextOfAction.getCaseParameters());

    final Set<Action> actionsScheduled = scheduledActions.stream().map(ScheduledAction::getAction).collect(Collectors.toSet());

    final List<ScheduledCharge> scheduledCharges = scheduledChargesService.getScheduledCharges(dataContextOfAction.getProductEntity().getIdentifier(), scheduledActions);

    final BigDecimal loanPaymentSize = CostComponentService.getLoanPaymentSize(
        dataContextOfAction.getCaseParametersEntity().getBalanceRangeMaximum(),
        dataContextOfAction.getCaseParametersEntity().getBalanceRangeMaximum(),
        dataContextOfAction.getInterest(),
        minorCurrencyUnitDigits,
        scheduledCharges);

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

    return constructPage(pageIndex, size, plannedPaymentsElements, chargeNames);
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