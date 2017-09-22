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
package io.mifos.individuallending.internal.service.costcomponent;

import io.mifos.core.lang.ServiceException;
import io.mifos.individuallending.api.v1.domain.product.AccountDesignators;
import io.mifos.individuallending.api.v1.domain.product.ChargeProportionalDesignator;
import io.mifos.individuallending.api.v1.domain.workflow.Action;
import io.mifos.individuallending.internal.repository.CaseParametersEntity;
import io.mifos.individuallending.internal.service.*;
import io.mifos.individuallending.internal.service.schedule.*;
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

  public PaymentBuilder getCostComponentsForAction(
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

  public PaymentBuilder getCostComponentsForOpen(final DataContextOfAction dataContextOfAction) {
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
        new SimulatedRunningBalances(),
        BigDecimal.ZERO,
        BigDecimal.ZERO,
        BigDecimal.ZERO,
        dataContextOfAction.getInterest(),
        minorCurrencyUnitDigits,
        true);
  }

  public PaymentBuilder getCostComponentsForDeny(final DataContextOfAction dataContextOfAction) {
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
        new SimulatedRunningBalances(),
        BigDecimal.ZERO,
        BigDecimal.ZERO,
        BigDecimal.ZERO,
        dataContextOfAction.getInterest(),
        minorCurrencyUnitDigits,
        true);
  }

  public PaymentBuilder getCostComponentsForApprove(final DataContextOfAction dataContextOfAction) {
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
        new SimulatedRunningBalances(),
        BigDecimal.ZERO,
        BigDecimal.ZERO,
        BigDecimal.ZERO,
        dataContextOfAction.getInterest(),
        minorCurrencyUnitDigits,
        true);
  }

  public PaymentBuilder getCostComponentsForDisburse(
      final @Nonnull DataContextOfAction dataContextOfAction,
      final @Nullable BigDecimal requestedDisbursalSize) {
    final DesignatorToAccountIdentifierMapper designatorToAccountIdentifierMapper
        = new DesignatorToAccountIdentifierMapper(dataContextOfAction);
    final String customerLoanPrincipalAccountIdentifier = designatorToAccountIdentifierMapper.mapOrThrow(AccountDesignators.CUSTOMER_LOAN_PRINCIPAL);
    final RealRunningBalances runningBalances = new RealRunningBalances(accountingAdapter, designatorToAccountIdentifierMapper);
    final BigDecimal currentBalance = runningBalances.getBalance(AccountDesignators.CUSTOMER_LOAN_PRINCIPAL);

    if (requestedDisbursalSize != null &&
        dataContextOfAction.getCaseParametersEntity().getBalanceRangeMaximum().compareTo(
        currentBalance.add(requestedDisbursalSize)) < 0)
      throw ServiceException.conflict("Cannot disburse over the maximum balance.");

    final Optional<LocalDateTime> optionalStartOfTerm = accountingAdapter.getDateOfOldestEntryContainingMessage(
        customerLoanPrincipalAccountIdentifier,
        dataContextOfAction.getMessageForCharge(Action.DISBURSE));
    final CaseParametersEntity caseParameters = dataContextOfAction.getCaseParametersEntity();
    final String productIdentifier = dataContextOfAction.getProductEntity().getIdentifier();
    final int minorCurrencyUnitDigits = dataContextOfAction.getProductEntity().getMinorCurrencyUnitDigits();
    final List<ScheduledAction> scheduledActions = Collections.singletonList(new ScheduledAction(Action.DISBURSE, today()));

    final BigDecimal disbursalSize;
    if (requestedDisbursalSize == null)
      disbursalSize = dataContextOfAction.getCaseParametersEntity().getBalanceRangeMaximum();
    else
      disbursalSize = requestedDisbursalSize;

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
        runningBalances,
        dataContextOfAction.getCaseParametersEntity().getPaymentSize(),
        disbursalSize,
        BigDecimal.ZERO,
        dataContextOfAction.getInterest(),
        minorCurrencyUnitDigits,
        true);
  }

  public PaymentBuilder getCostComponentsForApplyInterest(
      final DataContextOfAction dataContextOfAction)
  {
    final DesignatorToAccountIdentifierMapper designatorToAccountIdentifierMapper
        = new DesignatorToAccountIdentifierMapper(dataContextOfAction);
    final RunningBalances runningBalances = new RealRunningBalances(accountingAdapter, designatorToAccountIdentifierMapper);

    final LocalDate startOfTerm = getStartOfTermOrThrow(dataContextOfAction, designatorToAccountIdentifierMapper);

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
        runningBalances,
        dataContextOfAction.getCaseParametersEntity().getPaymentSize(),
        BigDecimal.ZERO,
        BigDecimal.ZERO,
        dataContextOfAction.getInterest(),
        minorCurrencyUnitDigits,
        true);
  }

  public PaymentBuilder getCostComponentsForAcceptPayment(
      final DataContextOfAction dataContextOfAction,
      final @Nullable BigDecimal requestedLoanPaymentSize,
      final LocalDate forDate)
  {
    final DesignatorToAccountIdentifierMapper designatorToAccountIdentifierMapper
        = new DesignatorToAccountIdentifierMapper(dataContextOfAction);
    final RealRunningBalances runningBalances = new RealRunningBalances(accountingAdapter, designatorToAccountIdentifierMapper);

    final LocalDate startOfTerm = getStartOfTermOrThrow(dataContextOfAction, designatorToAccountIdentifierMapper);

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


    final BigDecimal loanPaymentSize;

    if (requestedLoanPaymentSize != null) {
      loanPaymentSize = requestedLoanPaymentSize;
    }
    else {
      if (scheduledAction.getActionPeriod() != null && scheduledAction.getActionPeriod().isLastPeriod()) {
        loanPaymentSize = runningBalances.getBalance(AccountDesignators.CUSTOMER_LOAN_GROUP);
      }
      else {
        final BigDecimal paymentSizeBeforeOnTopCharges = runningBalances.getBalance(AccountDesignators.CUSTOMER_LOAN_GROUP)
            .min(dataContextOfAction.getCaseParametersEntity().getPaymentSize());

        @SuppressWarnings("UnnecessaryLocalVariable")
        final BigDecimal paymentSizeIncludingOnTopCharges = accruedCostComponents.entrySet().stream()
            .filter(entry -> entry.getKey().getChargeOnTop() != null && entry.getKey().getChargeOnTop())
            .map(entry -> entry.getValue().getAmount())
            .reduce(paymentSizeBeforeOnTopCharges, BigDecimal::add);

        loanPaymentSize = paymentSizeIncludingOnTopCharges;
      }
    }


    return getCostComponentsForScheduledCharges(
        accruedCostComponents,
        chargesSplitIntoScheduledAndAccrued.get(false),
        caseParameters.getBalanceRangeMaximum(),
        runningBalances,
        dataContextOfAction.getCaseParametersEntity().getPaymentSize(),
        BigDecimal.ZERO,
        loanPaymentSize,
        dataContextOfAction.getInterest(),
        minorCurrencyUnitDigits,
        true);
  }

  public PaymentBuilder getCostComponentsForClose(final DataContextOfAction dataContextOfAction) {
    final DesignatorToAccountIdentifierMapper designatorToAccountIdentifierMapper
        = new DesignatorToAccountIdentifierMapper(dataContextOfAction);
    final RealRunningBalances runningBalances = new RealRunningBalances(accountingAdapter, designatorToAccountIdentifierMapper);

    if (runningBalances.getBalance(AccountDesignators.CUSTOMER_LOAN_GROUP).compareTo(BigDecimal.ZERO) != 0)
      throw ServiceException.conflict("Cannot close loan until the balance is zero.");

    final LocalDate startOfTerm = getStartOfTermOrThrow(dataContextOfAction, designatorToAccountIdentifierMapper);

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
        runningBalances,
        dataContextOfAction.getCaseParametersEntity().getPaymentSize(),
        BigDecimal.ZERO,
        BigDecimal.ZERO,
        dataContextOfAction.getInterest(),
        minorCurrencyUnitDigits,
        true);
  }

  public PaymentBuilder getCostComponentsForMarkLate(
      final DataContextOfAction dataContextOfAction,
      final LocalDateTime forTime)
  {
    final DesignatorToAccountIdentifierMapper designatorToAccountIdentifierMapper
        = new DesignatorToAccountIdentifierMapper(dataContextOfAction);
    final RunningBalances runningBalances = new RealRunningBalances(accountingAdapter, designatorToAccountIdentifierMapper);

    final LocalDate startOfTerm = getStartOfTermOrThrow(dataContextOfAction, designatorToAccountIdentifierMapper);

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
        runningBalances,
        loanPaymentSize,
        BigDecimal.ZERO,
        BigDecimal.ZERO,
        dataContextOfAction.getInterest(),
        minorCurrencyUnitDigits,
        true);
  }

  private PaymentBuilder getCostComponentsForWriteOff(final DataContextOfAction dataContextOfAction) {
    return null;
  }

  private PaymentBuilder getCostComponentsForRecover(final DataContextOfAction dataContextOfAction) {
    return null;
  }

  public static PaymentBuilder getCostComponentsForScheduledCharges(
      final Map<ChargeDefinition, CostComponent> accruedCostComponents,
      final Collection<ScheduledCharge> scheduledCharges,
      final BigDecimal maximumBalance,
      final RunningBalances preChargeBalances,
      final BigDecimal contractualRepayment,
      final BigDecimal requestedDisbursement,
      final BigDecimal requestedRepayment,
      final BigDecimal interest,
      final int minorCurrencyUnitDigits,
      final boolean accrualAccounting) {
    final PaymentBuilder paymentBuilder = new PaymentBuilder(preChargeBalances, accrualAccounting);

    for (Map.Entry<ChargeDefinition, CostComponent> entry : accruedCostComponents.entrySet()) {
      final ChargeDefinition chargeDefinition = entry.getKey();
      final BigDecimal chargeAmount = entry.getValue().getAmount();

      //TODO: This should adjust differently depending on accrual accounting.
      // It can't be fixed until getAmountProportionalTo is fixed.
      paymentBuilder.addToBalance(chargeDefinition.getFromAccountDesignator(), chargeAmount.negate());
      paymentBuilder.addToBalance(chargeDefinition.getToAccountDesignator(), chargeAmount);
      paymentBuilder.addToCostComponent(chargeDefinition, chargeAmount);
    }


    for (final ScheduledCharge scheduledCharge : scheduledCharges) {
      if (accrualAccounting || !isAccrualChargeForAction(scheduledCharge.getChargeDefinition(), scheduledCharge.getScheduledAction().getAction())) {
        final BigDecimal amountProportionalTo = getAmountProportionalTo(
            scheduledCharge,
            maximumBalance,
            preChargeBalances,
            contractualRepayment,
            requestedDisbursement,
            requestedRepayment,
            paymentBuilder);
        if (scheduledCharge.getChargeRange().map(x ->
            !x.amountIsWithinRange(amountProportionalTo)).orElse(false))
          continue;

        final BigDecimal chargeAmount = howToApplyScheduledChargeToAmount(scheduledCharge, interest)
            .apply(amountProportionalTo)
            .setScale(minorCurrencyUnitDigits, BigDecimal.ROUND_HALF_EVEN);
        paymentBuilder.adjustBalances(
            scheduledCharge.getScheduledAction().getAction(),
            scheduledCharge.getChargeDefinition(),
            chargeAmount);
      }
    }

    return paymentBuilder;
  }

  private static BigDecimal getAmountProportionalTo(
      final ScheduledCharge scheduledCharge,
      final BigDecimal maximumBalance,
      final RunningBalances runningBalances,
      final BigDecimal contractualRepayment,
      final BigDecimal requestedDisbursement,
      final BigDecimal requestedRepayment,
      final PaymentBuilder paymentBuilder) {
    final Optional<ChargeProportionalDesignator> optionalChargeProportionalTo
        = ChargeProportionalDesignator.fromString(scheduledCharge.getChargeDefinition().getProportionalTo());
    return optionalChargeProportionalTo.map(chargeProportionalTo ->
        getAmountProportionalTo(
            scheduledCharge,
            chargeProportionalTo,
            maximumBalance,
            runningBalances,
            contractualRepayment,
            requestedDisbursement,
            requestedRepayment,
            paymentBuilder))
        .orElse(BigDecimal.ZERO);
  }

  static BigDecimal getAmountProportionalTo(
      final ScheduledCharge scheduledCharge,
      final ChargeProportionalDesignator chargeProportionalTo,
      final BigDecimal maximumBalance,
      final RunningBalances runningBalances,
      final BigDecimal contractualRepayment,
      final BigDecimal requestedDisbursement,
      final BigDecimal requestedRepayment,
      final PaymentBuilder paymentBuilder) {
    switch (chargeProportionalTo) {
      case NOT_PROPORTIONAL:
        return BigDecimal.ONE;
      case MAXIMUM_BALANCE_DESIGNATOR:
        return maximumBalance;
      case RUNNING_BALANCE_DESIGNATOR: {
        final BigDecimal customerLoanRunningBalance = runningBalances.getBalance(AccountDesignators.CUSTOMER_LOAN_GROUP);
        return customerLoanRunningBalance.subtract(paymentBuilder.getBalanceAdjustment(AccountDesignators.CUSTOMER_LOAN_GROUP));
      }
      case CONTRACTUAL_REPAYMENT_DESIGNATOR:
        return contractualRepayment;
      case REQUESTED_DISBURSEMENT_DESIGNATOR:
        return requestedDisbursement;
      case REQUESTED_REPAYMENT_DESIGNATOR:
        return requestedRepayment.add(paymentBuilder.getBalanceAdjustment(AccountDesignators.ENTRY));
      case TO_ACCOUNT_DESIGNATOR:
        return runningBalances.getBalance(scheduledCharge.getChargeDefinition().getToAccountDesignator())
            .subtract(paymentBuilder.getBalanceAdjustment(scheduledCharge.getChargeDefinition().getToAccountDesignator()));
      case FROM_ACCOUNT_DESIGNATOR:
        return runningBalances.getBalance(scheduledCharge.getChargeDefinition().getFromAccountDesignator())
            .add(paymentBuilder.getBalanceAdjustment(scheduledCharge.getChargeDefinition().getFromAccountDesignator()));
      default:
        return BigDecimal.ZERO;
    }
//TODO: correctly implement charges which are proportional to other charges.
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

  public BigDecimal getLoanPaymentSizeForSingleDisbursement(
      final BigDecimal disbursementSize,
      final DataContextOfAction dataContextOfAction) {
    final List<ScheduledAction> hypotheticalScheduledActions = ScheduledActionHelpers.getHypotheticalScheduledActions(
        today(),
        dataContextOfAction.getCaseParameters());
    final List<ScheduledCharge> hypotheticalScheduledCharges = scheduledChargesService.getScheduledCharges(
        dataContextOfAction.getProductEntity().getIdentifier(),
        hypotheticalScheduledActions);
    return getLoanPaymentSize(
        disbursementSize,
        disbursementSize,
        dataContextOfAction.getInterest(),
        dataContextOfAction.getProductEntity().getMinorCurrencyUnitDigits(),
        hypotheticalScheduledCharges);
  }

  public static BigDecimal getLoanPaymentSize(
      final BigDecimal maximumBalanceSize,
      final BigDecimal disbursementSize,
      final BigDecimal interest,
      final int minorCurrencyUnitDigits,
      final List<ScheduledCharge> scheduledCharges) {
    final int precision = disbursementSize.precision() - 4 + minorCurrencyUnitDigits + EXTRA_PRECISION;
    final Map<Period, BigDecimal> accrualRatesByPeriod
        = PeriodChargeCalculator.getPeriodAccrualInterestRate(interest, scheduledCharges, disbursementSize.precision());

    final int periodCount = accrualRatesByPeriod.size();
    if (periodCount == 0)
      return disbursementSize;

    final BigDecimal geometricMeanAccrualRate = accrualRatesByPeriod.values().stream()
        .collect(RateCollectors.geometricMean(precision));

    final List<ScheduledCharge> disbursementFees = scheduledCharges.stream()
        .filter(x -> x.getScheduledAction().getAction().equals(Action.DISBURSE))
        .collect(Collectors.toList());
    final PaymentBuilder paymentBuilder = getCostComponentsForScheduledCharges(
        Collections.emptyMap(),
        disbursementFees,
        maximumBalanceSize,
        new SimulatedRunningBalances(),
        BigDecimal.ZERO, //Contractual repayment not determined yet here.
        disbursementSize,
        BigDecimal.ZERO,
        interest,
        minorCurrencyUnitDigits,
        false
        );
    final BigDecimal finalDisbursementSize = paymentBuilder.getBalanceAdjustment(
        AccountDesignators.CUSTOMER_LOAN_PRINCIPAL,
        AccountDesignators.CUSTOMER_LOAN_FEES).negate();

    final MonetaryAmount presentValue = AnnuityPayment.calculate(
        Money.of(finalDisbursementSize, "XXX"),
        Rate.of(geometricMeanAccrualRate),
        periodCount);
    return BigDecimal.valueOf(presentValue.getNumber().doubleValueExact()).setScale(minorCurrencyUnitDigits, BigDecimal.ROUND_HALF_EVEN);
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
                                          final DesignatorToAccountIdentifierMapper designatorToAccountIdentifierMapper) {

    final String customerLoanPrincipalAccountIdentifier = designatorToAccountIdentifierMapper.mapOrThrow(AccountDesignators.CUSTOMER_LOAN_PRINCIPAL);

    final Optional<LocalDateTime> firstDisbursalDateTime = accountingAdapter.getDateOfOldestEntryContainingMessage(
        customerLoanPrincipalAccountIdentifier,
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
