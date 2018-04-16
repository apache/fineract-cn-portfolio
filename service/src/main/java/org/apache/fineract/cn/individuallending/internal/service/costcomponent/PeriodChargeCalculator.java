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

import org.apache.fineract.cn.individuallending.api.v1.domain.workflow.Action;
import org.apache.fineract.cn.individuallending.internal.service.RateCollectors;
import org.apache.fineract.cn.individuallending.internal.service.schedule.Period;
import org.apache.fineract.cn.individuallending.internal.service.schedule.ScheduledAction;
import org.apache.fineract.cn.individuallending.internal.service.schedule.ScheduledActionHelpers;
import org.apache.fineract.cn.individuallending.internal.service.schedule.ScheduledCharge;
import org.apache.fineract.cn.portfolio.api.v1.domain.ChargeDefinition;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @author Myrle Krantz
 */
class PeriodChargeCalculator {
  static Map<Period, BigDecimal> getPeriodAccrualInterestRate(
      final BigDecimal interest,
      final List<ScheduledCharge> scheduledCharges,
      final int precision) {
    return scheduledCharges.stream()
        .filter(PeriodChargeCalculator::accruedInterestCharge)
        .collect(Collectors.groupingBy(scheduledCharge -> scheduledCharge.getScheduledAction().getRepaymentPeriod(),
            Collectors.mapping(x -> chargeAmountPerPeriod(x, interest, precision), Collectors.reducing(BigDecimal.ZERO, BigDecimal::add))));
  }

  static Map<Period, BigDecimal> getPeriodAccrualCompoundedInterestRate(
      final BigDecimal interest,
      final List<ScheduledCharge> scheduledCharges,
      final int precision) {
    return scheduledCharges.stream()
        .filter(PeriodChargeCalculator::accruedInterestCharge)
        .collect(Collectors.groupingBy(scheduledCharge -> scheduledCharge.getScheduledAction().getRepaymentPeriod(),
            Collectors.mapping(x -> chargeAmountPerPeriod(x, interest, precision), RateCollectors.compound(precision))));
  }

  private static boolean accruedInterestCharge(final ScheduledCharge scheduledCharge)
  {
    return scheduledCharge.getChargeDefinition().getAccrualAccountDesignator() != null &&
        scheduledCharge.getChargeDefinition().getAccrueAction() != null &&
        scheduledCharge.getChargeDefinition().getAccrueAction().equals(Action.APPLY_INTEREST.name()) &&
        scheduledCharge.getScheduledAction().getAction() == Action.ACCEPT_PAYMENT &&
        scheduledCharge.getScheduledAction().getActionPeriod() != null &&
        scheduledCharge.getChargeDefinition().getChargeMethod() == ChargeDefinition.ChargeMethod.INTEREST;
  }

  static BigDecimal chargeAmountPerPeriod(
      final ScheduledCharge scheduledCharge,
      final BigDecimal amountInPercentagePoints,
      final int precision)
  {
    final BigDecimal amountAsFraction = amountInPercentagePoints.divide(BigDecimal.valueOf(100), 4, BigDecimal.ROUND_HALF_EVEN);

    final ChargeDefinition chargeDefinition = scheduledCharge.getChargeDefinition();
    final ScheduledAction scheduledAction = scheduledCharge.getScheduledAction();
    if (chargeDefinition.getForCycleSizeUnit() == null)
      return amountAsFraction;

    final BigDecimal actionPeriodDuration
        = BigDecimal.valueOf(
        scheduledAction.getActionPeriod()
            .getDuration()
            .getSeconds());
    final Optional<BigDecimal> accrualPeriodDuration = Optional.ofNullable(chargeDefinition.getAccrueAction())
        .flatMap(action -> ScheduledActionHelpers.getAccrualPeriodDurationForAction(Action.valueOf(action)))
        .map(Duration::getSeconds)
        .map(BigDecimal::valueOf);

    final BigDecimal chargeDefinitionCycleSizeUnitDuration
            = BigDecimal.valueOf(
            Optional.ofNullable(chargeDefinition.getForCycleSizeUnit())
                    .orElse(ChronoUnit.YEARS)
                    .getDuration()
                    .getSeconds());

    final BigDecimal accrualPeriodsInCycle = chargeDefinitionCycleSizeUnitDuration.divide(
        accrualPeriodDuration.orElse(actionPeriodDuration), precision, BigDecimal.ROUND_HALF_EVEN);
    final int accrualPeriodsInActionPeriod = actionPeriodDuration.divide(
        accrualPeriodDuration.orElse(actionPeriodDuration), precision, BigDecimal.ROUND_HALF_EVEN)
        .intValueExact();
    final BigDecimal rateForAccrualPeriod = amountAsFraction.divide(
        accrualPeriodsInCycle, precision, BigDecimal.ROUND_HALF_EVEN);
    return createCompoundedRate(rateForAccrualPeriod, accrualPeriodsInActionPeriod, precision);
  }

  static BigDecimal createCompoundedRate(final BigDecimal interestRate, final int periodCount, final int precision)
  {
    return Stream.generate(() -> interestRate).limit(periodCount).collect(RateCollectors.compound(precision));
  }
}