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

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

class PaymentBuilderServiceTestCase {
  private final String description;
  LocalDateTime startOfTerm = LocalDateTime.of(2015, 1, 15, 0, 0);
  LocalDateTime endOfTerm = LocalDate.of(2015, 8, 15).atStartOfDay();
  LocalDateTime forDate = startOfTerm.plusMonths(1);
  BigDecimal configuredPaymentSize = BigDecimal.valueOf(100_00, 2);
  BigDecimal requestedPaymentSize = BigDecimal.valueOf(100_00, 2);
  BigDecimal entryAccountBalance = BigDecimal.valueOf(10_000_00, 2);
  BigDecimal remainingPrincipal = BigDecimal.valueOf(2000_00, 2);
  BigDecimal balanceRangeMaximum = BigDecimal.valueOf(4000_00, 2);
  BigDecimal interestRate = BigDecimal.valueOf(5_00, 2);
  BigDecimal accruedInterest = BigDecimal.valueOf(10_00, 2);
  BigDecimal nonLateFees = BigDecimal.valueOf(10_00, 2);
  BigDecimal expectedPrincipalRepayment = BigDecimal.valueOf(80_00, 2);
  BigDecimal expectedFeeRepayment = BigDecimal.valueOf(10_00, 2);
  BigDecimal expectedInterestRepayment = BigDecimal.valueOf(10_00, 2);
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

  PaymentBuilderServiceTestCase requestedPaymentSize(BigDecimal newVal) {
    this.requestedPaymentSize = newVal;
    return this;
  }

  PaymentBuilderServiceTestCase remainingPrincipal(BigDecimal newVal) {
    this.remainingPrincipal = newVal;
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

  PaymentBuilderServiceTestCase expectedPrincipalRepayment(BigDecimal newVal) {
    this.expectedPrincipalRepayment = newVal;
    return this;
  }

  PaymentBuilderServiceTestCase expectedFeeRepayment(BigDecimal newVal) {
    this.expectedFeeRepayment = newVal;
    return this;
  }

  PaymentBuilderServiceTestCase expectedInterestRepayment(BigDecimal newVal) {
    this.expectedInterestRepayment = newVal;
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