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
import org.apache.fineract.cn.individuallending.api.v1.domain.product.LossProvisionStep;
import org.apache.fineract.cn.individuallending.api.v1.domain.workflow.Action;
import org.apache.fineract.cn.individuallending.internal.service.LossProvisionStepService;
import org.apache.fineract.cn.individuallending.internal.service.schedule.LossProvisionChargesService;
import org.apache.fineract.cn.portfolio.api.v1.domain.CostComponent;
import org.apache.fineract.cn.portfolio.api.v1.domain.Payment;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.mockito.Matchers;
import org.mockito.Mockito;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @author Myrle Krantz
 */
@RunWith(Parameterized.class)
public class DisbursePaymentBuilderServiceTest {

  @Parameterized.Parameters
  public static Collection testCases() {
    final Collection<PaymentBuilderServiceTestCase> ret = new ArrayList<>();
    ret.add(simpleCase());
    return ret;
  }

  private static PaymentBuilderServiceTestCase simpleCase() {
    return new PaymentBuilderServiceTestCase("simple case");
  }

  private final PaymentBuilderServiceTestCase testCase;

  public DisbursePaymentBuilderServiceTest(final PaymentBuilderServiceTestCase testCase) {
    this.testCase = testCase;
  }

  @Test
  public void getPaymentBuilder() throws Exception {
    final LossProvisionStepService lossProvisionStepsService = Mockito.mock(LossProvisionStepService.class);
    Mockito.doReturn(Optional.of(new LossProvisionStep(0, BigDecimal.ONE))).when(lossProvisionStepsService).findByProductIdAndDaysLate(Matchers.any(), Matchers.eq(0));
    final LossProvisionChargesService lossProvisionChargesService = new LossProvisionChargesService(lossProvisionStepsService);
    final PaymentBuilder paymentBuilder = PaymentBuilderServiceTestHarness.constructCallToPaymentBuilder(
        (scheduledChargesService) -> new DisbursePaymentBuilderService(scheduledChargesService, lossProvisionChargesService), testCase);

    final Payment payment = paymentBuilder.buildPayment(Action.DISBURSE, Collections.emptySet(), testCase.forDate.toLocalDate());
    Assert.assertNotNull(payment);
    final Map<String, CostComponent> mappedCostComponents = payment.getCostComponents().stream()
        .collect(Collectors.toMap(CostComponent::getChargeIdentifier, x -> x));

    Assert.assertEquals(
        testCase.configuredPaymentSize,
        mappedCostComponents.get(ChargeIdentifiers.DISBURSE_PAYMENT_ID).getAmount());
    Assert.assertEquals(
        testCase.configuredPaymentSize.multiply(BigDecimal.valueOf(1, 2)).setScale(2, BigDecimal.ROUND_HALF_EVEN),
        paymentBuilder.getBalanceAdjustments().get(AccountDesignators.PRODUCT_LOSS_ALLOWANCE));
    Assert.assertEquals(
        testCase.configuredPaymentSize.multiply(BigDecimal.valueOf(1, 2)).negate().setScale(2, BigDecimal.ROUND_HALF_EVEN),
        paymentBuilder.getBalanceAdjustments().get(AccountDesignators.GENERAL_LOSS_ALLOWANCE));
  }

}