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

import javax.annotation.Nonnull;
import java.time.Duration;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Objects;

/**
 * @author Myrle Krantz
 */
class Period implements Comparable<Period> {
  final private LocalDate beginDate;
  final private LocalDate endDate;

  Period(final LocalDate beginDate, final LocalDate endDateExclusive) {
    this.beginDate = beginDate;
    this.endDate = endDateExclusive;
  }

  Period(final LocalDate beginDate, final int periodLength) {
    this.beginDate = beginDate;
    this.endDate = beginDate.plusDays(periodLength);
  }

  LocalDate getBeginDate() {
    return beginDate;
  }

  LocalDate getEndDate() {
    return endDate;
  }

  Duration getDuration() {
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
    Period that = (Period) o;
    return Objects.equals(beginDate, that.beginDate) &&
            Objects.equals(endDate, that.endDate);
  }

  @Override
  public int hashCode() {
    return Objects.hash(beginDate, endDate);
  }

  @Override
  public int compareTo(@Nonnull Period o) {
    int comparison = endDate.compareTo(o.endDate);
    if (comparison == 0)
      return beginDate.compareTo(o.beginDate);
    else
      return comparison;
  }

  @Override
  public String toString() {
    return "Period{" +
            "beginDate=" + beginDate +
            ", endDate=" + endDate +
            '}';
  }
}
