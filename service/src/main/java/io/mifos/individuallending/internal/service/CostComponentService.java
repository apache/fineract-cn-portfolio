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
import io.mifos.individuallending.api.v1.domain.product.AccountDesignators;
import io.mifos.individuallending.api.v1.domain.product.ChargeProportionalDesignator;
import io.mifos.individuallending.api.v1.domain.workflow.Action;
import io.mifos.individuallending.internal.repository.CaseParametersEntity;
import io.mifos.portfolio.api.v1.domain.ChargeDefinition;
import io.mifos.portfolio.api.v1.domain.CostComponent;
import io.mifos.portfolio.service.internal.util.AccountingAdapter;
import org.javamoney.calc.common.Rate;
import org.javamoney.moneta.Money;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.money.MonetaryAmount;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * @author Myrle Krantz
 */
@Service
public class CostComponentService {
  private static final int EXTRA_PRECISION = 4;
  private static final int RUNNING_CALCULATION_PRECISION = 8;

  private final ScheduledChargesService scheduledChargesService;
  private final AccountingAdapter accountingAdapter;

  @Autowired
  public CostComponentService(
      final ScheduledChargesService scheduledChargesService,
      final AccountingAdapter accountingAdapter) {
    this.scheduledChargesService = scheduledChargesService;
    this.accountingAdapter = accountingAdapter;
  }

  public CostComponentsForRepaymentPeriod getCostComponentsForAction(
      final Action action,
      final DataContextOfAction dataContextOfAction,
      final BigDecimal forPaymentSize,
      final LocalDate forDate) {
    switch (action) {
      case OPEN:
        return getCostComponentsForOpen(dataContextOfAction);
      case APPROVE:
        return getCostComponentsForApprove(dataContextOfAction);
      case DENY:
        return getCostComponentsForDeny(dataContextOfAction);
      case DISBURSE:
        return getCostComponentsForDisburse(dataContextOfAction, forPaymentSize);
      case APPLY_INTEREST:
        return getCostComponentsForApplyInterest(dataContextOfAction);
      case ACCEPT_PAYMENT:
        return getCostComponentsForAcceptPayment(dataContextOfAction, forPaymentSize, forDate);
      case CLOSE:
        return getCostComponentsForClose(dataContextOfAction);
      case MARK_LATE:
        return getCostComponentsForMarkLate(dataContextOfAction, today().atStartOfDay());
      case WRITE_OFF:
        return getCostComponentsForWriteOff(dataContextOfAction);
      case RECOVER:
        return getCostComponentsForRecover(dataContextOfAction);
      default:
        throw ServiceException.internalError("Invalid action: ''{0}''.", action.name());
    }
  }

  public CostComponentsForRepaymentPeriod getCostComponentsForOpen(final DataContextOfAction dataContextOfAction) {
    final CaseParametersEntity caseParameters = dataContextOfAction.getCaseParametersEntity();
    final String productIdentifier = dataContextOfAction.getProductEntity().getIdentifier();
    final int minorCurrencyUnitDigits = dataContextOfAction.getProductEntity().getMinorCurrencyUnitDigits();
    final List<ScheduledAction> scheduledActions = Collections.singletonList(new ScheduledAction(Action.OPEN, today()));
    final List<ScheduledCharge> scheduledCharges = scheduledChargesService.getScheduledCharges(
        productIdentifier, scheduledActions);

    return getCostComponentsForScheduledCharges(
        Collections.emptyMap(),
        scheduledCharges,
        caseParameters.getBalanceRangeMaximum(),
        BigDecimal.ZERO,
        BigDecimal.ZERO,
        dataContextOfAction.getInterest(),
        minorCurrencyUnitDigits,
        true);
  }

  public CostComponentsForRepaymentPeriod getCostComponentsForDeny(final DataContextOfAction dataContextOfAction) {
    final CaseParametersEntity caseParameters = dataContextOfAction.getCaseParametersEntity();
    final String productIdentifier = dataContextOfAction.getProductEntity().getIdentifier();
    final int minorCurrencyUnitDigits = dataContextOfAction.getProductEntity().getMinorCurrencyUnitDigits();
    final List<ScheduledAction> scheduledActions = Collections.singletonList(new ScheduledAction(Action.DENY, today()));
    final List<ScheduledCharge> scheduledCharges = scheduledChargesService.getScheduledCharges(
        productIdentifier, scheduledActions);

    return getCostComponentsForScheduledCharges(
        Collections.emptyMap(),
        scheduledCharges,
        caseParameters.getBalanceRangeMaximum(),
        BigDecimal.ZERO,
        BigDecimal.ZERO,
        dataContextOfAction.getInterest(),
        minorCurrencyUnitDigits,
        true);
  }

  public CostComponentsForRepaymentPeriod getCostComponentsForApprove(final DataContextOfAction dataContextOfAction) {
    //Charge the approval fee if applicable.
    final CaseParametersEntity caseParameters = dataContextOfAction.getCaseParametersEntity();
    final String productIdentifier = dataContextOfAction.getProductEntity().getIdentifier();
    final int minorCurrencyUnitDigits = dataContextOfAction.getProductEntity().getMinorCurrencyUnitDigits();
    final List<ScheduledAction> scheduledActions = Collections.singletonList(new ScheduledAction(Action.APPROVE, today()));
    final List<ScheduledCharge> scheduledCharges = scheduledChargesService.getScheduledCharges(
        productIdentifier, scheduledActions);

    return getCostComponentsForScheduledCharges(
        Collections.emptyMap(),
        scheduledCharges,
        caseParameters.getBalanceRangeMaximum(),
        BigDecimal.ZERO,
        BigDecimal.ZERO,
        dataContextOfAction.getInterest(),
        minorCurrencyUnitDigits,
        true);
  }

  public CostComponentsForRepaymentPeriod getCostComponentsForDisburse(
      final @Nonnull DataContextOfAction dataContextOfAction,
      final @Nullable BigDecimal requestedDisbursalSize) {
    final DesignatorToAccountIdentifierMapper designatorToAccountIdentifierMapper
        = new DesignatorToAccountIdentifierMapper(dataContextOfAction);
    final String customerLoanAccountIdentifier = designatorToAccountIdentifierMapper.mapOrThrow(AccountDesignators.CUSTOMER_LOAN);
    final BigDecimal currentBalance = accountingAdapter.getCurrentBalance(customerLoanAccountIdentifier).negate();

    if (requestedDisbursalSize != null &&
        dataContextOfAction.getCaseParametersEntity().getBalanceRangeMaximum().compareTo(
        currentBalance.add(requestedDisbursalSize)) < 0)
      throw ServiceException.conflict("Cannot disburse over the maximum balance.");

    final Optional<LocalDateTime> optionalStartOfTerm = accountingAdapter.getDateOfOldestEntryContainingMessage(
        customerLoanAccountIdentifier,
        dataContextOfAction.getMessageForCharge(Action.DISBURSE));
    final CaseParametersEntity caseParameters = dataContextOfAction.getCaseParametersEntity();
    final String productIdentifier = dataContextOfAction.getProductEntity().getIdentifier();
    final int minorCurrencyUnitDigits = dataContextOfAction.getProductEntity().getMinorCurrencyUnitDigits();
    final List<ScheduledAction> scheduledActions = Collections.singletonList(new ScheduledAction(Action.DISBURSE, today()));

    final BigDecimal disbursalSize;
    if (requestedDisbursalSize == null)
      disbursalSize = dataContextOfAction.getCaseParametersEntity().getBalanceRangeMaximum().negate();
    else
      disbursalSize = requestedDisbursalSize.negate();

    final List<ScheduledCharge> scheduledCharges = scheduledChargesService.getScheduledCharges(
        productIdentifier, scheduledActions);


    final Map<Boolean, List<ScheduledCharge>> chargesSplitIntoScheduledAndAccrued = scheduledCharges.stream()
        .collect(Collectors.partitioningBy(x -> isAccruedChargeForAction(x.getChargeDefinition(), Action.DISBURSE)));

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
        caseParameters.getBalanceRangeMaximum(),
        currentBalance,
        disbursalSize,
        dataContextOfAction.getInterest(),
        minorCurrencyUnitDigits,
        true);
  }

  public CostComponentsForRepaymentPeriod getCostComponentsForApplyInterest(
      final DataContextOfAction dataContextOfAction)
  {
    final DesignatorToAccountIdentifierMapper designatorToAccountIdentifierMapper
        = new DesignatorToAccountIdentifierMapper(dataContextOfAction);
    final String customerLoanAccountIdentifier = designatorToAccountIdentifierMapper.mapOrThrow(AccountDesignators.CUSTOMER_LOAN);
    final BigDecimal currentBalance = accountingAdapter.getCurrentBalance(customerLoanAccountIdentifier).negate();

    final LocalDate startOfTerm = getStartOfTermOrThrow(dataContextOfAction, customerLoanAccountIdentifier);

    final CaseParametersEntity caseParameters = dataContextOfAction.getCaseParametersEntity();
    final String productIdentifier = dataContextOfAction.getProductEntity().getIdentifier();
    final int minorCurrencyUnitDigits = dataContextOfAction.getProductEntity().getMinorCurrencyUnitDigits();
    final LocalDate today = today();
    final ScheduledAction interestAction = new ScheduledAction(Action.APPLY_INTEREST, today, new Period(1, today));

    final List<ScheduledCharge> scheduledCharges = scheduledChargesService.getScheduledCharges(
        productIdentifier,
        Collections.singletonList(interestAction));

    final Map<Boolean, List<ScheduledCharge>> chargesSplitIntoScheduledAndAccrued = scheduledCharges.stream()
        .collect(Collectors.partitioningBy(x -> isAccruedChargeForAction(x.getChargeDefinition(), Action.APPLY_INTEREST)));

    final Map<ChargeDefinition, CostComponent> accruedCostComponents = chargesSplitIntoScheduledAndAccrued.get(true)
        .stream()
        .map(ScheduledCharge::getChargeDefinition)
        .collect(Collectors.toMap(chargeDefinition -> chargeDefinition,
            chargeDefinition -> getAccruedCostComponentToApply(dataContextOfAction, designatorToAccountIdentifierMapper, startOfTerm, chargeDefinition)));

    return getCostComponentsForScheduledCharges(
        accruedCostComponents,
        chargesSplitIntoScheduledAndAccrued.get(false),
        caseParameters.getBalanceRangeMaximum(),
        currentBalance,
        BigDecimal.ZERO,
        dataContextOfAction.getInterest(),
        minorCurrencyUnitDigits,
        true);
  }

  public CostComponentsForRepaymentPeriod getCostComponentsForAcceptPayment(
      final DataContextOfAction dataContextOfAction,
      final @Nullable BigDecimal requestedLoanPaymentSize,
      final LocalDate forDate)
  {
    final DesignatorToAccountIdentifierMapper designatorToAccountIdentifierMapper
        = new DesignatorToAccountIdentifierMapper(dataContextOfAction);
    final String customerLoanAccountIdentifier = designatorToAccountIdentifierMapper.mapOrThrow(AccountDesignators.CUSTOMER_LOAN);
    final BigDecimal currentBalance = accountingAdapter.getCurrentBalance(customerLoanAccountIdentifier).negate();

    final LocalDate startOfTerm = getStartOfTermOrThrow(dataContextOfAction, customerLoanAccountIdentifier);

    final CaseParametersEntity caseParameters = dataContextOfAction.getCaseParametersEntity();
    final String productIdentifier = dataContextOfAction.getProductEntity().getIdentifier();
    final int minorCurrencyUnitDigits = dataContextOfAction.getProductEntity().getMinorCurrencyUnitDigits();
    final ScheduledAction scheduledAction
        = ScheduledActionHelpers.getNextScheduledPayment(
        startOfTerm,
        forDate,
        dataContextOfAction.getCustomerCaseEntity().getEndOfTerm().toLocalDate(),
        dataContextOfAction.getCaseParameters()
    );

    final BigDecimal loanPaymentSize;

    if (requestedLoanPaymentSize != null) {
      loanPaymentSize = requestedLoanPaymentSize;
    }
    else {
      if (scheduledAction.actionPeriod != null && scheduledAction.actionPeriod.isLastPeriod()) {
        loanPaymentSize = currentBalance;
      }
      else {
        loanPaymentSize = currentBalance.min(dataContextOfAction.getCaseParametersEntity().getPaymentSize());
      }
    }

    final List<ScheduledCharge> scheduledChargesForThisAction = scheduledChargesService.getScheduledCharges(
        productIdentifier,
        Collections.singletonList(scheduledAction));

    final Map<Boolean, List<ScheduledCharge>> chargesSplitIntoScheduledAndAccrued = scheduledChargesForThisAction.stream()
        .collect(Collectors.partitioningBy(x -> isAccruedChargeForAction(x.getChargeDefinition(), Action.ACCEPT_PAYMENT)));

    final Map<ChargeDefinition, CostComponent> accruedCostComponents = chargesSplitIntoScheduledAndAccrued.get(true)
        .stream()
        .map(ScheduledCharge::getChargeDefinition)
        .collect(Collectors.toMap(chargeDefinition -> chargeDefinition,
            chargeDefinition -> getAccruedCostComponentToApply(dataContextOfAction, designatorToAccountIdentifierMapper, startOfTerm, chargeDefinition)));



    return getCostComponentsForScheduledCharges(
        accruedCostComponents,
        chargesSplitIntoScheduledAndAccrued.get(false),
        caseParameters.getBalanceRangeMaximum(),
        currentBalance,
        loanPaymentSize,
        dataContextOfAction.getInterest(),
        minorCurrencyUnitDigits,
        true);
  }

  public CostComponentsForRepaymentPeriod getCostComponentsForClose(final DataContextOfAction dataContextOfAction) {
    final DesignatorToAccountIdentifierMapper designatorToAccountIdentifierMapper
        = new DesignatorToAccountIdentifierMapper(dataContextOfAction);
    final String customerLoanAccountIdentifier = designatorToAccountIdentifierMapper.mapOrThrow(AccountDesignators.CUSTOMER_LOAN);
    final BigDecimal currentBalance = accountingAdapter.getCurrentBalance(customerLoanAccountIdentifier).negate();
    if (currentBalance.compareTo(BigDecimal.ZERO) != 0)
      throw ServiceException.conflict("Cannot close loan until the balance is zero.");

    final LocalDate startOfTerm = getStartOfTermOrThrow(dataContextOfAction, customerLoanAccountIdentifier);

    final CaseParametersEntity caseParameters = dataContextOfAction.getCaseParametersEntity();
    final String productIdentifier = dataContextOfAction.getProductEntity().getIdentifier();
    final int minorCurrencyUnitDigits = dataContextOfAction.getProductEntity().getMinorCurrencyUnitDigits();
    final LocalDate today = today();
    final ScheduledAction closeAction = new ScheduledAction(Action.CLOSE, today, new Period(1, today));

    final List<ScheduledCharge> scheduledCharges = scheduledChargesService.getScheduledCharges(
        productIdentifier,
        Collections.singletonList(closeAction));

    final Map<Boolean, List<ScheduledCharge>> chargesSplitIntoScheduledAndAccrued = scheduledCharges.stream()
        .collect(Collectors.partitioningBy(x -> isAccruedChargeForAction(x.getChargeDefinition(), Action.CLOSE)));

    final Map<ChargeDefinition, CostComponent> accruedCostComponents = chargesSplitIntoScheduledAndAccrued.get(true)
        .stream()
        .map(ScheduledCharge::getChargeDefinition)
        .collect(Collectors.toMap(chargeDefinition -> chargeDefinition,
            chargeDefinition -> getAccruedCostComponentToApply(dataContextOfAction, designatorToAccountIdentifierMapper, startOfTerm, chargeDefinition)));

    return getCostComponentsForScheduledCharges(
        accruedCostComponents,
        chargesSplitIntoScheduledAndAccrued.get(false),
        caseParameters.getBalanceRangeMaximum(),
        currentBalance,
        BigDecimal.ZERO,
        dataContextOfAction.getInterest(),
        minorCurrencyUnitDigits,
        true);
  }

  public CostComponentsForRepaymentPeriod getCostComponentsForMarkLate(final DataContextOfAction dataContextOfAction,
                                                                       final LocalDateTime forTime) {
    final DesignatorToAccountIdentifierMapper designatorToAccountIdentifierMapper
        = new DesignatorToAccountIdentifierMapper(dataContextOfAction);
    final String customerLoanAccountIdentifier = designatorToAccountIdentifierMapper.mapOrThrow(AccountDesignators.CUSTOMER_LOAN);
    final BigDecimal currentBalance = accountingAdapter.getCurrentBalance(customerLoanAccountIdentifier).negate();

    final LocalDate startOfTerm = getStartOfTermOrThrow(dataContextOfAction, customerLoanAccountIdentifier);

    final CaseParametersEntity caseParameters = dataContextOfAction.getCaseParametersEntity();
    final String productIdentifier = dataContextOfAction.getProductEntity().getIdentifier();
    final int minorCurrencyUnitDigits = dataContextOfAction.getProductEntity().getMinorCurrencyUnitDigits();
    final ScheduledAction scheduledAction = new ScheduledAction(Action.MARK_LATE, forTime.toLocalDate());

    final BigDecimal loanPaymentSize = dataContextOfAction.getCaseParametersEntity().getPaymentSize();

    final List<ScheduledCharge> scheduledChargesForThisAction = scheduledChargesService.getScheduledCharges(
        productIdentifier,
        Collections.singletonList(scheduledAction));

    final Map<Boolean, List<ScheduledCharge>> chargesSplitIntoScheduledAndAccrued = scheduledChargesForThisAction.stream()
        .collect(Collectors.partitioningBy(x -> isAccruedChargeForAction(x.getChargeDefinition(), Action.MARK_LATE)));

    final Map<ChargeDefinition, CostComponent> accruedCostComponents = chargesSplitIntoScheduledAndAccrued.get(true)
        .stream()
        .map(ScheduledCharge::getChargeDefinition)
        .collect(Collectors.toMap(chargeDefinition -> chargeDefinition,
            chargeDefinition -> getAccruedCostComponentToApply(dataContextOfAction, designatorToAccountIdentifierMapper, startOfTerm, chargeDefinition)));


    return getCostComponentsForScheduledCharges(
        accruedCostComponents,
        chargesSplitIntoScheduledAndAccrued.get(false),
        caseParameters.getBalanceRangeMaximum(),
        currentBalance,
        loanPaymentSize,
        dataContextOfAction.getInterest(),
        minorCurrencyUnitDigits,
        true);
  }

  private CostComponentsForRepaymentPeriod getCostComponentsForWriteOff(final DataContextOfAction dataContextOfAction) {
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
      final BigDecimal entryAccountAdjustment, //disbursement or payment size.
      final BigDecimal interest,
      final int minorCurrencyUnitDigits,
      final boolean accrualAccounting) {
    final Map<String, BigDecimal> balanceAdjustments = new HashMap<>();
    balanceAdjustments.put(AccountDesignators.CUSTOMER_LOAN, BigDecimal.ZERO);

    final Map<ChargeDefinition, CostComponent> costComponentMap = new HashMap<>();

    for (Map.Entry<ChargeDefinition, CostComponent> entry : accruedCostComponents.entrySet()) {
      final ChargeDefinition chargeDefinition = entry.getKey();
      final BigDecimal chargeAmount = entry.getValue().getAmount();
      costComponentMap.put(
          chargeDefinition,
          entry.getValue());

      //TODO: This should adjust differently depending on accrual accounting.
      // It can't be fixed until getAmountProportionalTo is fixed.
      adjustBalance(chargeDefinition.getFromAccountDesignator(), chargeAmount.negate(), balanceAdjustments);
      adjustBalance(chargeDefinition.getToAccountDesignator(), chargeAmount, balanceAdjustments);
    }


    for (final ScheduledCharge scheduledCharge : scheduledCharges) {
      if (accrualAccounting || !isAccrualChargeForAction(scheduledCharge.getChargeDefinition(), scheduledCharge.getScheduledAction().action)) {
        final BigDecimal amountProportionalTo = getAmountProportionalTo(
            scheduledCharge,
            maximumBalance,
            runningBalance,
            entryAccountAdjustment,
            balanceAdjustments);
        //TODO: getAmountProportionalTo is programmed under the assumption of non-accrual accounting.
        if (scheduledCharge.getChargeRange().map(x ->
            !x.amountIsWithinRange(amountProportionalTo)).orElse(false))
          continue;

        final CostComponent costComponent = costComponentMap
            .computeIfAbsent(scheduledCharge.getChargeDefinition(), CostComponentService::constructEmptyCostComponent);

        final BigDecimal chargeAmount = howToApplyScheduledChargeToAmount(scheduledCharge, interest)
            .apply(amountProportionalTo)
            .setScale(minorCurrencyUnitDigits, BigDecimal.ROUND_HALF_EVEN);
        adjustBalances(
            scheduledCharge.getScheduledAction().action,
            scheduledCharge.getChargeDefinition(),
            chargeAmount,
            balanceAdjustments,
            false); //TODO: once you've fixed getAmountProportionalTo, use the passed in variable.
        costComponent.setAmount(costComponent.getAmount().add(chargeAmount));
      }
    }

    return new CostComponentsForRepaymentPeriod(
        costComponentMap,
        balanceAdjustments.getOrDefault(AccountDesignators.LOANS_PAYABLE, BigDecimal.ZERO).negate());
  }

  private static BigDecimal getAmountProportionalTo(
      final ScheduledCharge scheduledCharge,
      final BigDecimal maximumBalance,
      final BigDecimal runningBalance,
      final BigDecimal loanPaymentSize,
      final Map<String, BigDecimal> balanceAdjustments) {
    final Optional<ChargeProportionalDesignator> optionalChargeProportionalTo
        = ChargeProportionalDesignator.fromString(scheduledCharge.getChargeDefinition().getProportionalTo());
    return optionalChargeProportionalTo.map(chargeProportionalTo ->
        getAmountProportionalTo(chargeProportionalTo, maximumBalance, runningBalance, loanPaymentSize, balanceAdjustments))
        .orElse(BigDecimal.ZERO);
  }

  static BigDecimal getAmountProportionalTo(
      final ChargeProportionalDesignator chargeProportionalTo,
      final BigDecimal maximumBalance,
      final BigDecimal runningBalance,
      final BigDecimal loanPaymentSize,
      final Map<String, BigDecimal> balanceAdjustments) {
    switch (chargeProportionalTo) {
      case NOT_PROPORTIONAL:
        return BigDecimal.ZERO;
      case MAXIMUM_BALANCE_DESIGNATOR:
        return maximumBalance;
      case RUNNING_BALANCE_DESIGNATOR:
        return runningBalance.subtract(balanceAdjustments.getOrDefault(AccountDesignators.CUSTOMER_LOAN, BigDecimal.ZERO));
      case REPAYMENT_DESIGNATOR:
        return loanPaymentSize;
      case PRINCIPAL_ADJUSTMENT_DESIGNATOR: {
        if (loanPaymentSize.compareTo(BigDecimal.ZERO) <= 0)
          return loanPaymentSize.abs();
        final BigDecimal newRunningBalance
            = runningBalance.subtract(balanceAdjustments.getOrDefault(AccountDesignators.CUSTOMER_LOAN, BigDecimal.ZERO));
        final BigDecimal newLoanPaymentSize = loanPaymentSize.min(newRunningBalance);
        return newLoanPaymentSize.add(balanceAdjustments.getOrDefault(AccountDesignators.CUSTOMER_LOAN, BigDecimal.ZERO)).abs();
      }
      default:
        return BigDecimal.ZERO;
    }
//TODO: correctly implement charges which are proportional to other charges.
  }

  private static CostComponent constructEmptyCostComponent(final ChargeDefinition chargeDefinition) {
    final CostComponent ret = new CostComponent();
    ret.setChargeIdentifier(chargeDefinition.getIdentifier());
    ret.setAmount(BigDecimal.ZERO);
    return ret;
  }

  private static Function<BigDecimal, BigDecimal> howToApplyScheduledChargeToAmount(
      final ScheduledCharge scheduledCharge, final BigDecimal interest)
  {
    switch (scheduledCharge.getChargeDefinition().getChargeMethod())
    {
      case FIXED: {
        return (amountProportionalTo) -> scheduledCharge.getChargeDefinition().getAmount();
      }
      case PROPORTIONAL: {
        final BigDecimal chargeAmountPerPeriod = PeriodChargeCalculator.chargeAmountPerPeriod(scheduledCharge, scheduledCharge.getChargeDefinition().getAmount(), RUNNING_CALCULATION_PRECISION);
        return chargeAmountPerPeriod::multiply;
      }
      case INTEREST: {
        final BigDecimal chargeAmountPerPeriod = PeriodChargeCalculator.chargeAmountPerPeriod(scheduledCharge, interest, RUNNING_CALCULATION_PRECISION);
        return chargeAmountPerPeriod::multiply;
      }
      default: {
        return (amountProportionalTo) -> BigDecimal.ZERO;
      }
    }
  }

  public BigDecimal getLoanPaymentSize(
      final BigDecimal assumedBalance,
      final DataContextOfAction dataContextOfAction) {
    final List<ScheduledAction> hypotheticalScheduledActions = ScheduledActionHelpers.getHypotheticalScheduledActions(
        today(),
        dataContextOfAction.getCaseParameters());
    final List<ScheduledCharge> hypotheticalScheduledCharges = scheduledChargesService.getScheduledCharges(
        dataContextOfAction.getProductEntity().getIdentifier(),
        hypotheticalScheduledActions);
    return getLoanPaymentSize(
        assumedBalance,
        dataContextOfAction.getInterest(),
        dataContextOfAction.getProductEntity().getMinorCurrencyUnitDigits(),
        hypotheticalScheduledCharges);
  }

  static BigDecimal getLoanPaymentSize(final BigDecimal startingBalance,
                                       final BigDecimal interest,
                                       final int minorCurrencyUnitDigits,
                                       final List<ScheduledCharge> scheduledCharges) {
    final int precision = startingBalance.precision() + minorCurrencyUnitDigits + EXTRA_PRECISION;
    final Map<Period, BigDecimal> accrualRatesByPeriod
        = PeriodChargeCalculator.getPeriodAccrualInterestRate(interest, scheduledCharges, precision);

    final int periodCount = accrualRatesByPeriod.size();
    if (periodCount == 0)
      return startingBalance;

    final BigDecimal geometricMeanAccrualRate = accrualRatesByPeriod.values().stream()
        .collect(RateCollectors.geometricMean(precision));

    final MonetaryAmount presentValue = AnnuityPayment.calculate(
        Money.of(startingBalance, "XXX"),
        Rate.of(geometricMeanAccrualRate),
        periodCount);
    return BigDecimal.valueOf(presentValue.getNumber().doubleValueExact()).setScale(minorCurrencyUnitDigits, BigDecimal.ROUND_HALF_EVEN);
  }

  private static void adjustBalances(
      final Action action,
      final ChargeDefinition chargeDefinition,
      final BigDecimal chargeAmount,
      final Map<String, BigDecimal> balanceAdjustments,
      boolean accrualAccounting) {
    if (accrualAccounting) {
      if (chargeIsAccrued(chargeDefinition)) {
        if (Action.valueOf(chargeDefinition.getAccrueAction()) == action) {
          adjustBalance(chargeDefinition.getFromAccountDesignator(), chargeAmount.negate(), balanceAdjustments);
          adjustBalance(chargeDefinition.getAccrualAccountDesignator(), chargeAmount, balanceAdjustments);
        } else if (Action.valueOf(chargeDefinition.getChargeAction()) == action) {
          adjustBalance(chargeDefinition.getAccrualAccountDesignator(), chargeAmount.negate(), balanceAdjustments);
          adjustBalance(chargeDefinition.getToAccountDesignator(), chargeAmount, balanceAdjustments);
        }
      } else if (Action.valueOf(chargeDefinition.getChargeAction()) == action) {
        adjustBalance(chargeDefinition.getFromAccountDesignator(), chargeAmount.negate(), balanceAdjustments);
        adjustBalance(chargeDefinition.getToAccountDesignator(), chargeAmount, balanceAdjustments);
      }
    }
    else if (Action.valueOf(chargeDefinition.getChargeAction()) == action) {
      adjustBalance(chargeDefinition.getFromAccountDesignator(), chargeAmount.negate(), balanceAdjustments);
      adjustBalance(chargeDefinition.getToAccountDesignator(), chargeAmount, balanceAdjustments);
    }
  }

  private static void adjustBalance(
      final String designator,
      final BigDecimal chargeAmount,
      final Map<String, BigDecimal> balanceAdjustments) {
    final BigDecimal balance = balanceAdjustments.computeIfAbsent(designator, (x) -> BigDecimal.ZERO);
    final BigDecimal newBalance = balance.add(chargeAmount);
    balanceAdjustments.put(designator, newBalance);
  }

  public static boolean chargeIsAccrued(final ChargeDefinition chargeDefinition) {
    return chargeDefinition.getAccrualAccountDesignator() != null;
  }

  private static boolean isAccruedChargeForAction(final ChargeDefinition chargeDefinition, final Action action) {
    return chargeDefinition.getAccrueAction() != null &&
        chargeDefinition.getChargeAction().equals(action.name());
  }

  private static boolean isAccrualChargeForAction(final ChargeDefinition chargeDefinition, final Action action) {
    return chargeDefinition.getAccrueAction() != null &&
        chargeDefinition.getAccrueAction().equals(action.name());
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

  private static LocalDate today() {
    return LocalDate.now(Clock.systemUTC());
  }

}
