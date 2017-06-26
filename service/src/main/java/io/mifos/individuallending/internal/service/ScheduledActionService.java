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
import io.mifos.individuallending.api.v1.domain.caseinstance.CaseParameters;
import io.mifos.individuallending.api.v1.domain.workflow.Action;
import org.springframework.stereotype.Service;

import javax.annotation.Nonnull;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @author Myrle Krantz
 */
@SuppressWarnings("WeakerAccess")
@Service
public class ScheduledActionService {

  List<ScheduledAction> getHypotheticalScheduledActions(final @Nonnull LocalDate initialDisbursalDate,
                                                                final @Nonnull CaseParameters caseParameters)
  {
    return getHypotheticalScheduledActionsHelper(initialDisbursalDate, caseParameters).collect(Collectors.toList());
  }

  private Stream<ScheduledAction> getHypotheticalScheduledActionsHelper(final @Nonnull LocalDate initialDisbursalDate,
                                                        final @Nonnull CaseParameters caseParameters)
  {
    //'Rough' end date, because if the repayment period takes the last period after that end date, then the repayment
    // period will 'win'.
    final LocalDate roughEndDate = getEndDate(caseParameters, initialDisbursalDate);

    final SortedSet<Period> repaymentPeriods = generateRepaymentPeriods(initialDisbursalDate, roughEndDate, caseParameters);
    final Period firstPeriod = repaymentPeriods.first();
    final Period lastPeriod = repaymentPeriods.last();

    return Stream.concat(Stream.of(
            new ScheduledAction(Action.OPEN, initialDisbursalDate, firstPeriod, firstPeriod),
            new ScheduledAction(Action.APPROVE, initialDisbursalDate, firstPeriod, firstPeriod)),
            Stream.concat(repaymentPeriods.stream().flatMap(this::generateScheduledActionsForRepaymentPeriod),
                    Stream.of(new ScheduledAction(Action.CLOSE, lastPeriod.getEndDate(), lastPeriod, lastPeriod))));
  }

  private LocalDate getEndDate(final @Nonnull CaseParameters caseParameters,
                               final @Nonnull LocalDate initialDisbursalDate) {
    final Integer maximumTermSize = caseParameters.getTermRange().getMaximum();
    final ChronoUnit termUnit = caseParameters.getTermRange().getTemporalUnit();

    return initialDisbursalDate.plus(
            maximumTermSize,
            termUnit);
  }

  private Stream<ScheduledAction> generateScheduledActionsForRepaymentPeriod(final @Nonnull Period repaymentPeriod) {
    return Stream.concat(generateScheduledInterestPaymentsForRepaymentPeriod(repaymentPeriod),
            Stream.of(new ScheduledAction(Action.ACCEPT_PAYMENT, repaymentPeriod.getEndDate(), repaymentPeriod, repaymentPeriod)));
  }

  private Stream<ScheduledAction> generateScheduledInterestPaymentsForRepaymentPeriod(final @Nonnull Period repaymentPeriod) {
    return getInterestDayInRepaymentPeriod(repaymentPeriod).map(x ->
            new ScheduledAction(Action.APPLY_INTEREST, x, new Period(x.minus(1, ChronoUnit.DAYS), x), repaymentPeriod));
  }

  private Stream<LocalDate> getInterestDayInRepaymentPeriod(final @Nonnull Period repaymentPeriod) {
    return Stream.iterate(repaymentPeriod.getBeginDate().plusDays(1), date -> date.plusDays(1))
            .limit(ChronoUnit.DAYS.between(repaymentPeriod.getBeginDate(), repaymentPeriod.getEndDate()));
  }

  private SortedSet<Period> generateRepaymentPeriods(
          final LocalDate initialDisbursalDate,
          final LocalDate endDate,
          final CaseParameters caseParameters) {

    final SortedSet<Period> ret = new TreeSet<>();
    LocalDate lastPaymentDate = initialDisbursalDate;
    LocalDate nextPaymentDate = generateNextPaymentDate(caseParameters, initialDisbursalDate);
    while (nextPaymentDate.isBefore(endDate))
    {
      final Period period = new Period(lastPaymentDate, nextPaymentDate);
      ret.add(period);
      lastPaymentDate = nextPaymentDate;
      nextPaymentDate = generateNextPaymentDate(caseParameters, nextPaymentDate);
    }
    ret.add(new Period(lastPaymentDate, nextPaymentDate));

    return ret;
  }

  private LocalDate generateNextPaymentDate(final CaseParameters caseParameters, final LocalDate lastPaymentDate) {
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

  private LocalDate incrementPaymentDate(LocalDate paymentDate, PaymentCycle paymentCycle) {
    return paymentDate.plus(
            paymentCycle.getPeriod(),
            paymentCycle.getTemporalUnit());
  }

  private LocalDate orientPaymentDate(final LocalDate paymentDate, final ChronoUnit maximumSpecifiedAlignmentChronoUnit, PaymentCycle paymentCycle) {
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

  private @Nonnull ChronoUnit min(@Nonnull final ChronoUnit a, @Nonnull final ChronoUnit b) {
    if (a.getDuration().compareTo(b.getDuration()) < 0)
      return a;
    else
      return b;
  }

  private LocalDate orientInYear(final LocalDate paymentDate) {
    return LocalDate.of(paymentDate.getYear(), 1, 1);
  }

  private LocalDate orientInMonth(final LocalDate paymentDate) {
    return LocalDate.of(paymentDate.getYear(), paymentDate.getMonth(), 1);
  }

  private LocalDate orientInWeek(final LocalDate paymentDate) {
    final DayOfWeek dayOfWeek = paymentDate.getDayOfWeek();
    final int dayOfWeekIndex = dayOfWeek.getValue() - 1;
    return paymentDate.minusDays(dayOfWeekIndex);
  }

  private LocalDate alignPaymentDate(final LocalDate paymentDate, final ChronoUnit maximumAlignmentChronoUnit, final PaymentCycle paymentCycle) {
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

  private LocalDate alignInMonths(final LocalDate paymentDate, final PaymentCycle paymentCycle) {
    final Integer alignmentMonth = paymentCycle.getAlignmentMonth();
    if (alignmentMonth == null)
      return paymentDate;

    return paymentDate.plusMonths(alignmentMonth);
  }

  private LocalDate alignInWeeks(final LocalDate paymentDate, final PaymentCycle paymentCycle) {
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

    throw new IllegalStateException("PaymentCycle.alignmentWeek should only ever be 0, 1, 2, or -1.");
  }

  private LocalDate alignInDays(final LocalDate paymentDate, final PaymentCycle paymentCycle) {
    final Integer alignmentDay = paymentCycle.getAlignmentDay();
    if (alignmentDay == null)
      return paymentDate;

    if ((paymentCycle.getAlignmentWeek() != null) || (paymentCycle.getTemporalUnit() == ChronoUnit.WEEKS))
      return alignInDaysOfWeek(paymentDate, alignmentDay);
    else
      return alignInDaysOfMonth(paymentDate, alignmentDay);
  }

  private LocalDate alignInDaysOfWeek(final LocalDate paymentDate, final Integer alignmentDay) {
    final int dayOfWeek = paymentDate.getDayOfWeek().getValue()-1;

    if (dayOfWeek < alignmentDay)
      return paymentDate.plusDays(alignmentDay - dayOfWeek);
    else if (dayOfWeek > alignmentDay)
      return paymentDate.plusDays(7 - (dayOfWeek - alignmentDay));
    else
      return paymentDate;
  }

  private LocalDate alignInDaysOfMonth(final LocalDate paymentDate, final Integer alignmentDay) {
    final int maxDay = YearMonth.of(paymentDate.getYear(), paymentDate.getMonth()).lengthOfMonth()-1;
    return paymentDate.plusDays(Math.min(maxDay, alignmentDay));
  }

  public List<ScheduledAction> getScheduledActions(final @Nonnull LocalDate initialDisbursalDate,
                                                   final CaseParameters caseParameters,
                                                   final Action action,
                                                   final LocalDate time) {
    return getHypotheticalScheduledActionsHelper(initialDisbursalDate, caseParameters)
            .filter(x -> x.actionPeriod.containsDate(time))
            .filter(x -> x.action.equals(action))
            .collect(Collectors.toList());
  }
}