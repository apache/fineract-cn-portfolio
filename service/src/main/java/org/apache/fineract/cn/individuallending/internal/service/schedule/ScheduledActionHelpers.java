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

import org.apache.fineract.cn.individuallending.api.v1.domain.caseinstance.CaseParameters;
import org.apache.fineract.cn.individuallending.api.v1.domain.workflow.Action;
import org.apache.fineract.cn.portfolio.api.v1.domain.PaymentCycle;

import javax.annotation.Nonnull;
import java.time.*;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @author Myrle Krantz
 */
@SuppressWarnings("WeakerAccess")
public class ScheduledActionHelpers {
  public static boolean actionHasNoActionPeriod(final Action action) {
    return preTermActions().anyMatch(x -> action == x) || postTermActions().anyMatch(x -> action == x);
  }

  private static Stream<Action> preTermActions() {
    return Stream.of(Action.OPEN, Action.APPROVE, Action.DISBURSE);
  }

  private static Stream<Action> postTermActions() {
    return Stream.of(Action.CLOSE);
  }

  public static List<ScheduledAction> getHypotheticalScheduledActions(final @Nonnull LocalDate startOfTerm,
                                                        final @Nonnull CaseParameters caseParameters)
  {
    final LocalDate endOfTerm = getRoughEndDate(startOfTerm, caseParameters);
    return Stream.concat( Stream.concat(
          preTermActions().map(action -> new ScheduledAction(action, startOfTerm)),
          getHypotheticalScheduledActionsForDisbursedLoan(startOfTerm, endOfTerm, caseParameters)),
          postTermActions().map(action -> new ScheduledAction(action, endOfTerm)))
        .collect(Collectors.toList());
  }

  public static ScheduledAction getNextScheduledPayment(final @Nonnull LocalDate startOfTerm,
                                                        final @Nonnull LocalDate fromDate,
                                                        final @Nonnull LocalDate endOfTerm,
                                                        final @Nonnull CaseParameters caseParameters) {
    final LocalDate effectiveEndOfTerm = fromDate.isAfter(endOfTerm) ? fromDate : endOfTerm;

    return getHypotheticalScheduledActionsForDisbursedLoan(startOfTerm, effectiveEndOfTerm, caseParameters)
        .filter(x -> x.getAction().equals(Action.ACCEPT_PAYMENT))
        .filter(x -> x.actionIsOnOrAfter(fromDate))
        .findFirst()
        .orElseGet(() -> new ScheduledAction(Action.ACCEPT_PAYMENT, fromDate));
  }

  private static Stream<ScheduledAction> getHypotheticalScheduledActionsForDisbursedLoan(
      final @Nonnull LocalDate startOfTerm,
      final @Nonnull LocalDate endOfTerm,
      final @Nonnull CaseParameters caseParameters)
  {
    return generateRepaymentPeriods(startOfTerm, endOfTerm, caseParameters)
        .flatMap(ScheduledActionHelpers::generateScheduledActionsForRepaymentPeriod);
  }

  /** 'Rough' end date, because if the repayment period takes the last period after that end date, then the repayment
   period will 'win'.*/

  public static LocalDate getRoughEndDate(final @Nonnull LocalDate startOfTerm,
                                          final @Nonnull CaseParameters caseParameters) {
    final Integer maximumTermSize = caseParameters.getTermRange().getMaximum();
    final ChronoUnit termUnit = caseParameters.getTermRange().getTemporalUnit();

    return startOfTerm.plus(
            maximumTermSize,
            termUnit);
  }

  private static Stream<ScheduledAction> generateScheduledActionsForRepaymentPeriod(final @Nonnull Period repaymentPeriod) {
    return Stream.concat(generateScheduledInterestPaymentsForRepaymentPeriod(repaymentPeriod),
            Stream.of(new ScheduledAction(Action.ACCEPT_PAYMENT, repaymentPeriod.getEndDate(), repaymentPeriod, repaymentPeriod)));
  }

  private static Stream<ScheduledAction> generateScheduledInterestPaymentsForRepaymentPeriod(final @Nonnull Period repaymentPeriod) {
    return getInterestDayInRepaymentPeriod(repaymentPeriod).map(x ->
            new ScheduledAction(Action.APPLY_INTEREST, x, new Period(x.minus(1, ChronoUnit.DAYS), x), repaymentPeriod));
  }

  private static Stream<LocalDate> getInterestDayInRepaymentPeriod(final @Nonnull Period repaymentPeriod) {
    return Stream.iterate(repaymentPeriod.getBeginDate().plusDays(1), date -> date.plusDays(1))
            .limit(ChronoUnit.DAYS.between(repaymentPeriod.getBeginDate(), repaymentPeriod.getEndDate()));
  }

  public static Stream<Period> generateRepaymentPeriods(
      final LocalDate startOfTerm,
      final LocalDate endOfTerm,
      final CaseParameters caseParameters) {

    final List<Period> ret = new ArrayList<>();
    LocalDate lastPaymentDate = startOfTerm;
    LocalDate nextPaymentDate = generateNextPaymentDate(caseParameters, lastPaymentDate);
    while (nextPaymentDate.isBefore(endOfTerm))
    {
      final Period period = new Period(lastPaymentDate, nextPaymentDate);
      ret.add(period);
      lastPaymentDate = nextPaymentDate;
      nextPaymentDate = generateNextPaymentDate(caseParameters, lastPaymentDate);
    }
    ret.add(new Period(lastPaymentDate, nextPaymentDate, true));

    return ret.stream();
  }

  private static LocalDate generateNextPaymentDate(final CaseParameters caseParameters, final LocalDate lastPaymentDate) {
    final PaymentCycle paymentCycle = caseParameters.getPaymentCycle();

    final ChronoUnit maximumSpecifiedAlignmentChronoUnit =
            paymentCycle.getAlignmentMonth() != null ? ChronoUnit.MONTHS :
            paymentCycle.getAlignmentWeek() != null ? ChronoUnit.WEEKS :
            paymentCycle.getAlignmentDay() != null ? ChronoUnit.DAYS :
            ChronoUnit.HOURS;

    final ChronoUnit maximumPossibleAlignmentChronoUnit =
            paymentCycle.getTemporalUnit().equals(ChronoUnit.YEARS) ? ChronoUnit.MONTHS :
            paymentCycle.getTemporalUnit().equals(ChronoUnit.MONTHS) ? ChronoUnit.WEEKS :
            paymentCycle.getTemporalUnit().equals(ChronoUnit.WEEKS) ? ChronoUnit.DAYS :
            ChronoUnit.HOURS; //Hours as a placeholder.

    final ChronoUnit maximumAlignmentChronoUnit = min(maximumSpecifiedAlignmentChronoUnit, maximumPossibleAlignmentChronoUnit);


    final LocalDate incrementedPaymentDate = incrementPaymentDate(lastPaymentDate, paymentCycle);
    final LocalDate orientedPaymentDate = orientPaymentDate(incrementedPaymentDate, maximumSpecifiedAlignmentChronoUnit, paymentCycle);
    return alignPaymentDate(orientedPaymentDate, maximumAlignmentChronoUnit, paymentCycle);
  }

  private static LocalDate incrementPaymentDate(final LocalDate paymentDate, final PaymentCycle paymentCycle) {
    return paymentDate.plus(
        paymentCycle.getPeriod(),
        paymentCycle.getTemporalUnit());
  }

  private static LocalDate orientPaymentDate(final LocalDate paymentDate, final ChronoUnit maximumSpecifiedAlignmentChronoUnit, PaymentCycle paymentCycle) {
    if (maximumSpecifiedAlignmentChronoUnit == ChronoUnit.HOURS)
      return paymentDate; //No need to orient at all since no alignment is specified.

    switch (paymentCycle.getTemporalUnit())
    {
      case YEARS:
        return orientInYear(paymentDate);
      case MONTHS:
        return orientInMonth(paymentDate);
      case WEEKS:
        return orientInWeek(paymentDate);
      default:
      case DAYS:
        return paymentDate;
    }
  }

  private static @Nonnull ChronoUnit min(@Nonnull final ChronoUnit a, @Nonnull final ChronoUnit b) {
    if (a.getDuration().compareTo(b.getDuration()) < 0)
      return a;
    else
      return b;
  }

  private static LocalDate orientInYear(final LocalDate paymentDate) {
    return LocalDate.of(paymentDate.getYear(), 1, 1);
  }

  private static LocalDate orientInMonth(final LocalDate paymentDate) {
    return LocalDate.of(paymentDate.getYear(), paymentDate.getMonth(), 1);
  }

  private static LocalDate orientInWeek(final LocalDate paymentDate) {
    final DayOfWeek dayOfWeek = paymentDate.getDayOfWeek();
    final int dayOfWeekIndex = dayOfWeek.getValue() - 1;
    return paymentDate.minusDays(dayOfWeekIndex);
  }

  private static LocalDate alignPaymentDate(final LocalDate paymentDate, final ChronoUnit maximumAlignmentChronoUnit, final PaymentCycle paymentCycle) {
    LocalDate ret = paymentDate;
    switch (maximumAlignmentChronoUnit)
    {
      case MONTHS:
        ret = alignInMonths(ret, paymentCycle);
      case WEEKS:
        ret = alignInWeeks(ret, paymentCycle);
      case DAYS:
        ret = alignInDays(ret, paymentCycle);
      default:
      case HOURS:
        return ret;
    }
  }

  private static LocalDate alignInMonths(final LocalDate paymentDate, final PaymentCycle paymentCycle) {
    final Integer alignmentMonth = paymentCycle.getAlignmentMonth();
    if (alignmentMonth == null)
      return paymentDate;

    return paymentDate.plusMonths(alignmentMonth);
  }

  private static LocalDate alignInWeeks(final LocalDate paymentDate, final PaymentCycle paymentCycle) {
    final Integer alignmentWeek = paymentCycle.getAlignmentWeek();
    if (alignmentWeek == null)
      return paymentDate;
    if ((alignmentWeek == 0) || (alignmentWeek == 1) || (alignmentWeek == 2))
      return paymentDate.plusWeeks(alignmentWeek);
    if (alignmentWeek == -1)
    {
      final LocalDate lastDayOfMonth = YearMonth.of(paymentDate.getYear(), paymentDate.getMonth()).atEndOfMonth();
      int dayOfWeek = lastDayOfMonth.getDayOfWeek().getValue() - 1;
      if (paymentCycle.getAlignmentDay() == null || dayOfWeek == paymentCycle.getAlignmentDay()) {
        return lastDayOfMonth;
      }
      else
        return lastDayOfMonth.minus(7, ChronoUnit.DAYS); //Will align days in next step.
    }

    throw new IllegalStateException("PaymentCycle.alignmentWeek should only ever be 0, 1, 2, or -1, but was " + alignmentWeek);
  }

  static private LocalDate alignInDays(final LocalDate paymentDate, final PaymentCycle paymentCycle) {
    final Integer alignmentDay = paymentCycle.getAlignmentDay();
    if (alignmentDay == null)
      return paymentDate;

    if ((paymentCycle.getAlignmentWeek() != null) || (paymentCycle.getTemporalUnit() == ChronoUnit.WEEKS))
      return alignInDaysOfWeek(paymentDate, alignmentDay);
    else
      return alignInDaysOfMonth(paymentDate, alignmentDay);
  }

  static private LocalDate alignInDaysOfWeek(final LocalDate paymentDate, final Integer alignmentDay) {
    final int dayOfWeek = paymentDate.getDayOfWeek().getValue()-1;

    if (dayOfWeek < alignmentDay)
      return paymentDate.plusDays(alignmentDay - dayOfWeek);
    else if (dayOfWeek > alignmentDay)
      return paymentDate.plusDays(7 - (dayOfWeek - alignmentDay));
    else
      return paymentDate;
  }

  private static LocalDate alignInDaysOfMonth(final LocalDate paymentDate, final Integer alignmentDay) {
    final int maxDay = YearMonth.of(paymentDate.getYear(), paymentDate.getMonth()).lengthOfMonth()-1;
    return paymentDate.plusDays(Math.min(maxDay, alignmentDay));
  }

  public static Optional<Duration> getAccrualPeriodDurationForAction(final Action action) {
    if (action == Action.APPLY_INTEREST)
      return Optional.of(ChronoUnit.DAYS.getDuration());
    else
      return Optional.empty();
  }
}