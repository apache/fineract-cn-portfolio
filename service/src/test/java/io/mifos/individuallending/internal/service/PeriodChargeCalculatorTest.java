/*
 * Copyright 2017 The Mifos Initiative.
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
package io.mifos.individuallending.internal.service;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Stream;

import static io.mifos.individuallending.internal.service.Fixture.getPeriod;
import static io.mifos.individuallending.internal.service.Fixture.scheduledInterestCharge;

/**
 * @author Myrle Krantz
 */
@RunWith(Parameterized.class)
public class PeriodChargeCalculatorTest {

  private static class TestCase {
    final String description;
    List<ScheduledCharge> scheduledCharges;
    int precision;
    Map<Period, BigDecimal> expectedPeriodRates;

    private TestCase(final String description) {
      this.description = description;
    }

    TestCase scheduledCharges(final List<ScheduledCharge> newVal) {
      this.scheduledCharges = newVal;
      return this;
    }

    TestCase precision(final int newVal) {
      this.precision = newVal;
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
    scheduledCharges.add(scheduledInterestCharge(0.01, initialDate, 0, 0, 1));
    scheduledCharges.add(scheduledInterestCharge(0.01, initialDate, 1, 1, 1));

    final BigDecimal dailyInterestRate = BigDecimal.valueOf(0.01)
            .divide(BigDecimal.valueOf(365.2425), 20, BigDecimal.ROUND_HALF_EVEN);

    final Map<Period, BigDecimal> expectedPeriodRates = new HashMap<>();
    expectedPeriodRates.put(getPeriod(initialDate, 0, 1), dailyInterestRate);
    expectedPeriodRates.put(getPeriod(initialDate, 1, 1), dailyInterestRate);

    return new TestCase("simpleCase")
            .scheduledCharges(scheduledCharges)
            .precision(20)
            .expectedPeriodRates(expectedPeriodRates);
  }

  private static TestCase bitOfCompoundingCase()
  {
    final LocalDate initialDate = LocalDate.now();
    final List<ScheduledCharge> scheduledCharges = new ArrayList<>();
    scheduledCharges.add(scheduledInterestCharge(0.01, initialDate, 0, 0, 3));
    scheduledCharges.add(scheduledInterestCharge(0.01, initialDate, 1, 0, 3));
    scheduledCharges.add(scheduledInterestCharge(0.01, initialDate, 2, 0, 3));
    scheduledCharges.add(scheduledInterestCharge(0.01, initialDate, 3, 2, 2));
    scheduledCharges.add(scheduledInterestCharge(0.01, initialDate, 4, 2, 2));

    final BigDecimal dailyInterestRate = BigDecimal.valueOf(0.01)
            .divide(BigDecimal.valueOf(365.2425), 20, BigDecimal.ROUND_HALF_EVEN);

    final Map<Period, BigDecimal> expectedPeriodRates = new HashMap<>();
    expectedPeriodRates.put(getPeriod(initialDate, 0, 3), createCompoundedInterestRate(dailyInterestRate, 3, 20));
    expectedPeriodRates.put(getPeriod(initialDate, 2, 2), createCompoundedInterestRate(dailyInterestRate, 2, 20));

    return new TestCase("bitOfCompoundingCase")
            .scheduledCharges(scheduledCharges)
            .precision(20)
            .expectedPeriodRates(expectedPeriodRates);
  }

  private static TestCase zeroInterestPerPeriod()
  {
    final LocalDate initialDate = LocalDate.now();
    final List<ScheduledCharge> scheduledCharges = new ArrayList<>();
    scheduledCharges.add(scheduledInterestCharge(0.00, initialDate, 0, 0, 3));
    scheduledCharges.add(scheduledInterestCharge(0.00, initialDate, 1, 0, 3));
    scheduledCharges.add(scheduledInterestCharge(0.00, initialDate, 2, 0, 3));
    scheduledCharges.add(scheduledInterestCharge(0.00, initialDate, 3, 2, 2));
    scheduledCharges.add(scheduledInterestCharge(0.00, initialDate, 4, 2, 2));

    final Map<Period, BigDecimal> expectedPeriodRates = new HashMap<>();
    expectedPeriodRates.put(getPeriod(initialDate, 0, 3), BigDecimal.ZERO.setScale(20, BigDecimal.ROUND_UNNECESSARY));
    expectedPeriodRates.put(getPeriod(initialDate, 2, 2), BigDecimal.ZERO.setScale(20, BigDecimal.ROUND_UNNECESSARY));

    return new TestCase("zeroInterestPerPeriod")
            .scheduledCharges(scheduledCharges)
            .precision(20)
            .expectedPeriodRates(expectedPeriodRates);
  }

  private static BigDecimal createCompoundedInterestRate(BigDecimal interestRate, int periodCount, int precision)
  {
    return Stream.generate(() -> interestRate).limit(periodCount).collect(RateCollectors.compound(precision));
  }

  private final TestCase testCase;

  public PeriodChargeCalculatorTest(final TestCase testCase) {
    this.testCase = testCase;
  }

  @Test
  public void test()
  {
    final PeriodChargeCalculator testSubject = new PeriodChargeCalculator();
    final Map<Period, BigDecimal> periodRates = testSubject.getPeriodAccrualRates(testCase.scheduledCharges, testCase.precision);
    Assert.assertEquals(testCase.expectedPeriodRates, periodRates);
  }
}
