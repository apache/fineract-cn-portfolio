/*
 * Copyright 2017 Kuelap, Inc.
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

import io.mifos.individuallending.internal.service.DataContextOfAction;
import io.mifos.portfolio.api.v1.domain.ChargeDefinition;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * @author Myrle Krantz
 */
public class AvailableRunningBalancesWithLimits implements RunningBalances {
  private final RunningBalances decoratedRunningBalances;

  private final Map<String, BigDecimal> upperLimits = new HashMap<>();

  AvailableRunningBalancesWithLimits(final RunningBalances decoratedRunningBalances) {
    this.decoratedRunningBalances = decoratedRunningBalances;
  }

  void setUpperLimit(final String designator, final BigDecimal limit) {
    upperLimits.put(designator, limit);
  }

  @Override
  public BigDecimal getAvailableBalance(final String designator, final BigDecimal requestedAmount) {
    final BigDecimal balance = getBalance(designator).orElse(requestedAmount);
    final BigDecimal upperLimit = upperLimits.get(designator);
    if (upperLimit == null)
      return balance;
    else
      return upperLimit.min(balance);
  }

  @Override
  public Optional<BigDecimal> getAccountBalance(final String accountDesignator) {
    return decoratedRunningBalances.getAccountBalance(accountDesignator);
  }

  @Override
  public BigDecimal getAccruedBalanceForCharge(final ChargeDefinition chargeDefinition) {
    return decoratedRunningBalances.getAccruedBalanceForCharge(chargeDefinition);
  }

  @Override
  public Optional<LocalDateTime> getStartOfTerm(final DataContextOfAction dataContextOfAction) {
    return decoratedRunningBalances.getStartOfTerm(dataContextOfAction);
  }
}
