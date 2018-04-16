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
import org.apache.fineract.cn.individuallending.api.v1.domain.product.ChargeProportionalDesignator;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.math.BigDecimal;
import java.util.*;

import static org.apache.fineract.cn.individuallending.api.v1.domain.product.ChargeProportionalDesignator.REQUESTED_DISBURSEMENT_DESIGNATOR;
import static org.apache.fineract.cn.individuallending.api.v1.domain.product.ChargeProportionalDesignator.RUNNING_BALANCE_DESIGNATOR;

/**
 * @author Myrle Krantz
 */
@RunWith(Parameterized.class)
public class CostComponentServiceTest {
  private static class TestCase {
    final String description;
    ChargeProportionalDesignator chargeProportionalDesignator = ChargeProportionalDesignator.NOT_PROPORTIONAL;
    BigDecimal maximumBalance = BigDecimal.ZERO;
    BigDecimal runningBalance = BigDecimal.ZERO;
    BigDecimal loanPaymentSize = BigDecimal.ZERO;
    BigDecimal expectedAmount = BigDecimal.ONE;

    private TestCase(String description) {
      this.description = description;
    }

    TestCase chargeProportionalDesignator(ChargeProportionalDesignator chargeProportionalDesignator) {
      this.chargeProportionalDesignator = chargeProportionalDesignator;
      return this;
    }

    TestCase maximumBalance(BigDecimal maximumBalance) {
      this.maximumBalance = maximumBalance;
      return this;
    }

    TestCase runningBalance(BigDecimal runningBalance) {
      this.runningBalance = runningBalance;
      return this;
    }

    TestCase loanPaymentSize(BigDecimal loanPaymentSize) {
      this.loanPaymentSize = loanPaymentSize;
      return this;
    }

    TestCase expectedAmount(BigDecimal expectedAmount) {
      this.expectedAmount = expectedAmount;
      return this;
    }

    @Override
    public String toString() {
      return "TestCase{" +
          "description='" + description + '\'' +
          ", chargeProportionalDesignator=" + chargeProportionalDesignator +
          ", maximumBalance=" + maximumBalance +
          ", runningBalance=" + runningBalance +
          ", loanPaymentSize=" + loanPaymentSize +
          ", expectedAmount=" + expectedAmount +
          '}';
    }
  }

  @Parameterized.Parameters
  public static Collection testCases() {
    final Collection<CostComponentServiceTest.TestCase> ret = new ArrayList<>();
    ret.add(new TestCase("simple"));
    ret.add(new TestCase("distribution fee")
        .chargeProportionalDesignator(REQUESTED_DISBURSEMENT_DESIGNATOR)
        .maximumBalance(BigDecimal.valueOf(2000))
        .loanPaymentSize(BigDecimal.valueOf(2000))
        .expectedAmount(BigDecimal.valueOf(2000)));
    ret.add(new TestCase("origination fee")
        .chargeProportionalDesignator(RUNNING_BALANCE_DESIGNATOR)
        .runningBalance(BigDecimal.valueOf(5000))
        .expectedAmount(BigDecimal.valueOf(5000)));
    return ret;
  }

  private final CostComponentServiceTest.TestCase testCase;

  public CostComponentServiceTest(final CostComponentServiceTest.TestCase testCase) {
    this.testCase = testCase;
  }

  @Test
  public void getAmountProportionalTo() {
    final SimulatedRunningBalances runningBalances = new SimulatedRunningBalances();
    runningBalances.adjustBalance(AccountDesignators.CUSTOMER_LOAN_PRINCIPAL, testCase.runningBalance.negate());
    final BigDecimal amount = CostComponentService.getAmountProportionalTo(
        null,
        testCase.chargeProportionalDesignator,
        testCase.maximumBalance,
        runningBalances,
        testCase.loanPaymentSize,
        testCase.loanPaymentSize,
        testCase.loanPaymentSize,
        new PaymentBuilder(runningBalances, false));

    Assert.assertEquals(testCase.toString(), testCase.expectedAmount, amount);
  }

}