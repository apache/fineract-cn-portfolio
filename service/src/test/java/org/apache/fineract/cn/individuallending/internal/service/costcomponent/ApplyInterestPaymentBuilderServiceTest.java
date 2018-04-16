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
import org.apache.fineract.cn.individuallending.api.v1.domain.workflow.Action;
import org.apache.fineract.cn.portfolio.api.v1.domain.Payment;
import org.junit.Assert;
import org.junit.Test;

import java.math.BigDecimal;
import java.util.Collections;

/**
 * @author Myrle Krantz
 */
public class ApplyInterestPaymentBuilderServiceTest {
  @Test
  public void getPaymentBuilder() throws Exception {
    final PaymentBuilderServiceTestCase testCase = new PaymentBuilderServiceTestCase("simple case");

    final PaymentBuilder paymentBuilder = PaymentBuilderServiceTestHarness.constructCallToPaymentBuilder(
        ApplyInterestPaymentBuilderService::new, testCase);

    final Payment payment = paymentBuilder.buildPayment(Action.APPLY_INTEREST, Collections.emptySet(), testCase.forDate.toLocalDate());
    Assert.assertNotNull(payment);

    Assert.assertEquals(BigDecimal.valueOf(27, 2), paymentBuilder.getBalanceAdjustments().get(AccountDesignators.INTEREST_ACCRUAL));
  }
}
