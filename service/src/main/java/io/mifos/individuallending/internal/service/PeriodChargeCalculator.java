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

import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * @author Myrle Krantz
 */
@SuppressWarnings("WeakerAccess")
@Service
public class PeriodChargeCalculator {
  public PeriodChargeCalculator()
  {
  }

  Map<Period, BigDecimal> getPeriodAccrualRates(final List<ScheduledCharge> scheduledCharges, final int precision) {
    return scheduledCharges.stream()
            .filter(PeriodChargeCalculator::accruedCharge)
            .collect(Collectors.groupingBy(scheduledCharge -> scheduledCharge.getScheduledAction().repaymentPeriod,
                    Collectors.mapping(x -> chargeAmountPerPeriod(x, precision), RateCollectors.compound(precision))));
  }

  private static boolean accruedCharge(final ScheduledCharge scheduledCharge)
  {
    return scheduledCharge.getChargeDefinition().getAccrualAccountDesignator() != null &&
            scheduledCharge.getChargeDefinition().getAccrueAction() != null;
  }

  static BigDecimal chargeAmountPerPeriod(final ScheduledCharge scheduledCharge, final int precision)
  {
    if (scheduledCharge.getChargeDefinition().getForCycleSizeUnit() == null)
      return scheduledCharge.getChargeDefinition().getAmount();

    final BigDecimal actionPeriodDuration
            = BigDecimal.valueOf(
            scheduledCharge.getScheduledAction().actionPeriod
                    .getDuration()
                    .getSeconds());
    final BigDecimal chargeDefinitionCycleSizeUnitDuration
            = BigDecimal.valueOf(
            Optional.ofNullable(scheduledCharge.getChargeDefinition().getForCycleSizeUnit())
                    .orElse(ChronoUnit.YEARS)
                    .getDuration()
                    .getSeconds());

    final BigDecimal periodsInCycle = chargeDefinitionCycleSizeUnitDuration.divide(actionPeriodDuration, precision, BigDecimal.ROUND_HALF_EVEN);
    return scheduledCharge.getChargeDefinition().getAmount().divide(periodsInCycle, precision, BigDecimal.ROUND_HALF_EVEN);
  }
}
