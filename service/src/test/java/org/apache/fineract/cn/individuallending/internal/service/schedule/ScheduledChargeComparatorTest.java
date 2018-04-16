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
package org.apache.fineract.cn.individuallending.internal.service.schedule;

import org.apache.fineract.cn.individuallending.api.v1.domain.workflow.Action;
import org.apache.fineract.cn.portfolio.api.v1.domain.ChargeDefinition;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Optional;

/**
 * @author Myrle Krantz
 */
@RunWith(Parameterized.class)
public class ScheduledChargeComparatorTest {
  static class TestCase {
    private final String description;
    ScheduledCharge a;
    ScheduledCharge b;
    int expected;

    TestCase(String description) {
      this.description = description;
    }


    TestCase setA(ScheduledCharge a) {
      this.a = a;
      return this;
    }

    TestCase setB(ScheduledCharge b) {
      this.b = b;
      return this;
    }

    TestCase setExpected(int expected) {
      this.expected = expected;
      return this;
    }

    @Override
    public String toString() {
      return "TestCase{" +
          "description='" + description + '\'' +
          ", a=" + a +
          ", b=" + b +
          ", expected=" + expected +
          '}';
    }
  }

  @Parameterized.Parameters
  public static Collection testCases() {
    final Collection<ScheduledChargeComparatorTest.TestCase> ret = new ArrayList<>();

    final ScheduledCharge trackDisbursalScheduledCharge = new ScheduledCharge(
        SCHEDULED_DISBURSE_ACTION,
        TRACK_DISBURSE_CHARGE_DEFINITION,
        Optional.empty());

    final ScheduledCharge disburseFeeScheduledCharge = new ScheduledCharge(
        SCHEDULED_DISBURSE_ACTION,
        DISBURSE_FEE_CHARGE_DEFINITION,
        Optional.of(new ChargeRange(BigDecimal.valueOf(1000_0000, 4), Optional.empty())));

    ret.add(new TestCase("disbursementFeeVstrackDisbursement")
        .setA(trackDisbursalScheduledCharge)
        .setB(disburseFeeScheduledCharge)
        .setExpected(1));
    ret.add(new TestCase("disbursementFeeVstrackDisbursement")
        .setA(disburseFeeScheduledCharge)
        .setB(trackDisbursalScheduledCharge)
        .setExpected(-1));
    ret.add(new TestCase("disbursementFeeVstrackDisbursement")
        .setA(disburseFeeScheduledCharge)
        .setB(disburseFeeScheduledCharge)
        .setExpected(0));

    return ret;
  }

  private final static ScheduledAction SCHEDULED_DISBURSE_ACTION = new ScheduledAction(
      Action.DISBURSE,
      LocalDate.of(2017, 8, 25));

  private final static ChargeDefinition TRACK_DISBURSE_CHARGE_DEFINITION = new ChargeDefinition() {{
    this.setIdentifier("track-disburse-payment");
    this.setName("Track disburse payment");
    this.setDescription("Track disburse payment");
    this.setAccrueAction(null);
    this.setChargeAction(Action.DISBURSE.name());
    this.setAmount(BigDecimal.valueOf(100_0000, 4));
    this.setChargeMethod(ChargeDefinition.ChargeMethod.PROPORTIONAL);
    this.setProportionalTo("{principaladjustment}");
    this.setFromAccountDesignator("pending-disbursal");
    this.setAccrualAccountDesignator(null);
    this.setToAccountDesignator("customer-loan");
    this.setForCycleSizeUnit(null);
    this.setReadOnly(true);
    this.setForSegmentSet(null);
    this.setFromSegment(null);
    this.setToSegment(null);
  }};

  private final static ChargeDefinition DISBURSE_FEE_CHARGE_DEFINITION = new ChargeDefinition() {{
    this.setIdentifier("disbursement-fee2");
    this.setName("disbursement-fee2");
    this.setDescription("Disbursement fee");
    this.setAccrueAction(null);
    this.setChargeAction(Action.DISBURSE.name());
    this.setAmount(BigDecimal.valueOf(1_0000, 4));
    this.setChargeMethod(ChargeDefinition.ChargeMethod.PROPORTIONAL);
    this.setProportionalTo("{principaladjustment}");
    this.setFromAccountDesignator("'entry'");
    this.setAccrualAccountDesignator(null);
    this.setToAccountDesignator("disbursement-fee-income");
    this.setForCycleSizeUnit(null);
    this.setReadOnly(false);
    this.setForSegmentSet("disbursement_ranges");
    this.setFromSegment("larger");
    this.setToSegment("larger");
  }};

  private final ScheduledChargeComparatorTest.TestCase testCase;

  public ScheduledChargeComparatorTest(final ScheduledChargeComparatorTest.TestCase testCase) {
    this.testCase = testCase;
  }

  @Test
  public void compare() {
    Assert.assertEquals(testCase.expected == 0, ScheduledChargeComparator.compareScheduledCharges(testCase.a, testCase.b) == 0);
    Assert.assertEquals(testCase.expected > 0, ScheduledChargeComparator.compareScheduledCharges(testCase.a, testCase.b) > 0);
    Assert.assertEquals(testCase.expected < 0, ScheduledChargeComparator.compareScheduledCharges(testCase.a, testCase.b) < 0);
  }

}