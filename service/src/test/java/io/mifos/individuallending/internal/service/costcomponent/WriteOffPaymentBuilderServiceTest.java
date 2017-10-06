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

import io.mifos.individuallending.api.v1.domain.product.AccountDesignators;
import io.mifos.individuallending.api.v1.domain.product.ChargeIdentifiers;
import io.mifos.individuallending.api.v1.domain.workflow.Action;
import io.mifos.portfolio.api.v1.domain.CostComponent;
import io.mifos.portfolio.api.v1.domain.Payment;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

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
    ret.add(simpleCase());
    //TODO: add use case for when the general loss allowance account doesn't have enough to cover the write off.
    return ret;
  }

  private static PaymentBuilderServiceTestCase simpleCase() {
    final PaymentBuilderServiceTestCase ret = new PaymentBuilderServiceTestCase("simple case");
    ret.runningBalances.adjustBalance(AccountDesignators.CUSTOMER_LOAN_PRINCIPAL, ret.balance.negate());
    ret.runningBalances.adjustBalance(AccountDesignators.GENERAL_LOSS_ALLOWANCE, ret.balance.negate());
    return ret;
  }

  private final PaymentBuilderServiceTestCase testCase;

  public WriteOffPaymentBuilderServiceTest(final PaymentBuilderServiceTestCase testCase) {
    this.testCase = testCase;
  }

  @Test
  public void getPaymentBuilder() throws Exception {
    final PaymentBuilder paymentBuilder = PaymentBuilderServiceTestHarness.constructCallToPaymentBuilder(
        (scheduledChargesService) -> new WriteOffPaymentBuilderService(), testCase);

    final Payment payment = paymentBuilder.buildPayment(Action.WRITE_OFF, Collections.emptySet(), testCase.forDate.toLocalDate());
    Assert.assertNotNull(payment);
    final Map<String, CostComponent> mappedCostComponents = payment.getCostComponents().stream()
        .collect(Collectors.toMap(CostComponent::getChargeIdentifier, x -> x));

    Assert.assertEquals(
        testCase.balance,
        mappedCostComponents.get(ChargeIdentifiers.WRITE_OFF_ID).getAmount());
  }

}
