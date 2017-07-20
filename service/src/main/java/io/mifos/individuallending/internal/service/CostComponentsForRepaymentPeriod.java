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

import io.mifos.portfolio.api.v1.domain.ChargeDefinition;
import io.mifos.portfolio.api.v1.domain.CostComponent;

import java.math.BigDecimal;
import java.util.Map;
import java.util.stream.Stream;

/**
 * @author Myrle Krantz
 */
public class CostComponentsForRepaymentPeriod {
  final private BigDecimal runningBalance;
  final private Map<ChargeDefinition, CostComponent> costComponents;
  final private BigDecimal balanceAdjustment;

  CostComponentsForRepaymentPeriod(
      final BigDecimal runningBalance,
      final Map<ChargeDefinition, CostComponent> costComponents,
      final BigDecimal balanceAdjustment) {
    this.runningBalance = runningBalance;
    this.costComponents = costComponents;
    this.balanceAdjustment = balanceAdjustment;
  }

  public BigDecimal getRunningBalance() {
    return runningBalance;
  }

  Map<ChargeDefinition, CostComponent> getCostComponents() {
    return costComponents;
  }

  public Stream<Map.Entry<ChargeDefinition, CostComponent>> stream() {
    return costComponents.entrySet().stream();
  }

  BigDecimal getBalanceAdjustment() {
    return balanceAdjustment;
  }
}
