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

import org.apache.fineract.cn.individuallending.api.v1.domain.product.ChargeIdentifiers;
import org.apache.fineract.cn.individuallending.api.v1.domain.workflow.Action;
import org.apache.fineract.cn.individuallending.internal.service.DefaultChargeDefinitionsMocker;
import org.apache.fineract.cn.portfolio.api.v1.domain.CostComponent;
import org.apache.fineract.cn.portfolio.api.v1.domain.Payment;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author Myrle Krantz
 */
@RunWith(Parameterized.class)
public class WriteOffPaymentBuilderServiceTest {

  @Parameterized.Parameters
  public static Collection testCases() {
    final Collection<PaymentBuilderServiceTestCase> ret = new ArrayList<>();
    ret.add(new PaymentBuilderServiceTestCase("simple case"));
    ret.add(lossProvisioningInsufficient());
    return ret;
  }

  private static PaymentBuilderServiceTestCase lossProvisioningInsufficient() {
    return new PaymentBuilderServiceTestCase("loss provisioning insufficient")
        .generalLossAllowance(BigDecimal.ZERO);
  }

  private final PaymentBuilderServiceTestCase testCase;

  public WriteOffPaymentBuilderServiceTest(final PaymentBuilderServiceTestCase testCase) {
    this.testCase = testCase;
  }

  @Test
  public void getPaymentBuilder() throws Exception {
    final PaymentBuilder paymentBuilder = PaymentBuilderServiceTestHarness.constructCallToPaymentBuilder(
        (scheduledChargesService) -> new WriteOffPaymentBuilderService(DefaultChargeDefinitionsMocker.getChargeDefinitionService(Collections.emptyList())), testCase);

    final Payment payment = paymentBuilder.buildPayment(Action.WRITE_OFF, Collections.emptySet(), testCase.forDate.toLocalDate());
    Assert.assertNotNull(payment);
    final Map<String, CostComponent> mappedCostComponents = payment.getCostComponents().stream()
        .collect(Collectors.toMap(CostComponent::getChargeIdentifier, x -> x));

    Assert.assertEquals(
        testCase.remainingPrincipal,
        mappedCostComponents.get(ChargeIdentifiers.WRITE_OFF_ID).getAmount());
  }
}
