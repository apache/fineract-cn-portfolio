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

import io.mifos.portfolio.api.v1.domain.CostComponent;
import io.mifos.portfolio.service.internal.service.ChargeDefinitionService;
import io.mifos.portfolio.service.internal.service.ProductService;
import io.mifos.core.lang.DateConverter;
import io.mifos.individuallending.api.v1.domain.caseinstance.CaseParameters;
import io.mifos.individuallending.api.v1.domain.caseinstance.ChargeName;
import io.mifos.individuallending.api.v1.domain.caseinstance.PlannedPayment;
import io.mifos.individuallending.api.v1.domain.caseinstance.PlannedPaymentPage;
import io.mifos.individuallending.api.v1.domain.product.AccountDesignators;
import io.mifos.individuallending.api.v1.domain.workflow.Action;
import io.mifos.portfolio.api.v1.domain.ChargeDefinition;
import io.mifos.portfolio.api.v1.domain.Product;
import io.mifos.portfolio.service.internal.util.ChargeInstance;
import org.javamoney.calc.common.Rate;
import org.javamoney.moneta.Money;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.Nonnull;
import javax.money.MonetaryAmount;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static io.mifos.individuallending.api.v1.domain.product.AccountDesignators.PENDING_DISBURSAL;
import static io.mifos.individuallending.api.v1.domain.product.AccountDesignators.CUSTOMER_LOAN;
import static io.mifos.individuallending.api.v1.domain.product.ChargeIdentifiers.PAYMENT_ID;
import static io.mifos.individuallending.api.v1.domain.product.ChargeIdentifiers.PAYMENT_NAME;
import static io.mifos.portfolio.api.v1.domain.ChargeDefinition.ChargeMethod.FIXED;

/**
 * @author Myrle Krantz
 */
@Service
public class IndividualLoanService {
  private static final int EXTRA_PRECISION = 4;
  private final ProductService productService;
  private final ChargeDefinitionService chargeDefinitionService;
  private final ScheduledActionService scheduledActionService;
  private final PeriodChargeCalculator periodChargeCalculator;

  @Autowired
  public IndividualLoanService(final ProductService productService,
                               final ChargeDefinitionService chargeDefinitionService,
                               final ScheduledActionService scheduledActionService,
                               final PeriodChargeCalculator periodChargeCalculator) {
    this.productService = productService;
    this.chargeDefinitionService = chargeDefinitionService;
    this.scheduledActionService = scheduledActionService;
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

    final List<ScheduledAction> scheduledActions = scheduledActionService.getHypotheticalScheduledActions(initialDisbursalDate, caseParameters);

    final List<ScheduledCharge> scheduledCharges = getScheduledCharges(productIdentifier, minorCurrencyUnitDigits, caseParameters.getMaximumBalance(), scheduledActions);

    final List<PlannedPayment> plannedPaymentsElements = getPlannedPaymentsElements(caseParameters.getMaximumBalance(), minorCurrencyUnitDigits, scheduledCharges);

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

  public List<ChargeInstance> getChargeInstances(final String productIdentifier,
                                                 final CaseParameters caseParameters,
                                                 final BigDecimal currentBalance,
                                                 final Action action,
                                                 final LocalDate initialDisbursalDate,
                                                 final LocalDate forDate) {
    final Product product = productService.findByIdentifier(productIdentifier)
            .orElseThrow(() -> new IllegalArgumentException("Non-existent product identifier."));
    final int minorCurrencyUnitDigits = product.getMinorCurrencyUnitDigits();
    final List<ScheduledAction> scheduledActions = scheduledActionService.getScheduledActions(initialDisbursalDate, caseParameters, action, forDate);
    final List<ScheduledCharge> scheduledCharges = getScheduledCharges(productIdentifier, minorCurrencyUnitDigits, currentBalance, scheduledActions);

    final CostComponentsForRepaymentPeriod costComponentsForScheduledCharges = getCostComponentsForScheduledCharges(scheduledCharges, currentBalance, minorCurrencyUnitDigits);

    return costComponentsForScheduledCharges.costComponents.entrySet().stream()
            .map(IndividualLoanService::mapToChargeInstance)
            .collect(Collectors.toList());
  }

  private static ChargeInstance mapToChargeInstance(final Map.Entry<ChargeDefinition, CostComponent> x) {
    return new ChargeInstance(x.getKey().getFromAccountDesignator(), x.getKey().getToAccountDesignator(), x.getValue().getAmount());
  }

  private static ChargeName chargeNameFromChargeDefinition(final ScheduledCharge scheduledCharge) {
    return new ChargeName(scheduledCharge.getChargeDefinition().getIdentifier(), scheduledCharge.getChargeDefinition().getName());
  }

  private List<ScheduledCharge> getScheduledCharges(
          final String productIdentifier,
          final int minorCurrencyUnitDigits,
          final BigDecimal initialBalance,
          final @Nonnull List<ScheduledAction> scheduledActions) {
    final Map<String, List<ChargeDefinition>> chargeDefinitionsMappedByChargeAction
            = chargeDefinitionService.getChargeDefinitionsMappedByChargeAction(productIdentifier);

    final Map<String, List<ChargeDefinition>> chargeDefinitionsMappedByAccrueAction
            = chargeDefinitionService.getChargeDefinitionsMappedByAccrueAction(productIdentifier);

    final ChargeDefinition acceptPaymentDefinition = getPaymentChargeDefinition();

    final List<ScheduledCharge> scheduledCharges = getScheduledCharges(
            scheduledActions,
            chargeDefinitionsMappedByChargeAction,
            chargeDefinitionsMappedByAccrueAction,
            acceptPaymentDefinition);
    int digitsInInitialBalance = initialBalance.precision();
    final Map<Period, BigDecimal> accrualRatesByPeriod
            = periodChargeCalculator.getPeriodAccrualRates(scheduledCharges,
            digitsInInitialBalance + minorCurrencyUnitDigits + EXTRA_PRECISION);

    if (accrualRatesByPeriod.size() != 0) {
      final BigDecimal geometricMeanAccrualRate = accrualRatesByPeriod.values().stream().collect(RateCollectors.geometricMean(digitsInInitialBalance + minorCurrencyUnitDigits + EXTRA_PRECISION));
      acceptPaymentDefinition.setAmount(loanPaymentInContextOfAccruedInterest(initialBalance, accrualRatesByPeriod.size(), geometricMeanAccrualRate));
    }
    else
      acceptPaymentDefinition.setAmount(initialBalance);
    return scheduledCharges;
  }

  private ChargeDefinition getPaymentChargeDefinition() {
    final ChargeDefinition ret = new ChargeDefinition();
    ret.setChargeAction(Action.ACCEPT_PAYMENT.name());
    ret.setIdentifier(PAYMENT_ID);
    ret.setName(PAYMENT_NAME);
    ret.setFromAccountDesignator(CUSTOMER_LOAN);
    ret.setToAccountDesignator(PENDING_DISBURSAL);
    ret.setChargeMethod(FIXED);
    return ret;
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
          final List<ScheduledCharge> scheduledCharges) {
    final Map<Period, SortedSet<ScheduledCharge>> orderedScheduledChargesGroupedByPeriod
            = scheduledCharges.stream()
            .collect(Collectors.groupingBy(x -> x.getScheduledAction().repaymentPeriod,
                    Collectors.mapping(x -> x,
                            Collector.of(
                                    () -> new TreeSet<>(new ScheduledChargeComparator()),
                                    SortedSet::add,
                                    (left, right) -> { left.addAll(right); return left; }))));

    final SortedSet<Period> sortedRepaymentPeriods
            = scheduledCharges.stream()
            .map(x -> x.getScheduledAction().repaymentPeriod)
            .collect(Collector.of(TreeSet::new, TreeSet::add, (left, right) -> { left.addAll(right); return left; }));

    BigDecimal balance = initialBalance.setScale(minorCurrencyUnitDigits, BigDecimal.ROUND_HALF_EVEN);
    final List<PlannedPayment> plannedPayments = new ArrayList<>();
    for (final Period repaymentPeriod : sortedRepaymentPeriods)
    {
      final SortedSet<ScheduledCharge> scheduledChargesInPeriod = orderedScheduledChargesGroupedByPeriod.get(repaymentPeriod);
      final CostComponentsForRepaymentPeriod costComponentsForRepaymentPeriod =
              getCostComponentsForScheduledCharges(scheduledChargesInPeriod, balance, minorCurrencyUnitDigits);

      final PlannedPayment plannedPayment = new PlannedPayment();
      plannedPayment.setCostComponents(costComponentsForRepaymentPeriod.costComponents.values().stream().collect(Collectors.toList()));
      plannedPayment.setDate(DateConverter.toIsoString(repaymentPeriod.getEndDate()));
      balance = balance.add(costComponentsForRepaymentPeriod.balanceAdjustment);
      plannedPayment.setRemainingPrincipal(balance);
      plannedPayments.add(plannedPayment);
    }
    if (balance.compareTo(BigDecimal.ZERO) != 0)
    {
      final PlannedPayment lastPayment = plannedPayments.get(plannedPayments.size() - 1);
      final Optional<CostComponent> lastPaymentPayment = lastPayment.getCostComponents().stream()
              .filter(x -> x.getChargeIdentifier().equals(PAYMENT_ID)).findAny();
      lastPaymentPayment.ifPresent(x -> {
        x.setAmount(x.getAmount().subtract(lastPayment.getRemainingPrincipal()));
        lastPayment.setRemainingPrincipal(BigDecimal.ZERO.setScale(minorCurrencyUnitDigits, BigDecimal.ROUND_HALF_EVEN));
      });
    }
    return plannedPayments;
  }

  private static class CostComponentsForRepaymentPeriod {
    final Map<ChargeDefinition, CostComponent> costComponents;
    final BigDecimal balanceAdjustment;

    private CostComponentsForRepaymentPeriod(
            final Map<ChargeDefinition, CostComponent> costComponents,
            final BigDecimal balanceAdjustment) {
      this.costComponents = costComponents;
      this.balanceAdjustment = balanceAdjustment;
    }
  }

  static private CostComponentsForRepaymentPeriod getCostComponentsForScheduledCharges(
          final Collection<ScheduledCharge> scheduledCharges,
          final BigDecimal balance,
          final int minorCurrencyUnitDigits) {
    BigDecimal balanceAdjustment = BigDecimal.ZERO;

    final Map<ChargeDefinition, CostComponent> costComponentMap = new HashMap<>();
    for (final ScheduledCharge scheduledCharge : scheduledCharges)
    {
      final CostComponent costComponent = costComponentMap
              .computeIfAbsent(scheduledCharge.getChargeDefinition(),
                      chargeIdentifier -> {
                        final CostComponent ret = new CostComponent();
                        ret.setChargeIdentifier(scheduledCharge.getChargeDefinition().getIdentifier());
                        ret.setAmount(BigDecimal.ZERO);
                        return ret;
                      });

      final BigDecimal chargeAmount = howToApplyScheduledChargeToBalance(scheduledCharge, 8)
              .apply(balance)
              .setScale(minorCurrencyUnitDigits, BigDecimal.ROUND_HALF_EVEN);
      if (chargeDefinitionTouchesCustomerLoanAccount(scheduledCharge.getChargeDefinition()))
        balanceAdjustment = balanceAdjustment.add(chargeAmount);
      costComponent.setAmount(costComponent.getAmount().add(chargeAmount));
    }

    return new CostComponentsForRepaymentPeriod(
            costComponentMap,
            balanceAdjustment);
  }

  private static boolean chargeDefinitionTouchesCustomerLoanAccount(final ChargeDefinition chargeDefinition)
  {
    return chargeDefinition.getToAccountDesignator().equals(AccountDesignators.CUSTOMER_LOAN) ||
            chargeDefinition.getFromAccountDesignator().equals(AccountDesignators.CUSTOMER_LOAN) ||
            (chargeDefinition.getAccrualAccountDesignator() != null && chargeDefinition.getAccrualAccountDesignator().equals(AccountDesignators.CUSTOMER_LOAN));
  }

  private static Function<BigDecimal, BigDecimal> howToApplyScheduledChargeToBalance(
          final ScheduledCharge scheduledCharge,
          final int precision)
  {
    switch (scheduledCharge.getChargeDefinition().getChargeMethod())
    {
      case FIXED:
        return (x) -> scheduledCharge.getChargeDefinition().getAmount();
      case PROPORTIONAL:
        return (x) -> PeriodChargeCalculator.chargeAmountPerPeriod(scheduledCharge, precision).multiply(x);
      default:
        return (x) -> BigDecimal.ZERO;
    }
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
                                                    final Map<String, List<ChargeDefinition>> chargeDefinitionsMappedByAccrueAction,
                                                    final ChargeDefinition acceptPaymentDefinition) {
    return scheduledActions.stream()
            .flatMap(scheduledAction -> getChargeDefinitionStream(chargeDefinitionsMappedByChargeAction, chargeDefinitionsMappedByAccrueAction, acceptPaymentDefinition, scheduledAction)
                    .map(chargeDefinition -> new ScheduledCharge(scheduledAction, chargeDefinition)))
            .collect(Collectors.toList());
  }

  private Stream<ChargeDefinition> getChargeDefinitionStream(
          final Map<String, List<ChargeDefinition>> chargeDefinitionsMappedByChargeAction,
          final Map<String, List<ChargeDefinition>> chargeDefinitionsMappedByAccrueAction,
          final ChargeDefinition acceptPaymentDefinition,
          final ScheduledAction scheduledAction) {
    List<ChargeDefinition> chargeMapping = chargeDefinitionsMappedByChargeAction.get(scheduledAction.action.name());
    if ((chargeMapping == null) && (scheduledAction.action == Action.valueOf(acceptPaymentDefinition.getChargeAction())))
      chargeMapping = Collections.singletonList(acceptPaymentDefinition);

    if (chargeMapping == null)
      chargeMapping = Collections.emptyList();

    List<ChargeDefinition> accrueMapping = chargeDefinitionsMappedByAccrueAction.get(scheduledAction.action.name());
    if ((accrueMapping == null) && (scheduledAction.action == Action.valueOf(acceptPaymentDefinition.getChargeAction())))
      accrueMapping = Collections.singletonList(acceptPaymentDefinition);

    if (accrueMapping == null)
      accrueMapping = Collections.emptyList();

    return Stream.concat(accrueMapping.stream(), chargeMapping.stream());
  }
}