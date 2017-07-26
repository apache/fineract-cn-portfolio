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
import io.mifos.individuallending.api.v1.domain.caseinstance.CaseParameters;
import io.mifos.individuallending.api.v1.domain.product.AccountDesignators;
import io.mifos.individuallending.api.v1.domain.product.ChargeIdentifiers;
import io.mifos.individuallending.api.v1.domain.workflow.Action;
import io.mifos.individuallending.internal.mapper.CaseParametersMapper;
import io.mifos.individuallending.internal.repository.CaseParametersRepository;
import io.mifos.portfolio.api.v1.domain.AccountAssignment;
import io.mifos.portfolio.api.v1.domain.ChargeDefinition;
import io.mifos.portfolio.api.v1.domain.CostComponent;
import io.mifos.portfolio.service.internal.repository.CaseEntity;
import io.mifos.portfolio.service.internal.repository.CaseRepository;
import io.mifos.portfolio.service.internal.repository.ProductEntity;
import io.mifos.portfolio.service.internal.repository.ProductRepository;
import io.mifos.portfolio.service.internal.util.AccountingAdapter;
import org.javamoney.calc.common.Rate;
import org.javamoney.moneta.Money;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.Nullable;
import javax.money.MonetaryAmount;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.function.BiFunction;
import java.util.stream.Collectors;

/**
 * @author Myrle Krantz
 */
@Service
public class CostComponentService {
  private static final int EXTRA_PRECISION = 4;
  private static final int RUNNING_CALCULATION_PRECISION = 8;

  private final ProductRepository productRepository;
  private final CaseRepository caseRepository;
  private final CaseParametersRepository caseParametersRepository;
  private final IndividualLoanService individualLoanService;
  private final AccountingAdapter accountingAdapter;

  @Autowired
  public CostComponentService(
          final ProductRepository productRepository,
          final CaseRepository caseRepository,
          final CaseParametersRepository caseParametersRepository,
          final IndividualLoanService individualLoanService,
          final AccountingAdapter accountingAdapter) {
    this.productRepository = productRepository;
    this.caseRepository = caseRepository;
    this.caseParametersRepository = caseParametersRepository;
    this.individualLoanService = individualLoanService;
    this.accountingAdapter = accountingAdapter;
  }

  public DataContextOfAction checkedGetDataContext(
          final String productIdentifier,
          final String caseIdentifier,
          final @Nullable List<AccountAssignment> oneTimeAccountAssignments) {

    final ProductEntity product =
            productRepository.findByIdentifier(productIdentifier)
                    .orElseThrow(() -> ServiceException.notFound("Product not found ''{0}''.", productIdentifier));
    final CaseEntity customerCase =
            caseRepository.findByProductIdentifierAndIdentifier(productIdentifier, caseIdentifier)
                    .orElseThrow(() -> ServiceException.notFound("Case not found ''{0}.{1}''.", productIdentifier, caseIdentifier));

    final CaseParameters caseParameters =
            caseParametersRepository.findByCaseId(customerCase.getId())
                    .map(x -> CaseParametersMapper.mapEntity(x, product.getMinorCurrencyUnitDigits()))
                    .orElseThrow(() -> ServiceException.notFound(
                            "Individual loan not found ''{0}.{1}''.",
                            productIdentifier, caseIdentifier));

    return new DataContextOfAction(product, customerCase, caseParameters, oneTimeAccountAssignments);
  }

  public CostComponentsForRepaymentPeriod getCostComponentsForAction(
      final Action action,
      final DataContextOfAction dataContextOfAction) {
    switch (action) {
      case OPEN:
        return getCostComponentsForOpen(dataContextOfAction);
      case APPROVE:
        return getCostComponentsForApprove(dataContextOfAction);
      case DENY:
        return getCostComponentsForDeny(dataContextOfAction);
      case DISBURSE:
        return getCostComponentsForDisburse(dataContextOfAction);
      case APPLY_INTEREST:
        return getCostComponentsForApplyInterest(dataContextOfAction);
      case ACCEPT_PAYMENT:
        return getCostComponentsForAcceptPayment(dataContextOfAction, null);
      case CLOSE:
        return getCostComponentsForClose(dataContextOfAction);
      case MARK_LATE:
        return getCostComponentsForMarkLate(dataContextOfAction);
      case WRITE_OFF:
        return getCostComponentsForWriteOff(dataContextOfAction);
      case RECOVER:
        return getCostComponentsForRecover(dataContextOfAction);
      default:
        throw ServiceException.internalError("Invalid action: ''{0}''.", action.name());
    }
  }

  public CostComponentsForRepaymentPeriod getCostComponentsForOpen(final DataContextOfAction dataContextOfAction) {
    final CaseParameters caseParameters = dataContextOfAction.getCaseParameters();
    final String productIdentifier = dataContextOfAction.getProduct().getIdentifier();
    final int minorCurrencyUnitDigits = dataContextOfAction.getProduct().getMinorCurrencyUnitDigits();
    final List<ScheduledAction> scheduledActions = Collections.singletonList(new ScheduledAction(Action.OPEN, today()));
    final List<ScheduledCharge> scheduledCharges = individualLoanService.getScheduledCharges(
        productIdentifier, scheduledActions);

    return getCostComponentsForScheduledCharges(
        Collections.emptyMap(),
        scheduledCharges,
        caseParameters.getMaximumBalance(),
        BigDecimal.ZERO,
        BigDecimal.ZERO,
        minorCurrencyUnitDigits);
  }

  public CostComponentsForRepaymentPeriod getCostComponentsForDeny(final DataContextOfAction dataContextOfAction) {
    final CaseParameters caseParameters = dataContextOfAction.getCaseParameters();
    final String productIdentifier = dataContextOfAction.getProduct().getIdentifier();
    final int minorCurrencyUnitDigits = dataContextOfAction.getProduct().getMinorCurrencyUnitDigits();
    final List<ScheduledAction> scheduledActions = Collections.singletonList(new ScheduledAction(Action.DENY, today()));
    final List<ScheduledCharge> scheduledCharges = individualLoanService.getScheduledCharges(
        productIdentifier, scheduledActions);

    return getCostComponentsForScheduledCharges(
        Collections.emptyMap(),
        scheduledCharges,
        caseParameters.getMaximumBalance(),
        BigDecimal.ZERO,
        BigDecimal.ZERO,
        minorCurrencyUnitDigits);
  }

  public CostComponentsForRepaymentPeriod getCostComponentsForApprove(final DataContextOfAction dataContextOfAction) {
    //Charge the approval fee if applicable.
    final CaseParameters caseParameters = dataContextOfAction.getCaseParameters();
    final String productIdentifier = dataContextOfAction.getProduct().getIdentifier();
    final int minorCurrencyUnitDigits = dataContextOfAction.getProduct().getMinorCurrencyUnitDigits();
    final List<ScheduledAction> scheduledActions = Collections.singletonList(new ScheduledAction(Action.APPROVE, today()));
    final List<ScheduledCharge> scheduledCharges = individualLoanService.getScheduledCharges(
        productIdentifier, scheduledActions);

    return getCostComponentsForScheduledCharges(
        Collections.emptyMap(),
        scheduledCharges,
        caseParameters.getMaximumBalance(),
        BigDecimal.ZERO,
        BigDecimal.ZERO,
        minorCurrencyUnitDigits);
  }

  public CostComponentsForRepaymentPeriod getCostComponentsForDisburse(final DataContextOfAction dataContextOfAction) {
    final DesignatorToAccountIdentifierMapper designatorToAccountIdentifierMapper
        = new DesignatorToAccountIdentifierMapper(dataContextOfAction);
    final String customerLoanAccountIdentifier = designatorToAccountIdentifierMapper.mapOrThrow(AccountDesignators.CUSTOMER_LOAN);
    final BigDecimal currentBalance = accountingAdapter.getCurrentBalance(customerLoanAccountIdentifier);


    final Optional<LocalDateTime> optionalStartOfTerm = accountingAdapter.getDateOfOldestEntryContainingMessage(
        customerLoanAccountIdentifier,
        dataContextOfAction.getMessageForCharge(Action.DISBURSE));
    final CaseParameters caseParameters = dataContextOfAction.getCaseParameters();
    final String productIdentifier = dataContextOfAction.getProduct().getIdentifier();
    final int minorCurrencyUnitDigits = dataContextOfAction.getProduct().getMinorCurrencyUnitDigits();
    final List<ScheduledAction> scheduledActions = Collections.singletonList(new ScheduledAction(Action.DISBURSE, today()));
    final List<ScheduledCharge> scheduledCharges = individualLoanService.getScheduledCharges(
        productIdentifier, scheduledActions);


    final Map<Boolean, List<ScheduledCharge>> chargesSplitIntoScheduledAndAccrued = scheduledCharges.stream()
        .collect(Collectors.partitioningBy(x -> isAccruedChargeForAction(x, Action.DISBURSE)));

    final Map<ChargeDefinition, CostComponent> accruedCostComponents =
        optionalStartOfTerm.map(startOfTerm ->
        chargesSplitIntoScheduledAndAccrued.get(true)
        .stream()
        .map(ScheduledCharge::getChargeDefinition)
        .collect(Collectors.toMap(chargeDefinition -> chargeDefinition,
            chargeDefinition -> getAccruedCostComponentToApply(
                dataContextOfAction,
                designatorToAccountIdentifierMapper,
                startOfTerm.toLocalDate(),
                chargeDefinition)))).orElse(Collections.emptyMap());

    return getCostComponentsForScheduledCharges(
        accruedCostComponents,
        chargesSplitIntoScheduledAndAccrued.get(false),
        caseParameters.getMaximumBalance(),
        currentBalance,
        caseParameters.getMaximumBalance(),//TODO: This needs to be provided by the user.
        minorCurrencyUnitDigits);
  }

  public CostComponentsForRepaymentPeriod getCostComponentsForApplyInterest(
      final DataContextOfAction dataContextOfAction)
  {
    final DesignatorToAccountIdentifierMapper designatorToAccountIdentifierMapper
        = new DesignatorToAccountIdentifierMapper(dataContextOfAction);
    final String customerLoanAccountIdentifier = designatorToAccountIdentifierMapper.mapOrThrow(AccountDesignators.CUSTOMER_LOAN);
    final BigDecimal currentBalance = accountingAdapter.getCurrentBalance(customerLoanAccountIdentifier);

    final LocalDate startOfTerm = getStartOfTermOrThrow(dataContextOfAction, customerLoanAccountIdentifier);

    final CaseParameters caseParameters = dataContextOfAction.getCaseParameters();
    final String productIdentifier = dataContextOfAction.getProduct().getIdentifier();
    final int minorCurrencyUnitDigits = dataContextOfAction.getProduct().getMinorCurrencyUnitDigits();
    final LocalDate today = today();
    final ScheduledAction interestAction = new ScheduledAction(Action.APPLY_INTEREST, today, new Period(1, today));

    final List<ScheduledCharge> scheduledCharges = individualLoanService.getScheduledCharges(
        productIdentifier,
        Collections.singletonList(interestAction));

    final Map<Boolean, List<ScheduledCharge>> chargesSplitIntoScheduledAndAccrued = scheduledCharges.stream()
        .collect(Collectors.partitioningBy(x -> isAccruedChargeForAction(x, Action.APPLY_INTEREST)));

    final Map<ChargeDefinition, CostComponent> accruedCostComponents = chargesSplitIntoScheduledAndAccrued.get(true)
        .stream()
        .map(ScheduledCharge::getChargeDefinition)
        .collect(Collectors.toMap(chargeDefinition -> chargeDefinition,
            chargeDefinition -> getAccruedCostComponentToApply(dataContextOfAction, designatorToAccountIdentifierMapper, startOfTerm, chargeDefinition)));

    return getCostComponentsForScheduledCharges(
        accruedCostComponents,
        chargesSplitIntoScheduledAndAccrued.get(false),
        caseParameters.getMaximumBalance(),
        currentBalance,
        BigDecimal.ZERO,
        minorCurrencyUnitDigits);
  }

  public CostComponentsForRepaymentPeriod getCostComponentsForAcceptPayment(
      final DataContextOfAction dataContextOfAction,
      final @Nullable BigDecimal requestedLoanPaymentSize)
  {
    final DesignatorToAccountIdentifierMapper designatorToAccountIdentifierMapper
        = new DesignatorToAccountIdentifierMapper(dataContextOfAction);
    final String customerLoanAccountIdentifier = designatorToAccountIdentifierMapper.mapOrThrow(AccountDesignators.CUSTOMER_LOAN);
    final BigDecimal currentBalance = accountingAdapter.getCurrentBalance(customerLoanAccountIdentifier);

    final LocalDate startOfTerm = getStartOfTermOrThrow(dataContextOfAction, customerLoanAccountIdentifier);

    final CaseParameters caseParameters = dataContextOfAction.getCaseParameters();
    final String productIdentifier = dataContextOfAction.getProduct().getIdentifier();
    final int minorCurrencyUnitDigits = dataContextOfAction.getProduct().getMinorCurrencyUnitDigits();
    final ScheduledAction scheduledAction
        = ScheduledActionHelpers.getNextScheduledPayment(
        startOfTerm,
        dataContextOfAction.getCustomerCase().getEndOfTerm().toLocalDate(),
        caseParameters
    );

    final BigDecimal loanPaymentSize;
    if (requestedLoanPaymentSize != null)
      loanPaymentSize = requestedLoanPaymentSize;
    else {
      final List<ScheduledAction> hypotheticalScheduledActions = ScheduledActionHelpers.getHypotheticalScheduledActions(
          today(),
          caseParameters);
      final List<ScheduledCharge> hypotheticalScheduledCharges = individualLoanService.getScheduledCharges(
          productIdentifier,
          hypotheticalScheduledActions);
      loanPaymentSize = getLoanPaymentSize(currentBalance, minorCurrencyUnitDigits, hypotheticalScheduledCharges);
    }

    final List<ScheduledCharge> scheduledChargesForThisAction = individualLoanService.getScheduledCharges(
        productIdentifier,
        Collections.singletonList(scheduledAction));

    final Map<Boolean, List<ScheduledCharge>> chargesSplitIntoScheduledAndAccrued = scheduledChargesForThisAction.stream()
        .collect(Collectors.partitioningBy(x -> isAccruedChargeForAction(x, Action.ACCEPT_PAYMENT)));

    final Map<ChargeDefinition, CostComponent> accruedCostComponents = chargesSplitIntoScheduledAndAccrued.get(true)
        .stream()
        .map(ScheduledCharge::getChargeDefinition)
        .collect(Collectors.toMap(chargeDefinition -> chargeDefinition,
            chargeDefinition -> getAccruedCostComponentToApply(dataContextOfAction, designatorToAccountIdentifierMapper, startOfTerm, chargeDefinition)));



    return getCostComponentsForScheduledCharges(
        accruedCostComponents,
        chargesSplitIntoScheduledAndAccrued.get(false),
        caseParameters.getMaximumBalance(),
        currentBalance,
        loanPaymentSize,
        minorCurrencyUnitDigits);
  }

  private static boolean isAccruedChargeForAction(final ScheduledCharge scheduledCharge, final Action action) {
    return scheduledCharge.getChargeDefinition().getAccrueAction() != null &&
        scheduledCharge.getChargeDefinition().getChargeAction().equals(action.name());
  }

  private CostComponent getAccruedCostComponentToApply(final DataContextOfAction dataContextOfAction,
                                                       final DesignatorToAccountIdentifierMapper designatorToAccountIdentifierMapper,
                                                       final LocalDate startOfTerm,
                                                       final ChargeDefinition chargeDefinition) {
    final CostComponent ret = new CostComponent();

    final String accrualAccountIdentifier = designatorToAccountIdentifierMapper.mapOrThrow(chargeDefinition.getAccrualAccountDesignator());

    final BigDecimal amountAccrued = accountingAdapter.sumMatchingEntriesSinceDate(
        accrualAccountIdentifier,
        startOfTerm,
        dataContextOfAction.getMessageForCharge(Action.valueOf(chargeDefinition.getAccrueAction())));
    final BigDecimal amountApplied = accountingAdapter.sumMatchingEntriesSinceDate(
        accrualAccountIdentifier,
        startOfTerm,
        dataContextOfAction.getMessageForCharge(Action.valueOf(chargeDefinition.getChargeAction())));

    ret.setChargeIdentifier(chargeDefinition.getIdentifier());
    ret.setAmount(amountAccrued.subtract(amountApplied));
    return ret;
  }

  private LocalDate getStartOfTermOrThrow(final DataContextOfAction dataContextOfAction,
                                          final String customerLoanAccountIdentifier) {
    final Optional<LocalDateTime> firstDisbursalDateTime = accountingAdapter.getDateOfOldestEntryContainingMessage(
        customerLoanAccountIdentifier,
        dataContextOfAction.getMessageForCharge(Action.DISBURSE));

    return firstDisbursalDateTime.map(LocalDateTime::toLocalDate)
        .orElseThrow(() -> ServiceException.internalError(
            "Start of term for loan ''{0}'' could not be acquired from accounting.",
            dataContextOfAction.getCompoundIdentifer()));
  }

  private CostComponentsForRepaymentPeriod getCostComponentsForMarkLate(final DataContextOfAction dataContextOfAction) {
    return null;
  }
  private CostComponentsForRepaymentPeriod getCostComponentsForWriteOff(final DataContextOfAction dataContextOfAction) {
    return null;
  }
  private CostComponentsForRepaymentPeriod getCostComponentsForClose(final DataContextOfAction dataContextOfAction) {
    return null;
  }
  private CostComponentsForRepaymentPeriod getCostComponentsForRecover(final DataContextOfAction dataContextOfAction) {
    return null;
  }

  static CostComponentsForRepaymentPeriod getCostComponentsForScheduledCharges(
      final Map<ChargeDefinition, CostComponent> accruedCostComponents,
      final Collection<ScheduledCharge> scheduledCharges,
      final BigDecimal maximumBalance,
      final BigDecimal runningBalance,
      final BigDecimal loanPaymentSize,
      final int minorCurrencyUnitDigits) {
    BigDecimal balanceAdjustment = BigDecimal.ZERO;
    BigDecimal currentRunningBalance = runningBalance;

    final Map<ChargeDefinition, CostComponent> costComponentMap = new HashMap<>();

    for (Map.Entry<ChargeDefinition, CostComponent> entry : accruedCostComponents.entrySet()) {
      costComponentMap.put(entry.getKey(), entry.getValue());

      if (chargeDefinitionTouchesAccount(entry.getKey(), AccountDesignators.CUSTOMER_LOAN))
        balanceAdjustment = balanceAdjustment.add(entry.getValue().getAmount());
    }

    final Map<Boolean, List<ScheduledCharge>> partitionedCharges = scheduledCharges.stream()
        .collect(Collectors.partitioningBy(CostComponentService::proportionalToPrincipalAdjustment));

    for (final ScheduledCharge scheduledCharge : partitionedCharges.get(false))
    {
      final CostComponent costComponent = costComponentMap
          .computeIfAbsent(scheduledCharge.getChargeDefinition(), CostComponentService::constructEmptyCostComponent);

      final BigDecimal chargeAmount = howToApplyScheduledChargeToBalance(scheduledCharge)
          .apply(maximumBalance, currentRunningBalance)
          .setScale(minorCurrencyUnitDigits, BigDecimal.ROUND_HALF_EVEN);
      if (chargeDefinitionTouchesAccount(scheduledCharge.getChargeDefinition(), AccountDesignators.CUSTOMER_LOAN))
        balanceAdjustment = balanceAdjustment.add(chargeAmount);
      costComponent.setAmount(costComponent.getAmount().add(chargeAmount));
      currentRunningBalance = currentRunningBalance.add(chargeAmount);
    }

    final BigDecimal principalAdjustment = loanPaymentSize.subtract(balanceAdjustment);
    for (final ScheduledCharge scheduledCharge : partitionedCharges.get(true))
    {
      final CostComponent costComponent = costComponentMap
          .computeIfAbsent(scheduledCharge.getChargeDefinition(), CostComponentService::constructEmptyCostComponent);

      final BigDecimal chargeAmount = applyPrincipalAdjustmentCharge(scheduledCharge, principalAdjustment)
          .setScale(minorCurrencyUnitDigits, BigDecimal.ROUND_HALF_EVEN);
      if (chargeDefinitionTouchesAccount(scheduledCharge.getChargeDefinition(), AccountDesignators.CUSTOMER_LOAN))
        balanceAdjustment = balanceAdjustment.add(chargeAmount);
      costComponent.setAmount(costComponent.getAmount().add(chargeAmount));
      currentRunningBalance = currentRunningBalance.add(chargeAmount);
    }

    return new CostComponentsForRepaymentPeriod(
        runningBalance,
        costComponentMap,
        balanceAdjustment.negate());
  }

  private static BigDecimal applyPrincipalAdjustmentCharge(
      final ScheduledCharge scheduledCharge,
      final BigDecimal principalAdjustment) {
    return scheduledCharge.getChargeDefinition().getAmount().multiply(principalAdjustment);
  }

  private static CostComponent constructEmptyCostComponent(final ChargeDefinition chargeDefinition) {
    final CostComponent ret = new CostComponent();
    ret.setChargeIdentifier(chargeDefinition.getIdentifier());
    ret.setAmount(BigDecimal.ZERO);
    return ret;
  }

  private static boolean proportionalToPrincipalAdjustment(final ScheduledCharge scheduledCharge) {
    if (!scheduledCharge.getChargeDefinition().getChargeMethod().equals(ChargeDefinition.ChargeMethod.PROPORTIONAL))
      return false;
    final String proportionalTo = scheduledCharge.getChargeDefinition().getProportionalTo();
    return proportionalTo != null && proportionalTo.equals(ChargeIdentifiers.PRINCIPAL_ADJUSTMENT_DESIGNATOR);
  }

  private static BiFunction<BigDecimal, BigDecimal, BigDecimal> howToApplyScheduledChargeToBalance(
      final ScheduledCharge scheduledCharge)
  {

    switch (scheduledCharge.getChargeDefinition().getChargeMethod())
    {
      case FIXED:
        return (maximumBalance, runningBalance) -> scheduledCharge.getChargeDefinition().getAmount();
      case PROPORTIONAL: {
        switch (scheduledCharge.getChargeDefinition().getProportionalTo()) {
          case ChargeIdentifiers.RUNNING_BALANCE_DESIGNATOR:
            return (maximumBalance, runningBalance) ->
                PeriodChargeCalculator.chargeAmountPerPeriod(scheduledCharge, RUNNING_CALCULATION_PRECISION)
                    .multiply(runningBalance);
          case ChargeIdentifiers.MAXIMUM_BALANCE_DESIGNATOR:
            return (maximumBalance, runningBalance) ->
                PeriodChargeCalculator.chargeAmountPerPeriod(scheduledCharge, RUNNING_CALCULATION_PRECISION)
                    .multiply(maximumBalance);
          case ChargeIdentifiers.PRINCIPAL_ADJUSTMENT_DESIGNATOR: //This is handled elsewhere.
            throw new IllegalStateException("A principal adjustment charge should not be passed to the same application function as the other charges.");
          default:
//TODO: correctly implement charges which are proportionate to other charges.
            return (maximumBalance, runningBalance) ->
                PeriodChargeCalculator.chargeAmountPerPeriod(scheduledCharge, RUNNING_CALCULATION_PRECISION)
                    .multiply(maximumBalance);
        }
      }
      default:
        return (maximumBalance, runningBalance) -> BigDecimal.ZERO;
    }
  }

  private static boolean chargeDefinitionTouchesCustomerVisibleAccount(final ChargeDefinition chargeDefinition)
  {
    return chargeDefinitionTouchesAccount(chargeDefinition, AccountDesignators.CUSTOMER_LOAN) ||
        chargeDefinitionTouchesAccount(chargeDefinition, AccountDesignators.ENTRY);
  }

  private static boolean chargeDefinitionTouchesAccount(final ChargeDefinition chargeDefinition, final String accountDesignator)
  {
    return chargeDefinition.getToAccountDesignator().equals(accountDesignator) ||
        chargeDefinition.getFromAccountDesignator().equals(accountDesignator) ||
        (chargeDefinition.getAccrualAccountDesignator() != null && chargeDefinition.getAccrualAccountDesignator().equals(accountDesignator));
  }

  static BigDecimal getLoanPaymentSize(final BigDecimal startingBalance,
                                       final int minorCurrencyUnitDigits,
                                       final List<ScheduledCharge> scheduledCharges) {
    final int precision = startingBalance.precision() + minorCurrencyUnitDigits + EXTRA_PRECISION;
    final Map<Period, BigDecimal> accrualRatesByPeriod
        = PeriodChargeCalculator.getPeriodAccrualRates(scheduledCharges, precision);

    final int periodCount = accrualRatesByPeriod.size();
    if (periodCount == 0)
      return startingBalance;

    final BigDecimal geometricMeanAccrualRate = accrualRatesByPeriod.values().stream()
        .collect(RateCollectors.geometricMean(precision));

    final MonetaryAmount presentValue = AnnuityPayment.calculate(
        Money.of(startingBalance, "XXX"),
        Rate.of(geometricMeanAccrualRate),
        periodCount);
    return BigDecimal.valueOf(presentValue.getNumber().doubleValueExact());
  }

  private static LocalDate today() {
    return LocalDate.now(Clock.systemUTC());
  }
}
