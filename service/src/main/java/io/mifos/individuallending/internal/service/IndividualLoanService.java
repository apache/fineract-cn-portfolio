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
import io.mifos.portfolio.api.v1.domain.ChargeDefinition;
import io.mifos.portfolio.api.v1.domain.CostComponent;
import io.mifos.portfolio.api.v1.domain.Product;
import io.mifos.portfolio.service.internal.service.ChargeDefinitionService;
import io.mifos.portfolio.service.internal.service.ProductService;
import org.javamoney.calc.common.Rate;
import org.javamoney.moneta.Money;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.Nonnull;
import javax.money.MonetaryAmount;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static io.mifos.individuallending.api.v1.domain.product.ChargeIdentifiers.REPAYMENT_ID;

/**
 * @author Myrle Krantz
 */
@Service
public class IndividualLoanService {
  private static final int EXTRA_PRECISION = 4;
  private final ProductService productService;
  private final ChargeDefinitionService chargeDefinitionService;
  private final PeriodChargeCalculator periodChargeCalculator;

  @Autowired
  public IndividualLoanService(final ProductService productService,
                               final ChargeDefinitionService chargeDefinitionService,
                               final PeriodChargeCalculator periodChargeCalculator) {
    this.productService = productService;
    this.chargeDefinitionService = chargeDefinitionService;
    this.periodChargeCalculator = periodChargeCalculator;
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

    final int precision = caseParameters.getMaximumBalance().precision() + minorCurrencyUnitDigits + EXTRA_PRECISION;
    final Map<Period, BigDecimal> accrualRatesByPeriod
        = periodChargeCalculator.getPeriodAccrualRates(scheduledCharges,
        precision);

    final BigDecimal geometricMeanAccrualRate = accrualRatesByPeriod.values().stream().collect(RateCollectors.geometricMean(precision));
    final BigDecimal loanPaymentSize = loanPaymentInContextOfAccruedInterest(caseParameters.getMaximumBalance(), accrualRatesByPeriod.size(), geometricMeanAccrualRate);

    final List<PlannedPayment> plannedPaymentsElements = getPlannedPaymentsElements(caseParameters.getMaximumBalance(), minorCurrencyUnitDigits, scheduledCharges, loanPaymentSize);

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

  private static class ScheduledChargeComparator implements Comparator<ScheduledCharge>
  {
    @Override
    public int compare(ScheduledCharge o1, ScheduledCharge o2) {
      int ret = o1.getScheduledAction().when.compareTo(o2.getScheduledAction().when);
      if (ret == 0)
        ret = o1.getScheduledAction().action.compareTo(o2.getScheduledAction().action);
      if (ret == 0)
        return o1.getChargeDefinition().getIdentifier().compareTo(o2.getChargeDefinition().getIdentifier());
      else
        return ret;
    }
  }

  static private List<PlannedPayment> getPlannedPaymentsElements(
      final BigDecimal initialBalance,
      final int minorCurrencyUnitDigits,
      final List<ScheduledCharge> scheduledCharges,
      final BigDecimal loanPaymentSize) {
    final Map<Period, SortedSet<ScheduledCharge>> orderedScheduledChargesGroupedByPeriod
            = scheduledCharges.stream()
            .collect(Collectors.groupingBy(scheduledCharge -> {
                  final ScheduledAction scheduledAction = scheduledCharge.getScheduledAction();
                  if (ScheduledActionHelpers.actionHasNoActionPeriod(scheduledAction.action))
                    return new Period(null, null);
                  else
                    return scheduledAction.repaymentPeriod;
                  },
                    Collectors.mapping(x -> x,
                            Collector.of(
                                    () -> new TreeSet<>(new ScheduledChargeComparator()),
                                    SortedSet::add,
                                    (left, right) -> { left.addAll(right); return left; }))));

    final SortedSet<Period> sortedRepaymentPeriods
            = orderedScheduledChargesGroupedByPeriod.keySet().stream()
            .collect(Collector.of(TreeSet::new, TreeSet::add, (left, right) -> { left.addAll(right); return left; }));

    BigDecimal balance = initialBalance.setScale(minorCurrencyUnitDigits, BigDecimal.ROUND_HALF_EVEN);
    final List<PlannedPayment> plannedPayments = new ArrayList<>();
    for (final Period repaymentPeriod : sortedRepaymentPeriods)
    {
      final SortedSet<ScheduledCharge> scheduledChargesInPeriod = orderedScheduledChargesGroupedByPeriod.get(repaymentPeriod);
      final CostComponentsForRepaymentPeriod costComponentsForRepaymentPeriod =
              CostComponentService.getCostComponentsForScheduledCharges(
                  Collections.emptyMap(),
                  scheduledChargesInPeriod,
                  balance,
                  balance,
                  loanPaymentSize,
                  minorCurrencyUnitDigits);

      final PlannedPayment plannedPayment = new PlannedPayment();
      plannedPayment.setCostComponents(new ArrayList<>(costComponentsForRepaymentPeriod.getCostComponents().values()));
      plannedPayment.setDate(repaymentPeriod.getEndDateAsString());
      balance = balance.add(costComponentsForRepaymentPeriod.getBalanceAdjustment());
      plannedPayment.setRemainingPrincipal(balance);
      plannedPayments.add(plannedPayment);
    }
    if (balance.compareTo(BigDecimal.ZERO) != 0)
    {
      final PlannedPayment lastPayment = plannedPayments.get(plannedPayments.size() - 1);
      final Optional<CostComponent> lastPaymentPayment = lastPayment.getCostComponents().stream()
              .filter(x -> x.getChargeIdentifier().equals(REPAYMENT_ID)).findAny();
      lastPaymentPayment.ifPresent(x -> {
        x.setAmount(x.getAmount().subtract(lastPayment.getRemainingPrincipal()));
        lastPayment.setRemainingPrincipal(BigDecimal.ZERO.setScale(minorCurrencyUnitDigits, BigDecimal.ROUND_HALF_EVEN));
      });
    }
    return plannedPayments;
  }

  private BigDecimal loanPaymentInContextOfAccruedInterest(
          final BigDecimal initialBalance,
          final int periodCount,
          final BigDecimal geometricMeanOfInterest) {
    if (periodCount == 0)
      throw new IllegalStateException("To calculate a loan payment there must be at least one payment period.");

    final MonetaryAmount presentValue = AnnuityPayment.calculate(Money.of(initialBalance, "XXX"), Rate.of(geometricMeanOfInterest), periodCount);
    return BigDecimal.valueOf(presentValue.getNumber().doubleValueExact()).negate();
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


    return Stream.concat(accrueMapping, chargeMapping);
  }
}