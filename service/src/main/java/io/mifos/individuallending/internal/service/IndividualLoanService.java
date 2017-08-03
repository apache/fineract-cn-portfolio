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

import io.mifos.individuallending.api.v1.domain.caseinstance.CaseParameters;
import io.mifos.individuallending.api.v1.domain.caseinstance.ChargeName;
import io.mifos.individuallending.api.v1.domain.caseinstance.PlannedPayment;
import io.mifos.individuallending.api.v1.domain.caseinstance.PlannedPaymentPage;
import io.mifos.individuallending.api.v1.domain.product.ChargeProportionalDesignator;
import io.mifos.portfolio.api.v1.domain.ChargeDefinition;
import io.mifos.portfolio.api.v1.domain.Product;
import io.mifos.portfolio.service.internal.service.ChargeDefinitionService;
import io.mifos.portfolio.service.internal.service.ProductService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.Nonnull;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @author Myrle Krantz
 */
@Service
public class IndividualLoanService {
  private final ProductService productService;
  private final ChargeDefinitionService chargeDefinitionService;

  @Autowired
  public IndividualLoanService(final ProductService productService,
                               final ChargeDefinitionService chargeDefinitionService) {
    this.productService = productService;
    this.chargeDefinitionService = chargeDefinitionService;
  }

  public PlannedPaymentPage getPlannedPaymentsPage(
          final String productIdentifier,
          final CaseParameters caseParameters,
          final int pageIndex,
          final int size,
          final @Nonnull LocalDate initialDisbursalDate) {
    final Product product = productService.findByIdentifier(productIdentifier)
            .orElseThrow(() -> new IllegalArgumentException("Non-existent product identifier."));
    final int minorCurrencyUnitDigits = product.getMinorCurrencyUnitDigits();

    final List<ScheduledAction> scheduledActions = ScheduledActionHelpers.getHypotheticalScheduledActions(initialDisbursalDate, caseParameters);

    final List<ScheduledCharge> scheduledCharges = getScheduledCharges(productIdentifier, scheduledActions);

    final BigDecimal loanPaymentSize = CostComponentService.getLoanPaymentSize(
        caseParameters.getMaximumBalance(),
        minorCurrencyUnitDigits,
        scheduledCharges);

    final List<PlannedPayment> plannedPaymentsElements = getPlannedPaymentsElements(
        caseParameters.getMaximumBalance(),
        minorCurrencyUnitDigits,
        scheduledCharges,
        loanPaymentSize);

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

  List<ScheduledCharge> getScheduledCharges(
      final String productIdentifier,
      final @Nonnull List<ScheduledAction> scheduledActions) {
    final Map<String, List<ChargeDefinition>> chargeDefinitionsMappedByChargeAction
            = chargeDefinitionService.getChargeDefinitionsMappedByChargeAction(productIdentifier);

    final Map<String, List<ChargeDefinition>> chargeDefinitionsMappedByAccrueAction
            = chargeDefinitionService.getChargeDefinitionsMappedByAccrueAction(productIdentifier);

    return getScheduledCharges(
            scheduledActions,
            chargeDefinitionsMappedByChargeAction,
            chargeDefinitionsMappedByAccrueAction);
  }

  static private List<PlannedPayment> getPlannedPaymentsElements(
      final BigDecimal initialBalance,
      final int minorCurrencyUnitDigits,
      final List<ScheduledCharge> scheduledCharges,
      final BigDecimal loanPaymentSize) {
    final Map<Period, SortedSet<ScheduledCharge>> orderedScheduledChargesGroupedByPeriod
        = scheduledCharges.stream()
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

    BigDecimal balance = initialBalance.setScale(minorCurrencyUnitDigits, BigDecimal.ROUND_HALF_EVEN);
    final List<PlannedPayment> plannedPayments = new ArrayList<>();
    for (final Period repaymentPeriod : sortedRepaymentPeriods)
    {
      final BigDecimal currentLoanPaymentSize;
      if (repaymentPeriod.isDefined()) {
        currentLoanPaymentSize = loanPaymentSize;
      }
      else
        currentLoanPaymentSize = BigDecimal.ZERO;

      final SortedSet<ScheduledCharge> scheduledChargesInPeriod = orderedScheduledChargesGroupedByPeriod.get(repaymentPeriod);
      final CostComponentsForRepaymentPeriod costComponentsForRepaymentPeriod =
              CostComponentService.getCostComponentsForScheduledCharges(
                  Collections.emptyMap(),
                  scheduledChargesInPeriod,
                  balance,
                  balance,
                  currentLoanPaymentSize,
                  minorCurrencyUnitDigits,
                  false);

      final PlannedPayment plannedPayment = new PlannedPayment();
      plannedPayment.setCostComponents(new ArrayList<>(costComponentsForRepaymentPeriod.getCostComponents().values()));
      plannedPayment.setDate(repaymentPeriod.getEndDateAsString());
      balance = balance.add(costComponentsForRepaymentPeriod.getBalanceAdjustment());
      plannedPayment.setRemainingPrincipal(balance);
      plannedPayments.add(plannedPayment);
    }
    return plannedPayments;
  }

  private static Period getPeriodFromScheduledCharge(final ScheduledCharge scheduledCharge) {
    final ScheduledAction scheduledAction = scheduledCharge.getScheduledAction();
    if (ScheduledActionHelpers.actionHasNoActionPeriod(scheduledAction.action))
      return new Period(null, null);
    else
      return scheduledAction.repaymentPeriod;
  }

  private List<ScheduledCharge> getScheduledCharges(final List<ScheduledAction> scheduledActions,
                                                    final Map<String, List<ChargeDefinition>> chargeDefinitionsMappedByChargeAction,
                                                    final Map<String, List<ChargeDefinition>> chargeDefinitionsMappedByAccrueAction) {
    return scheduledActions.stream()
        .flatMap(scheduledAction ->
            getChargeDefinitionStream(
                chargeDefinitionsMappedByChargeAction,
                chargeDefinitionsMappedByAccrueAction,
                scheduledAction)
                .map(chargeDefinition -> new ScheduledCharge(scheduledAction, chargeDefinition)))
        .collect(Collectors.toList());
  }

  private Stream<ChargeDefinition> getChargeDefinitionStream(
          final Map<String, List<ChargeDefinition>> chargeDefinitionsMappedByChargeAction,
          final Map<String, List<ChargeDefinition>> chargeDefinitionsMappedByAccrueAction,
          final ScheduledAction scheduledAction) {
    final List<ChargeDefinition> chargeMappingList = chargeDefinitionsMappedByChargeAction
        .get(scheduledAction.action.name());
    Stream<ChargeDefinition> chargeMapping = chargeMappingList == null ? Stream.empty() : chargeMappingList.stream();
    if (chargeMapping == null)
      chargeMapping = Stream.empty();

    final List<ChargeDefinition> accrueMappingList = chargeDefinitionsMappedByAccrueAction
        .get(scheduledAction.action.name());
    Stream<ChargeDefinition> accrueMapping = accrueMappingList == null ? Stream.empty() : accrueMappingList.stream();
    if (accrueMapping == null)
      accrueMapping = Stream.empty();

    return Stream.concat(
        accrueMapping.sorted(IndividualLoanService::proportionalityApplicationOrder),
        chargeMapping.sorted(IndividualLoanService::proportionalityApplicationOrder));
  }

  private static class ScheduledChargeComparator implements Comparator<ScheduledCharge>
  {
    @Override
    public int compare(ScheduledCharge o1, ScheduledCharge o2) {
      int ret = o1.getScheduledAction().when.compareTo(o2.getScheduledAction().when);
      if (ret == 0)
        ret = o1.getScheduledAction().action.compareTo(o2.getScheduledAction().action);
      if (ret == 0)
        ret = proportionalityApplicationOrder(o1.getChargeDefinition(), o2.getChargeDefinition());
      if (ret == 0)
        return o1.getChargeDefinition().getIdentifier().compareTo(o2.getChargeDefinition().getIdentifier());
      else
        return ret;
    }
  }

  private static int proportionalityApplicationOrder(final ChargeDefinition o1, final ChargeDefinition o2) {
    final Optional<ChargeProportionalDesignator> aProportionalToDesignator
        = ChargeProportionalDesignator.fromString(o1.getProportionalTo());
    final Optional<ChargeProportionalDesignator> bProportionalToDesignator
        = ChargeProportionalDesignator.fromString(o2.getProportionalTo());

    if (aProportionalToDesignator.isPresent() && bProportionalToDesignator.isPresent())
      return Integer.compare(
          aProportionalToDesignator.get().getOrderOfApplication(),
          bProportionalToDesignator.get().getOrderOfApplication());
    else if (aProportionalToDesignator.isPresent())
      return 1;
    else if (bProportionalToDesignator.isPresent())
      return -1;
    else
      return 0;
  }
}