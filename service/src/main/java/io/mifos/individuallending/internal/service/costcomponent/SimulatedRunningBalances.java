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

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Myrle Krantz
 */
public class SimulatedRunningBalances implements RunningBalances {
  final private Map<String, BigDecimal> balances;

  public SimulatedRunningBalances() {
    this.balances = new HashMap<>();
  }

  @Override
  public BigDecimal getAccountBalance(final String accountDesignator) {
    return balances.getOrDefault(accountDesignator, BigDecimal.ZERO);
  }

  void adjustBalance(final String key, final BigDecimal amount) {
    final BigDecimal sign = ACCOUNT_SIGNS.get(key);
    final BigDecimal currentValue = balances.getOrDefault(key, BigDecimal.ZERO);
    final BigDecimal newValue = currentValue.add(amount.multiply(sign));
    balances.put(key, newValue);
  }

  Map<String, BigDecimal> snapshot() {
    return new HashMap<>(balances);
  }
}