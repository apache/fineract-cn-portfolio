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
import java.time.LocalDate;
import java.time.LocalDateTime;

class PaymentBuilderServiceTestCase {
  private final String description;
  private LocalDateTime startOfTerm = LocalDateTime.of(2015, 1, 15, 0, 0);
  LocalDateTime endOfTerm = LocalDate.of(2015, 8, 15).atStartOfDay();
  LocalDateTime forDate = startOfTerm.plusMonths(1);
  BigDecimal paymentSize = BigDecimal.valueOf(100_00, 2);
  BigDecimal balance = BigDecimal.valueOf(2000_00, 2);
  BigDecimal balanceRangeMaximum = BigDecimal.valueOf(1000_00, 2);
  BigDecimal interestRate = BigDecimal.valueOf(5_00, 2);
  BigDecimal accruedInterest = BigDecimal.valueOf(10_00, 2);
  SimulatedRunningBalances runningBalances;

  PaymentBuilderServiceTestCase(final String description) {
    this.description = description;
    runningBalances = new SimulatedRunningBalances(startOfTerm);
  }

  PaymentBuilderServiceTestCase endOfTerm(LocalDateTime endOfTerm) {
    this.endOfTerm = endOfTerm;
    return this;
  }

  PaymentBuilderServiceTestCase forDate(LocalDateTime forDate) {
    this.forDate = forDate;
    return this;
  }

  PaymentBuilderServiceTestCase paymentSize(BigDecimal paymentSize) {
    this.paymentSize = paymentSize;
    return this;
  }

  PaymentBuilderServiceTestCase balance(BigDecimal balance) {
    this.balance = balance;
    return this;
  }

  PaymentBuilderServiceTestCase balanceRangeMaximum(BigDecimal balanceRangeMaximum) {
    this.balanceRangeMaximum = balanceRangeMaximum;
    return this;
  }

  PaymentBuilderServiceTestCase interestRate(BigDecimal interestRate) {
    this.interestRate = interestRate;
    return this;
  }

  PaymentBuilderServiceTestCase accruedInterest(BigDecimal accruedInterest) {
    this.accruedInterest = accruedInterest;
    return this;
  }

  PaymentBuilderServiceTestCase runningBalances(SimulatedRunningBalances newVal) {
    this.runningBalances = newVal;
    return this;
  }

  @Override
  public String toString() {
    return "PaymentBuilderServiceTestCase{" +
        "description='" + description + '\'' +
        '}';
  }
}