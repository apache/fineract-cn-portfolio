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
  LocalDateTime startOfTerm = LocalDateTime.of(2015, 1, 15, 0, 0);
  LocalDateTime endOfTerm = LocalDate.of(2015, 8, 15).atStartOfDay();
  LocalDateTime forDate = startOfTerm.plusMonths(1);
  BigDecimal paymentSize = BigDecimal.valueOf(100_00, 2);
  BigDecimal balance = BigDecimal.valueOf(2000_00, 2);
  BigDecimal balanceRangeMaximum = BigDecimal.valueOf(4000_00, 2);
  BigDecimal interestRate = BigDecimal.valueOf(5_00, 2);
  BigDecimal accruedInterest = BigDecimal.valueOf(10_00, 2);
  BigDecimal nonLateFees = BigDecimal.valueOf(10_00, 2);
  BigDecimal expectedFeeRepayment = BigDecimal.valueOf(10_00, 2);
  BigDecimal expectedPrincipalRepayment = BigDecimal.valueOf(80_00, 2);
  BigDecimal generalLossAllowance = BigDecimal.valueOf(2000_00, 2);

  PaymentBuilderServiceTestCase(final String description) {
    this.description = description;
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

  PaymentBuilderServiceTestCase nonLateFees(BigDecimal newVal) {
    this.nonLateFees = newVal;
    return this;
  }

  PaymentBuilderServiceTestCase expectedFeeRepayment(BigDecimal newVal) {
    this.expectedFeeRepayment = newVal;
    return this;
  }

  PaymentBuilderServiceTestCase expectedPrincipalRepayment(BigDecimal newVal) {
    this.expectedPrincipalRepayment = newVal;
    return this;
  }

  PaymentBuilderServiceTestCase generalLossAllowance(BigDecimal newVal) {
    this.generalLossAllowance = newVal;
    return this;
  }

  @Override
  public String toString() {
    return "PaymentBuilderServiceTestCase{" +
        "description='" + description + '\'' +
        '}';
  }
}