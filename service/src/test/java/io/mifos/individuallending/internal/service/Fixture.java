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

import io.mifos.individuallending.api.v1.domain.caseinstance.CaseParameters;
import io.mifos.individuallending.api.v1.domain.product.AccountDesignators;
import io.mifos.individuallending.api.v1.domain.workflow.Action;
import io.mifos.portfolio.api.v1.domain.ChargeDefinition;
import io.mifos.portfolio.api.v1.domain.PaymentCycle;
import io.mifos.portfolio.api.v1.domain.TermRange;
import org.junit.Assert;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static java.math.BigDecimal.ROUND_HALF_EVEN;

/**
 * @author Myrle Krantz
 */
class Fixture {

  static CaseParameters getTestCaseParameters()
  {
    final CaseParameters ret = new CaseParameters(generateRandomIdentifier("fred"));

    ret.setMaximumBalance(fixScale(BigDecimal.valueOf(2000L)));
    ret.setTermRange(new TermRange(ChronoUnit.DAYS, 2));
    ret.setPaymentCycle(new PaymentCycle(ChronoUnit.DAYS, 1, 1, null, null));

    return ret;
  }

  private static String generateRandomIdentifier(final String prefix) {
    //prefix followed by a random positive number with less than 4 digits.
    return prefix + Math.floorMod(Math.abs(new Random().nextInt()), 1000);
  }
  private static BigDecimal fixScale(final BigDecimal bigDecimal)
  {
    return bigDecimal.setScale(4, ROUND_HALF_EVEN);
  }


  static ScheduledAction scheduledInterestAction(
          final LocalDate initialDisbursementDate,
          final int daysIn,
          final Period repaymentPeriod)
  {
    Assert.assertTrue(daysIn >= 1);
    final LocalDate when = initialDisbursementDate.plusDays(daysIn);
    final Period actionPeriod = new Period(initialDisbursementDate.plusDays(daysIn - 1), daysIn);
    return new ScheduledAction(Action.APPLY_INTEREST, when, actionPeriod, repaymentPeriod);
  }

  static List<ScheduledAction> scheduledRepaymentActions(final LocalDate initial, final LocalDate... paymentDates)
  {
    final List<ScheduledAction> ret = new ArrayList<>();
    LocalDate begin = initial;
    for (final LocalDate paymentDate : paymentDates) {
      ret.add(scheduledRepaymentAction(begin, paymentDate));
      begin = paymentDate;
    }
    return ret;
  }

  private static ScheduledAction scheduledRepaymentAction(final LocalDate from, final LocalDate to) {
    final Period repaymentPeriod = new Period(from, to);
    return new ScheduledAction(Action.ACCEPT_PAYMENT, to, repaymentPeriod, repaymentPeriod);
  }

  static ScheduledCharge scheduledInterestCharge(
          final double amount,
          final LocalDate initialDate,
          final int chargeDateDelta,
          final int periodBeginDelta,
          final int periodLength)
  {
    final LocalDate chargeDate = initialDate.plusDays(chargeDateDelta);
    final ScheduledAction scheduledAction = new ScheduledAction(
            Action.APPLY_INTEREST,
            chargeDate,
            new Period(chargeDate, 1),
            getPeriod(initialDate, periodBeginDelta, periodLength));
    final ChargeDefinition chargeDefinition = new ChargeDefinition();
    chargeDefinition.setChargeMethod(ChargeDefinition.ChargeMethod.PROPORTIONAL);
    chargeDefinition.setForCycleSizeUnit(ChronoUnit.YEARS);
    chargeDefinition.setIdentifier("blah");
    chargeDefinition.setAccrueAction(Action.APPLY_INTEREST.name());
    chargeDefinition.setChargeAction(Action.ACCEPT_PAYMENT.name());
    chargeDefinition.setAmount(BigDecimal.valueOf(amount));
    chargeDefinition.setFromAccountDesignator(AccountDesignators.CUSTOMER_LOAN);
    chargeDefinition.setAccrualAccountDesignator(AccountDesignators.INTEREST_ACCRUAL);
    chargeDefinition.setToAccountDesignator(AccountDesignators.INTEREST_INCOME);
    return new ScheduledCharge(scheduledAction, chargeDefinition);
  }

  static Period getPeriod(final LocalDate initialDate, final int periodBeginDelta, final int periodLength) {
    return new Period(initialDate.plusDays(periodBeginDelta), periodLength);
  }
}
