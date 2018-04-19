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
package org.apache.fineract.cn.individuallending.internal.service.costcomponent;

import org.apache.fineract.cn.individuallending.api.v1.domain.product.AccountDesignators;
import org.apache.fineract.cn.individuallending.api.v1.domain.product.ChargeProportionalDesignator;
import org.apache.fineract.cn.individuallending.api.v1.domain.workflow.Action;
import org.apache.fineract.cn.individuallending.internal.service.AnnuityPayment;
import org.apache.fineract.cn.individuallending.internal.service.RateCollectors;
import org.apache.fineract.cn.individuallending.internal.service.schedule.Period;
import org.apache.fineract.cn.individuallending.internal.service.schedule.ScheduledCharge;
import org.apache.fineract.cn.portfolio.api.v1.domain.ChargeDefinition;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * @author Myrle Krantz
 */
public class CostComponentService {
  private static final int EXTRA_PRECISION = 4;
  private static final int RUNNING_CALCULATION_PRECISION = 8;

  public static PaymentBuilder getCostComponentsForScheduledCharges(
      final Collection<ScheduledCharge> scheduledCharges,
      final BigDecimal maximumBalance,
      final RunningBalances preChargeBalances,
      final BigDecimal contractualRepayment,
      final BigDecimal requestedDisbursement,
      final BigDecimal requestedRepayment,
      final BigDecimal percentPoints,
      final int minorCurrencyUnitDigits,
      final boolean accrualAccounting) {
    final PaymentBuilder paymentBuilder = new PaymentBuilder(preChargeBalances, accrualAccounting);

    for (final ScheduledCharge scheduledCharge : scheduledCharges) {
      if (accrualAccounting || !isAccrualChargeForAction(scheduledCharge.getChargeDefinition(), scheduledCharge.getScheduledAction().getAction())) {
        final BigDecimal chargeAmount;
        if (!isIncurralActionForAccruedCharge(scheduledCharge.getChargeDefinition(), scheduledCharge.getScheduledAction().getAction()))
        {
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

          chargeAmount = howToApplyScheduledChargeToAmount(scheduledCharge, percentPoints)
              .apply(amountProportionalTo)
              .setScale(minorCurrencyUnitDigits, BigDecimal.ROUND_HALF_EVEN);
        }
        else
        {
          chargeAmount = preChargeBalances.getAccruedBalanceForCharge(scheduledCharge.getChargeDefinition())
              .add(paymentBuilder.getBalanceAdjustment(scheduledCharge.getChargeDefinition().getAccrualAccountDesignator()));
        }

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
        final BigDecimal customerLoanRunningBalance = runningBalances.getBalance(AccountDesignators.CUSTOMER_LOAN_GROUP).orElse(BigDecimal.ZERO);
        return customerLoanRunningBalance.subtract(paymentBuilder.getBalanceAdjustment(AccountDesignators.CUSTOMER_LOAN_GROUP));
      }
      case PRINCIPAL_DESIGNATOR: {
        return runningBalances.getBalance(AccountDesignators.CUSTOMER_LOAN_PRINCIPAL).orElse(BigDecimal.ZERO);
      }
      case CONTRACTUAL_REPAYMENT_DESIGNATOR:
        return contractualRepayment;
      case REQUESTED_DISBURSEMENT_DESIGNATOR:
        return requestedDisbursement;
      case REQUESTED_REPAYMENT_DESIGNATOR:
        return requestedRepayment.add(paymentBuilder.getBalanceAdjustment(AccountDesignators.ENTRY));
      case TO_ACCOUNT_DESIGNATOR:
        return runningBalances.getBalance(scheduledCharge.getChargeDefinition().getToAccountDesignator()).orElse(BigDecimal.ZERO)
            .subtract(paymentBuilder.getBalanceAdjustment(scheduledCharge.getChargeDefinition().getToAccountDesignator()));
      case FROM_ACCOUNT_DESIGNATOR:
        return runningBalances.getBalance(scheduledCharge.getChargeDefinition().getFromAccountDesignator()).orElse(BigDecimal.ZERO)
            .add(paymentBuilder.getBalanceAdjustment(scheduledCharge.getChargeDefinition().getFromAccountDesignator()));
      default:
        return BigDecimal.ZERO;
    }
  }

  private static Function<BigDecimal, BigDecimal> howToApplyScheduledChargeToAmount(
      final ScheduledCharge scheduledCharge, final BigDecimal percentPoints)
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
        final BigDecimal chargeAmountPerPeriod = PeriodChargeCalculator.chargeAmountPerPeriod(scheduledCharge, percentPoints, RUNNING_CALCULATION_PRECISION);
        return chargeAmountPerPeriod::multiply;
      }
      default: {
        return (amountProportionalTo) -> BigDecimal.ZERO;
      }
    }
  }

  public static BigDecimal getLoanPaymentSize(
      final BigDecimal maximumBalanceSize,
      final BigDecimal disbursementSize,
      final BigDecimal interest,
      final int minorCurrencyUnitDigits,
      final List<ScheduledCharge> scheduledCharges) {
    final int precision = disbursementSize.precision() - disbursementSize.scale() + minorCurrencyUnitDigits + EXTRA_PRECISION;
    final Map<Period, BigDecimal> accrualRatesByPeriod
        = PeriodChargeCalculator.getPeriodAccrualInterestRate(interest, scheduledCharges, precision);

    final int periodCount = accrualRatesByPeriod.size();
    if (periodCount == 0)
      return disbursementSize;

    final BigDecimal geometricMeanAccrualRate = accrualRatesByPeriod.values().stream()
        .collect(RateCollectors.geometricMean(precision));

    final List<ScheduledCharge> disbursementFees = scheduledCharges.stream()
        .filter(x -> x.getScheduledAction().getAction().equals(Action.DISBURSE))
        .collect(Collectors.toList());
    final PaymentBuilder paymentBuilder = getCostComponentsForScheduledCharges(
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

    final BigDecimal presentValue = AnnuityPayment.calculate(
        finalDisbursementSize,
        geometricMeanAccrualRate,
        periodCount,
        minorCurrencyUnitDigits);
    return presentValue.setScale(minorCurrencyUnitDigits, BigDecimal.ROUND_HALF_EVEN);
  }

  private static boolean isIncurralActionForAccruedCharge(final ChargeDefinition chargeDefinition, final Action action) {
    return chargeDefinition.getAccrueAction() != null &&
        chargeDefinition.getChargeAction().equals(action.name());
  }

  private static boolean isAccrualChargeForAction(final ChargeDefinition chargeDefinition, final Action action) {
    return chargeDefinition.getAccrueAction() != null &&
        chargeDefinition.getAccrueAction().equals(action.name());
  }

  public static LocalDate today() {
    return LocalDate.now(Clock.systemUTC());
  }

}
