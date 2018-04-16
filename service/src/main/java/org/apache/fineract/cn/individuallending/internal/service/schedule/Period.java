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

import javax.annotation.Nonnull;
import java.time.Duration;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Objects;

/**
 * @author Myrle Krantz
 */
public class Period implements Comparable<Period> {
  final private LocalDate beginDate;
  final private LocalDate endDate;
  final private boolean lastPeriod;

  public Period(final LocalDate beginDate, final LocalDate endDateExclusive) {
    this.beginDate = beginDate;
    this.endDate = endDateExclusive;
    this.lastPeriod = false;
  }

  public Period(final LocalDate beginDate, final LocalDate endDateExclusive, final boolean lastPeriod) {
    this.beginDate = beginDate;
    this.endDate = endDateExclusive;
    this.lastPeriod = lastPeriod;
  }

  public Period(final LocalDate beginDate, final int periodLength) {
    this.beginDate = beginDate;
    this.endDate = beginDate.plusDays(periodLength);
    this.lastPeriod = false;
  }

  public Period(final int periodLength, final LocalDate endDate) {
    this.beginDate = endDate.minusDays(periodLength);
    this.endDate = endDate;
    this.lastPeriod = false;
  }

  public LocalDate getBeginDate() {
    return beginDate;
  }

  public LocalDate getEndDate() {
    return endDate;
  }

  public boolean isLastPeriod() {
    return lastPeriod;
  }

  public Duration getDuration() {
    long days = beginDate.until(endDate, ChronoUnit.DAYS);
    return ChronoUnit.DAYS.getDuration().multipliedBy(days);
  }

  boolean containsDate(final LocalDate date) {
    return this.getBeginDate().compareTo(date) <= 0 && this.getEndDate().compareTo(date) > 0;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    Period period = (Period) o;
    return lastPeriod == period.lastPeriod &&
        Objects.equals(beginDate, period.beginDate) &&
        Objects.equals(endDate, period.endDate);
  }

  @Override
  public int hashCode() {
    return Objects.hash(beginDate, endDate, lastPeriod);
  }

  @Override
  public int compareTo(@Nonnull Period o) {
    int comparison = compareNullableDates(endDate, o.endDate);
    if (comparison != 0)
      return comparison;

    comparison = compareNullableDates(beginDate, o.beginDate);
    if (comparison != 0)
      return comparison;

    if (lastPeriod == o.lastPeriod)
      return 0;
    else if (lastPeriod)
      return -1;
    else
      return 1;
  }

  @SuppressWarnings("ConstantConditions")
  private static int compareNullableDates(final LocalDate x, final LocalDate y) {
    if ((x == null) && (y == null))
      return 0;
    else if ((x == null) && (y != null))
      return -1;
    else if ((x != null) && (y == null))
      return 1;
    else
      return x.compareTo(y);
  }

  @Override
  public String toString() {
    return "Period{" +
        "beginDate=" + beginDate +
        ", endDate=" + endDate +
        ", lastPeriod=" + lastPeriod +
        '}';
  }
}
