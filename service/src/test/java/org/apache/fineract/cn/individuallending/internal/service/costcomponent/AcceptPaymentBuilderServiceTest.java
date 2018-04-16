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
import org.apache.fineract.cn.individuallending.api.v1.domain.product.ChargeIdentifiers;
import org.apache.fineract.cn.individuallending.api.v1.domain.workflow.Action;
import org.apache.fineract.cn.portfolio.api.v1.domain.CostComponent;
import org.apache.fineract.cn.portfolio.api.v1.domain.Payment;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author Myrle Krantz
 */
@RunWith(Parameterized.class)
public class AcceptPaymentBuilderServiceTest {

  @Parameterized.Parameters
  public static Collection testCases() {
    final Collection<PaymentBuilderServiceTestCase> ret = new ArrayList<>();
    ret.add(new PaymentBuilderServiceTestCase("simple case"));
    ret.add(disbursementFeesExceedFirstRepayment());
    ret.add(lastLittleRepaymentZerosPrincipal());
    ret.add(lastBigRepaymentZerosPrincipal());
    ret.add(explicitlySetRepaymentSizeIsSmallerThanFees());
    return ret;
  }

  private static PaymentBuilderServiceTestCase disbursementFeesExceedFirstRepayment() {
    return new PaymentBuilderServiceTestCase("disbursement fees exceed first repayment")
        .nonLateFees(BigDecimal.valueOf(200_00, 2))
        .expectedPrincipalRepayment(BigDecimal.ZERO)
        .expectedFeeRepayment(BigDecimal.valueOf(90_00, 2));
  }

  private static PaymentBuilderServiceTestCase lastLittleRepaymentZerosPrincipal() {
    return new PaymentBuilderServiceTestCase("last repayment should zero principal, although standard repayment larger than principal")
        .nonLateFees(BigDecimal.ZERO)
        .endOfTerm(LocalDateTime.now(Clock.systemUTC()))
        .forDate(LocalDateTime.now(Clock.systemUTC()))
        .requestedPaymentSize(null)
        .remainingPrincipal(BigDecimal.valueOf(42_00, 2))
        .expectedPrincipalRepayment(BigDecimal.valueOf(42_00, 2))
        .expectedInterestRepayment(BigDecimal.valueOf(10_00, 2))
        .expectedFeeRepayment(BigDecimal.ZERO);
  }

  private static PaymentBuilderServiceTestCase lastBigRepaymentZerosPrincipal() {
    return new PaymentBuilderServiceTestCase("last repayment should zero principal, although standard repayment smaller than principal")
        .nonLateFees(BigDecimal.ZERO)
        .endOfTerm(LocalDateTime.now(Clock.systemUTC()))
        .forDate(LocalDateTime.now(Clock.systemUTC()))
        .requestedPaymentSize(null)
        .remainingPrincipal(BigDecimal.valueOf(142_00, 2))
        .expectedPrincipalRepayment(BigDecimal.valueOf(142_00, 2))
        .expectedInterestRepayment(BigDecimal.valueOf(10_00, 2))
        .expectedFeeRepayment(BigDecimal.ZERO);
  }

  private static PaymentBuilderServiceTestCase explicitlySetRepaymentSizeIsSmallerThanFees() {
    return new PaymentBuilderServiceTestCase("a payment size was chosen which is smaller than the fees due.")
        .nonLateFees(BigDecimal.valueOf(50_00, 2))
        .accruedInterest(BigDecimal.ZERO)
        .requestedPaymentSize(BigDecimal.valueOf(49_00, 2))
        .expectedInterestRepayment(BigDecimal.ZERO)
        .expectedPrincipalRepayment(BigDecimal.ZERO)
        .expectedFeeRepayment(BigDecimal.valueOf(49_00, 2));
  }

  private final PaymentBuilderServiceTestCase testCase;

  public AcceptPaymentBuilderServiceTest(final PaymentBuilderServiceTestCase testCase) {
    this.testCase = testCase;
  }

  @Test
  public void getPaymentBuilder() throws Exception {
    final PaymentBuilder paymentBuilder = PaymentBuilderServiceTestHarness.constructCallToPaymentBuilder(
        AcceptPaymentBuilderService::new, testCase);

    final Payment payment = paymentBuilder.buildPayment(
        Action.ACCEPT_PAYMENT,
        Collections.emptySet(),
        testCase.forDate.toLocalDate());

    Assert.assertNotNull(payment);
    final Map<String, BigDecimal> mappedCostComponents = payment.getCostComponents().stream()
        .collect(Collectors.toMap(CostComponent::getChargeIdentifier, CostComponent::getAmount));

    Assert.assertEquals(testCase.toString(),
        testCase.expectedInterestRepayment, mappedCostComponents.getOrDefault(ChargeIdentifiers.INTEREST_ID, BigDecimal.ZERO));
    Assert.assertEquals(testCase.toString(),
        testCase.expectedInterestRepayment, mappedCostComponents.getOrDefault(ChargeIdentifiers.REPAY_INTEREST_ID, BigDecimal.ZERO));
    Assert.assertEquals(testCase.toString(),
        testCase.expectedPrincipalRepayment, mappedCostComponents.getOrDefault(ChargeIdentifiers.REPAY_PRINCIPAL_ID, BigDecimal.ZERO));
    Assert.assertEquals(testCase.toString(),
        testCase.expectedFeeRepayment, mappedCostComponents.getOrDefault(ChargeIdentifiers.REPAY_FEES_ID, BigDecimal.ZERO));

    final BigDecimal expectedTotalRepaymentSize = testCase.expectedFeeRepayment.add(testCase.expectedInterestRepayment).add(testCase.expectedPrincipalRepayment);
    Assert.assertEquals(expectedTotalRepaymentSize.negate(), payment.getBalanceAdjustments().getOrDefault(AccountDesignators.ENTRY, BigDecimal.ZERO));
    Assert.assertEquals(testCase.expectedFeeRepayment, payment.getBalanceAdjustments().getOrDefault(AccountDesignators.CUSTOMER_LOAN_FEES, BigDecimal.ZERO));
    Assert.assertEquals(testCase.expectedInterestRepayment, payment.getBalanceAdjustments().getOrDefault(AccountDesignators.CUSTOMER_LOAN_INTEREST, BigDecimal.ZERO));
    Assert.assertEquals(testCase.expectedPrincipalRepayment, payment.getBalanceAdjustments().getOrDefault(AccountDesignators.CUSTOMER_LOAN_PRINCIPAL, BigDecimal.ZERO));
  }
}