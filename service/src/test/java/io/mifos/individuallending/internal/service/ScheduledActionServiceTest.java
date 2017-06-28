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

import io.mifos.portfolio.api.v1.domain.PaymentCycle;
import io.mifos.portfolio.api.v1.domain.TermRange;
import io.mifos.individuallending.api.v1.domain.caseinstance.CaseParameters;
import io.mifos.individuallending.api.v1.domain.workflow.Action;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

import static io.mifos.individuallending.internal.service.Fixture.*;

/**
 * @author Myrle Krantz
 */
@RunWith(Parameterized.class)
public class ScheduledActionServiceTest {
  private static class TestCase
  {
    final String description;
    LocalDate initialDisbursementDate;
    CaseParameters caseParameters;
    List<ScheduledAction> expectedResultContents = Collections.emptyList();
    long expectedPaymentCount;
    long expectedInterestCount;
    private LocalDate earliestActionDate;
    private LocalDate latestActionDate;

    TestCase(final String description) {
      this.description = description;
    }

    TestCase initialDisbursementDate(final LocalDate newVal) {
      this.initialDisbursementDate = newVal;
      return this;
    }

    TestCase caseParameters(final CaseParameters newVal) {
      this.caseParameters = newVal;
      return this;
    }

    TestCase expectedResultContents(final List<ScheduledAction> newVal) {
      this.expectedResultContents = newVal;
      return this;
    }

    TestCase expectedDateRangeOfSchedule(final LocalDate from, final LocalDate to) {
      this.earliestActionDate = from;
      this.latestActionDate = to;
      return this;
    }

    TestCase expectedPaymentCount(final long newVal) {
      this.expectedPaymentCount = newVal;
      return this;
    }

    TestCase expectedInterestCount(final long newVal) {
      this.expectedInterestCount = newVal;
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
    ret.add(simpleTestCase());
    ret.add(monthlyPaymentNotAlignedCase());
    ret.add(monthlyPaymentAlignedByDayCase());
    ret.add(biWeeklyPaymentAlignedByDayOfWeekCase());
    ret.add(endOfMonthLeapYearPaymentCase());
    ret.add(firstMondayQuarterlyPaymentCase());
    ret.add(lastMondayQuarterlyPaymentCase());
    ret.add(lastWeekNoDayCase());
    ret.add(yearlyPaymentCase());
    ret.add(yearlyPaymentFirstDayOfSecondMonthCase());
    ret.add(yearlyPaymentThirdMonthCase());
    return ret;
  }

  private static TestCase simpleTestCase()
  {
    final LocalDate initialDisbursementDate = LocalDate.of(2017, 1, 20);
    final Period firstRepaymentPeriod = new Period(initialDisbursementDate, 1);

    return new TestCase("simple").caseParameters(getTestCaseParameters()).initialDisbursementDate(initialDisbursementDate)
            .expectedResultContents(Collections.singletonList(scheduledInterestAction(initialDisbursementDate, 1, firstRepaymentPeriod)))
            .expectedInterestCount(2)
            .expectedPaymentCount(2)
            .expectedDateRangeOfSchedule(initialDisbursementDate, initialDisbursementDate.plusDays(2));
  }

  private static TestCase monthlyPaymentNotAlignedCase()
  {
    final LocalDate initialDisbursementDate = LocalDate.of(2017, 1, 20);
    final CaseParameters caseParameters = getTestCaseParameters();
    caseParameters.setTermRange(new TermRange(ChronoUnit.YEARS, 1));
    caseParameters.setPaymentCycle(new PaymentCycle(ChronoUnit.MONTHS, 1, null, null, null));

    return new TestCase("monthlyPaymentNotAligned")
            .caseParameters(caseParameters)
            .initialDisbursementDate(initialDisbursementDate)
            .expectedInterestCount(365)
            .expectedPaymentCount(12)
            .expectedDateRangeOfSchedule(initialDisbursementDate, initialDisbursementDate.plusDays(365));
  }

  private static TestCase monthlyPaymentAlignedByDayCase()
  {
    final LocalDate initialDisbursementDate = LocalDate.of(2017, 1, 20);
    final CaseParameters caseParameters = getTestCaseParameters();
    caseParameters.setTermRange(new TermRange(ChronoUnit.YEARS, 1));
    caseParameters.setPaymentCycle(new PaymentCycle(ChronoUnit.MONTHS, 1, 0, null, null));

    return new TestCase("monthlyPaymentAlignedByDay")
            .caseParameters(caseParameters)
            .initialDisbursementDate(initialDisbursementDate)
            .expectedResultContents(scheduledRepaymentActions(initialDisbursementDate,
                    LocalDate.of(2017, 2, 1),
                    LocalDate.of(2017, 3, 1),
                    LocalDate.of(2017, 4, 1),
                    LocalDate.of(2017, 5, 1),
                    LocalDate.of(2017, 6, 1),
                    LocalDate.of(2017, 7, 1),
                    LocalDate.of(2017, 8, 1),
                    LocalDate.of(2017, 9, 1),
                    LocalDate.of(2017, 10, 1),
                    LocalDate.of(2017, 11, 1),
                    LocalDate.of(2017, 12, 1),
                    LocalDate.of(2018, 1, 1),
                    LocalDate.of(2018, 2, 1)))
            .expectedInterestCount(377)
            .expectedPaymentCount(13)
            .expectedDateRangeOfSchedule(initialDisbursementDate, LocalDate.of(2018, 2, 1));
  }

  private static TestCase biWeeklyPaymentAlignedByDayOfWeekCase()
  {
    final LocalDate initialDisbursementDate = LocalDate.of(2017, 1, 20);
    final CaseParameters caseParameters = getTestCaseParameters();
    caseParameters.setTermRange(new TermRange(ChronoUnit.MONTHS, 3));
    caseParameters.setPaymentCycle(new PaymentCycle(ChronoUnit.WEEKS, 2, 3, null, null));


    return new TestCase("biWeeklyPaymentAlignedByDayOfWeekCase")
            .caseParameters(caseParameters)
            .initialDisbursementDate(initialDisbursementDate)
            .expectedResultContents(scheduledRepaymentActions(initialDisbursementDate,
                    LocalDate.of(2017, 2, 2),
                    LocalDate.of(2017, 2, 16),
                    LocalDate.of(2017, 3, 2),
                    LocalDate.of(2017, 3, 16),
                    LocalDate.of(2017, 3, 30),
                    LocalDate.of(2017, 4, 13),
                    LocalDate.of(2017, 4, 27)))
            .expectedInterestCount(97)
            .expectedPaymentCount(7)
            .expectedDateRangeOfSchedule(initialDisbursementDate, LocalDate.of(2018, 4, 27));
  }

  private static TestCase endOfMonthLeapYearPaymentCase()
  {
    final LocalDate initialDisbursementDate = LocalDate.of(2019, 12, 31);
    final CaseParameters caseParameters = getTestCaseParameters();
    caseParameters.setTermRange(new TermRange(ChronoUnit.YEARS, 1));
    caseParameters.setPaymentCycle(new PaymentCycle(ChronoUnit.MONTHS, 1, 31, null, null));


    return new TestCase("endOfMonthLeapYearPaymentCase")
            .caseParameters(caseParameters)
            .initialDisbursementDate(initialDisbursementDate)
            .expectedResultContents(scheduledRepaymentActions(initialDisbursementDate,
                    LocalDate.of(2020, 1, 31),
                    LocalDate.of(2020, 2, 29),
                    LocalDate.of(2020, 3, 31),
                    LocalDate.of(2020, 4, 30),
                    LocalDate.of(2020, 5, 31),
                    LocalDate.of(2020, 6, 30),
                    LocalDate.of(2020, 7, 31),
                    LocalDate.of(2020, 8, 31),
                    LocalDate.of(2020, 9, 30),
                    LocalDate.of(2020, 10, 31),
                    LocalDate.of(2020, 11, 30),
                    LocalDate.of(2020, 12, 31)))
            .expectedInterestCount(366)
            .expectedPaymentCount(12)
            .expectedDateRangeOfSchedule(initialDisbursementDate, LocalDate.of(2020, 12, 31));

  }

  private static TestCase firstMondayQuarterlyPaymentCase()
  {
    final LocalDate initialDisbursementDate = LocalDate.of(2017, 1, 1);
    final CaseParameters caseParameters = getTestCaseParameters();
    caseParameters.setTermRange(new TermRange(ChronoUnit.YEARS, 2));
    caseParameters.setPaymentCycle(new PaymentCycle(ChronoUnit.MONTHS, 3, 0, 0, null));


    return new TestCase("firstMondayQuarterlyPaymentCase")
            .caseParameters(caseParameters)
            .initialDisbursementDate(initialDisbursementDate)
            .expectedResultContents(scheduledRepaymentActions(initialDisbursementDate,
                    LocalDate.of(2017, 4, 3),
                    LocalDate.of(2017, 7, 3),
                    LocalDate.of(2017, 10, 2),
                    LocalDate.of(2018, 1, 1),
                    LocalDate.of(2018, 4, 2),
                    LocalDate.of(2018, 7, 2),
                    LocalDate.of(2018, 10, 1),
                    LocalDate.of(2019, 1, 7)))
            .expectedInterestCount(736)
            .expectedPaymentCount(8)
            .expectedDateRangeOfSchedule(initialDisbursementDate, LocalDate.of(2019, 4, 1));

  }

  private static TestCase lastMondayQuarterlyPaymentCase()
  {
    final LocalDate initialDisbursementDate = LocalDate.of(2017, 1, 1);
    final CaseParameters caseParameters = getTestCaseParameters();
    caseParameters.setTermRange(new TermRange(ChronoUnit.YEARS, 2));
    caseParameters.setPaymentCycle(new PaymentCycle(ChronoUnit.MONTHS, 3, 0, -1, null));

    return new TestCase("lastMondayQuarterlyPaymentCase")
            .caseParameters(caseParameters)
            .initialDisbursementDate(initialDisbursementDate)
            .expectedResultContents(scheduledRepaymentActions(initialDisbursementDate,
                    LocalDate.of(2017, 4, 24),
                    LocalDate.of(2017, 7, 31),
                    LocalDate.of(2017, 10, 30),
                    LocalDate.of(2018, 1, 29),
                    LocalDate.of(2018, 4, 30),
                    LocalDate.of(2018, 7, 30),
                    LocalDate.of(2018, 10, 29),
                    LocalDate.of(2019, 1, 28)))
            .expectedInterestCount(757)
            .expectedPaymentCount(8)
            .expectedDateRangeOfSchedule(initialDisbursementDate, LocalDate.of(2019, 1, 28));
  }

  private static TestCase lastWeekNoDayCase()
  {
    final LocalDate initialDisbursementDate = LocalDate.of(2017, 1, 1);
    final CaseParameters caseParameters = getTestCaseParameters();
    caseParameters.setTermRange(new TermRange(ChronoUnit.YEARS, 2));
    caseParameters.setPaymentCycle(new PaymentCycle(ChronoUnit.MONTHS, 3, null, -1, null));

    return new TestCase("lastWeekNoDayCase")
            .caseParameters(caseParameters)
            .initialDisbursementDate(initialDisbursementDate)
            .expectedResultContents(scheduledRepaymentActions(initialDisbursementDate,
                    LocalDate.of(2017, 4, 30),
                    LocalDate.of(2017, 7, 31),
                    LocalDate.of(2017, 10, 31),
                    LocalDate.of(2018, 1, 31),
                    LocalDate.of(2018, 4, 30),
                    LocalDate.of(2018, 7, 31),
                    LocalDate.of(2018, 10, 31),
                    LocalDate.of(2019, 1, 31)))
            .expectedInterestCount(760)
            .expectedPaymentCount(8)
            .expectedDateRangeOfSchedule(initialDisbursementDate, LocalDate.of(2019, 1, 31));
  }

  private static TestCase yearlyPaymentCase()
  {
    final LocalDate initialDisbursementDate = LocalDate.of(2017, 1, 5);
    final CaseParameters caseParameters = getTestCaseParameters();
    caseParameters.setTermRange(new TermRange(ChronoUnit.YEARS, 3));
    caseParameters.setPaymentCycle(new PaymentCycle(ChronoUnit.YEARS, 1, null, null, null));


    return new TestCase("yearlyPaymentCase")
            .caseParameters(caseParameters)
            .initialDisbursementDate(initialDisbursementDate)
            .expectedResultContents(scheduledRepaymentActions(initialDisbursementDate,
                    LocalDate.of(2018, 1, 5),
                    LocalDate.of(2019, 1, 5),
                    LocalDate.of(2020, 1, 5)))
            .expectedInterestCount(1095)
            .expectedPaymentCount(3)
            .expectedDateRangeOfSchedule(initialDisbursementDate, LocalDate.of(2020, 1, 5));
  }

  private static TestCase yearlyPaymentFirstDayOfSecondMonthCase()
  {
    final LocalDate initialDisbursementDate = LocalDate.of(2017, 1, 5);
    final CaseParameters caseParameters = getTestCaseParameters();
    caseParameters.setTermRange(new TermRange(ChronoUnit.YEARS, 3));
    caseParameters.setPaymentCycle(new PaymentCycle(ChronoUnit.YEARS, 1, 0, null, 1));


    return new TestCase("yearlyPaymentFirstDayOfSecondMonthCase")
            .caseParameters(caseParameters)
            .initialDisbursementDate(initialDisbursementDate)
            .expectedResultContents(scheduledRepaymentActions(initialDisbursementDate,
                    LocalDate.of(2018, 2, 1),
                    LocalDate.of(2019, 2, 1),
                    LocalDate.of(2020, 2, 1)))
            .expectedInterestCount(1122)
            .expectedPaymentCount(3)
            .expectedDateRangeOfSchedule(initialDisbursementDate, LocalDate.of(2020, 2, 1));
  }

  private static TestCase yearlyPaymentThirdMonthCase()
  {
    final LocalDate initialDisbursementDate = LocalDate.of(2017, 1, 5);
    final CaseParameters caseParameters = getTestCaseParameters();
    caseParameters.setTermRange(new TermRange(ChronoUnit.YEARS, 5));
    caseParameters.setPaymentCycle(new PaymentCycle(ChronoUnit.YEARS, 1, null, null, 2));


    return new TestCase("yearlyPaymentThirdMonthCase")
            .caseParameters(caseParameters)
            .initialDisbursementDate(initialDisbursementDate)
            .expectedResultContents(scheduledRepaymentActions(initialDisbursementDate,
                    LocalDate.of(2018, 3, 1),
                    LocalDate.of(2019, 3, 1),
                    LocalDate.of(2020, 3, 1),
                    LocalDate.of(2021, 3, 1),
                    LocalDate.of(2022, 3, 1)))
            .expectedInterestCount(1881)
            .expectedPaymentCount(5)
            .expectedDateRangeOfSchedule(initialDisbursementDate, LocalDate.of(2022, 3, 1));
  }

  private final TestCase testCase;

  public ScheduledActionServiceTest(final TestCase testCase)
  {
    this.testCase = testCase;
  }

  @Test
  public void getScheduledActions() throws Exception {
    final ScheduledActionService testSubject = new ScheduledActionService();
    final List<ScheduledAction> result = testSubject.getHypotheticalScheduledActions(testCase.initialDisbursementDate, testCase.caseParameters);

    Assert.assertTrue(testCase.description, result.containsAll(testCase.expectedResultContents));
    result.forEach(x -> {
      Assert.assertTrue(x.toString(), testCase.earliestActionDate.isBefore(x.when) || testCase.earliestActionDate.isEqual(x.when));
      Assert.assertTrue(x.toString(), testCase.latestActionDate.isAfter(x.when) || testCase.latestActionDate.isEqual(x.when));
    });
    Assert.assertEquals(testCase.expectedPaymentCount, countActionsByType(result, Action.ACCEPT_PAYMENT));
    Assert.assertEquals(testCase.expectedInterestCount, countActionsByType(result, Action.APPLY_INTEREST));
    Assert.assertEquals(1, countActionsByType(result, Action.APPROVE));
    Assert.assertEquals(1, countActionsByType(result, Action.CLOSE));
    result.forEach(x -> Assert.assertNotNull(x.actionPeriod));
    result.forEach(x -> Assert.assertNotNull(x.repaymentPeriod));
    Assert.assertTrue(noDuplicatesInResult(result));
    Assert.assertTrue(maximumOneInterestPerDay(result));
  }

  private long countActionsByType(final List<ScheduledAction> scheduledActions, final Action actionToCount) {
    return scheduledActions.stream().filter(x -> x.action == actionToCount)
            .collect(Collectors.counting());
  }

  private boolean maximumOneInterestPerDay(final List<ScheduledAction> result) {
    final List<LocalDate> interestDays = result.stream()
            .filter(x -> x.action == Action.APPLY_INTEREST)
            .map(x -> x.when)
            .collect(Collectors.toList());

    final Set<LocalDate> interestDaysSet = new HashSet<>();
    interestDaysSet.addAll(interestDays);
    return (interestDays.size() == interestDaysSet.size());
  }

  private boolean noDuplicatesInResult(final List<ScheduledAction> result) {
    final Set<ScheduledAction> duplicatesRemoved = new HashSet<>();
    duplicatesRemoved.addAll(result);
    return (duplicatesRemoved.size() == result.size());
  }
}