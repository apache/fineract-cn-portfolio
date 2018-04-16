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
import org.apache.fineract.cn.portfolio.api.v1.domain.ChargeDefinition;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * @author Myrle Krantz
 */
public class SimulatedRunningBalances implements RunningBalances {
  private final static Map<String, BigDecimal> ACCOUNT_SIGNS = new HashMap<String, BigDecimal>() {{
    this.put(AccountDesignators.CUSTOMER_LOAN_PRINCIPAL, NEGATIVE);
    this.put(AccountDesignators.CUSTOMER_LOAN_FEES, NEGATIVE);
    this.put(AccountDesignators.CUSTOMER_LOAN_INTEREST, NEGATIVE);
    this.put(AccountDesignators.LOAN_FUNDS_SOURCE, NEGATIVE);
    this.put(AccountDesignators.PROCESSING_FEE_INCOME, POSITIVE);
    this.put(AccountDesignators.ORIGINATION_FEE_INCOME, POSITIVE);
    this.put(AccountDesignators.DISBURSEMENT_FEE_INCOME, POSITIVE);
    this.put(AccountDesignators.INTEREST_INCOME, POSITIVE);
    this.put(AccountDesignators.INTEREST_ACCRUAL, POSITIVE);
    this.put(AccountDesignators.LATE_FEE_INCOME, POSITIVE);
    this.put(AccountDesignators.LATE_FEE_ACCRUAL, POSITIVE);
    this.put(AccountDesignators.PRODUCT_LOSS_ALLOWANCE, NEGATIVE);
    this.put(AccountDesignators.GENERAL_LOSS_ALLOWANCE, NEGATIVE);
    this.put(AccountDesignators.EXPENSE, NEGATIVE);
    this.put(AccountDesignators.ENTRY, POSITIVE);
    //TODO: derive signs from IndividualLendingPatternFactory.individualLendingRequiredAccounts instead.
  }};
  final private Map<String, BigDecimal> balances = new HashMap<>();
  private final LocalDateTime startOfTerm;

  public SimulatedRunningBalances() {
    this.startOfTerm = LocalDateTime.now(Clock.systemUTC());
  }

  SimulatedRunningBalances(final LocalDateTime startOfTerm) {
    this.startOfTerm = startOfTerm;
  }

  @Override
  public BigDecimal getAccountSign(final String accountDesignator) {
    return ACCOUNT_SIGNS.get(accountDesignator);
  }

  @Override
  public Optional<BigDecimal> getAccountBalance(final String accountDesignator) {
    return Optional.ofNullable(balances.get(accountDesignator));
  }

  @Override
  public BigDecimal getAccruedBalanceForCharge(
      final ChargeDefinition chargeDefinition) {
    return balances.getOrDefault(chargeDefinition.getAccrualAccountDesignator(), BigDecimal.ZERO);
    //This is not accurate for all cases, but good enough for the cases it's used in.
  }

  @Override
  public Optional<LocalDateTime> getStartOfTerm() {
    return Optional.ofNullable(startOfTerm);
  }

  public void adjustBalance(final String accountDesignator, final BigDecimal amount) {
    final BigDecimal currentValue = balances.getOrDefault(accountDesignator, BigDecimal.ZERO);
    final BigDecimal newValue = isAccountNegative(accountDesignator) ? currentValue.add(amount.negate())
        : currentValue.add(amount);
    balances.put(accountDesignator, newValue);
  }

  Map<String, BigDecimal> snapshot() {
    return new HashMap<>(balances);
  }
}