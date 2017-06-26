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
import org.junit.BeforeClass;
import org.junit.Test;

import java.time.LocalDate;
import java.time.ZoneId;

/**
 * @author Myrle Krantz
 */
public class PeriodTest {
  private static LocalDate today;
  private static LocalDate yesterday;
  private static LocalDate tommorrow;
  private static LocalDate dayAfterTommorrow;

  @BeforeClass
  public static void prepare() {
    today = LocalDate.now(ZoneId.of("UTC"));
    yesterday = today.minusDays(1);
    tommorrow = today.plusDays(1);
    dayAfterTommorrow = tommorrow.plusDays(1);
  }

  @Test
  public void getDuration() throws Exception {
    final Period testSubjectByDates = new Period(today, dayAfterTommorrow);
    Assert.assertEquals(2, testSubjectByDates.getDuration().toDays());

    final Period testSubjectByDuration = new Period(today, 5);
    Assert.assertEquals(5, testSubjectByDuration.getDuration().toDays());
  }

  @Test
  public void containsDate() throws Exception {
    final Period testSubject = new Period(today, 1);

    Assert.assertTrue(testSubject.containsDate(today));
    Assert.assertFalse(testSubject.containsDate(tommorrow));
    Assert.assertFalse(testSubject.containsDate(yesterday));
    Assert.assertFalse(testSubject.containsDate(dayAfterTommorrow));

  }

  @Test
  public void compareTo() throws Exception {
    final Period yesterdayPeriod = new Period(yesterday, today);
    final Period todayPeriod = new Period(today, tommorrow);
    final Period tommorrowPeriod = new Period(tommorrow, dayAfterTommorrow);

    Assert.assertTrue(yesterdayPeriod.compareTo(todayPeriod) < 0);
    Assert.assertTrue(todayPeriod.compareTo(todayPeriod) == 0);
    Assert.assertTrue(tommorrowPeriod.compareTo(todayPeriod) > 0);
  }

}