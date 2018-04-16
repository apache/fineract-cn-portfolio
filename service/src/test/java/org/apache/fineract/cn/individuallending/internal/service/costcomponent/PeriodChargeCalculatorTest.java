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

import org.apache.fineract.cn.individuallending.internal.service.schedule.Period;
import org.apache.fineract.cn.individuallending.internal.service.schedule.ScheduledCharge;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;

import static org.apache.fineract.cn.individuallending.internal.service.Fixture.getPeriod;
import static org.apache.fineract.cn.individuallending.internal.service.Fixture.scheduledInterestBookingCharge;

/**
 * @author Myrle Krantz
 */
@RunWith(Parameterized.class)
public class PeriodChargeCalculatorTest {

  private static class TestCase {
    final String description;
    List<ScheduledCharge> scheduledCharges;
    int precision = 20;
    Map<Period, BigDecimal> expectedPeriodRates;
    private BigDecimal interest;

    private TestCase(final String description) {
      this.description = description;
    }

    TestCase scheduledCharges(final List<ScheduledCharge> newVal) {
      this.scheduledCharges = newVal;
      return this;
    }

    TestCase expectedPeriodRates(final Map<Period, BigDecimal> newVal) {
      this.expectedPeriodRates = newVal;
      return this;
    }

    @Override
    public String toString() {
      return "TestCase{" +
              "description='" + description + '\'' +
              '}';
    }

    TestCase interest(BigDecimal newVal) {
      this.interest = newVal;
      return this;
    }
  }

  @Parameterized.Parameters
  public static Collection testCases() {
    final Collection<TestCase> ret = new ArrayList<>();
    ret.add(simpleCase());
    ret.add(bitOfCompoundingCase());
    ret.add(zeroInterestPerPeriod());
    return ret;
  }

  private static TestCase simpleCase()
  {
    final LocalDate initialDate = LocalDate.now();
    final List<ScheduledCharge> scheduledCharges = new ArrayList<>();
    scheduledCharges.add(scheduledInterestBookingCharge(initialDate, 0, 0, 1));
    scheduledCharges.add(scheduledInterestBookingCharge(initialDate, 1, 1, 1));

    final BigDecimal dailyInterestRate = BigDecimal.valueOf(0.01)
        .divide(BigDecimal.valueOf(365.2425), 20, BigDecimal.ROUND_HALF_EVEN);

    final Map<Period, BigDecimal> expectedPeriodRates = new HashMap<>();
    expectedPeriodRates.put(getPeriod(initialDate, 0, 1), dailyInterestRate);
    expectedPeriodRates.put(getPeriod(initialDate, 1, 1), dailyInterestRate);

    return new TestCase("simpleCase")
        .interest(BigDecimal.ONE)
        .scheduledCharges(scheduledCharges)
        .expectedPeriodRates(expectedPeriodRates);
  }

  private static TestCase bitOfCompoundingCase()
  {
    final LocalDate initialDate = LocalDate.now();
    final List<ScheduledCharge> scheduledCharges = new ArrayList<>();
    scheduledCharges.add(scheduledInterestBookingCharge(initialDate, 2, 0, 3));
    scheduledCharges.add(scheduledInterestBookingCharge(initialDate, 4, 2, 2));

    final BigDecimal dailyInterestRate = BigDecimal.valueOf(0.10)
        .divide(BigDecimal.valueOf(365.2425), 20, BigDecimal.ROUND_HALF_EVEN);

    final Map<Period, BigDecimal> expectedPeriodRates = new HashMap<>();
    expectedPeriodRates.put(getPeriod(initialDate, 0, 3), PeriodChargeCalculator.createCompoundedRate(dailyInterestRate, 3, 20));
    expectedPeriodRates.put(getPeriod(initialDate, 2, 2), PeriodChargeCalculator.createCompoundedRate(dailyInterestRate, 2, 20));

    return new TestCase("bitOfCompoundingCase")
        .interest(BigDecimal.TEN)
        .scheduledCharges(scheduledCharges)
        .expectedPeriodRates(expectedPeriodRates);
  }

  private static TestCase zeroInterestPerPeriod()
  {
    final LocalDate initialDate = LocalDate.now();
    final List<ScheduledCharge> scheduledCharges = new ArrayList<>();
    scheduledCharges.add(scheduledInterestBookingCharge(initialDate, 2, 0, 3));
    scheduledCharges.add(scheduledInterestBookingCharge(initialDate, 4, 2, 2));

    final Map<Period, BigDecimal> expectedPeriodRates = new HashMap<>();
    expectedPeriodRates.put(getPeriod(initialDate, 0, 3), BigDecimal.ZERO.setScale(20, BigDecimal.ROUND_UNNECESSARY));
    expectedPeriodRates.put(getPeriod(initialDate, 2, 2), BigDecimal.ZERO.setScale(20, BigDecimal.ROUND_UNNECESSARY));

    return new TestCase("zeroInterestPerPeriod")
        .interest(BigDecimal.ZERO)
        .scheduledCharges(scheduledCharges)
        .expectedPeriodRates(expectedPeriodRates);
  }

  private final TestCase testCase;

  public PeriodChargeCalculatorTest(final TestCase testCase) {
    this.testCase = testCase;
  }

  @Test
  public void getPeriodAccrualRatesTest()
  {
    final Map<Period, BigDecimal> periodRates = PeriodChargeCalculator.getPeriodAccrualInterestRate(testCase.interest, testCase.scheduledCharges, testCase.precision);
    Assert.assertEquals(testCase.expectedPeriodRates, periodRates);
  }
}
