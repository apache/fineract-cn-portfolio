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
package io.mifos.portfolio.api.v1.domain;

import io.mifos.portfolio.api.v1.validation.ValidPaymentCycleUnit;
import io.mifos.portfolio.api.v1.validation.ValidWeek;
import org.hibernate.validator.constraints.Range;

import javax.annotation.Nullable;
import javax.validation.constraints.NotNull;
import java.time.temporal.ChronoUnit;
import java.util.Objects;

/**
 * @author Myrle Krantz
 */
@SuppressWarnings({"WeakerAccess", "unused"})
public final class PaymentCycle {
  @NotNull
  @ValidPaymentCycleUnit
  private ChronoUnit temporalUnit;

  @Range(min = 1)
  private Integer period;

  //for example 2nd Monday would be a list of {{ChronoUnit.WEEKS, 1}, {ChronUnit.DAYS, 1}}
  //if only frequency matters, these parameters will be null.
  private Integer alignmentDay;

  @ValidWeek
  private Integer alignmentWeek;

  private Integer alignmentMonth;

  public PaymentCycle() {
  }

  public PaymentCycle(ChronoUnit temporalUnit, Integer period, Integer alignmentDay, Integer alignmentWeek, Integer alignmentMonth) {
    this.temporalUnit = temporalUnit;
    this.period = period;
    this.alignmentDay = alignmentDay;
    this.alignmentWeek = alignmentWeek;
    this.alignmentMonth = alignmentMonth;
  }

  public ChronoUnit getTemporalUnit() {
    return temporalUnit;
  }

  public void setTemporalUnit(ChronoUnit temporalUnit) {
    this.temporalUnit = temporalUnit;
  }

  public Integer getPeriod() {
    return period;
  }

  public void setPeriod(Integer period) {
    this.period = period;
  }

  @Nullable
  public Integer getAlignmentDay() {
    return alignmentDay;
  }

  public void setAlignmentDay(@Nullable Integer alignmentDay) {
    this.alignmentDay = alignmentDay;
  }

  @Nullable
  public Integer getAlignmentWeek() {
    return alignmentWeek;
  }

  public void setAlignmentWeek(@Nullable Integer alignmentWeek) {
    this.alignmentWeek = alignmentWeek;
  }

  @Nullable
  public Integer getAlignmentMonth() {
    return alignmentMonth;
  }

  public void setAlignmentMonth(@Nullable Integer alignmentMonth) {
    this.alignmentMonth = alignmentMonth;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    PaymentCycle that = (PaymentCycle) o;
    return temporalUnit == that.temporalUnit &&
            Objects.equals(period, that.period) &&
            Objects.equals(alignmentDay, that.alignmentDay) &&
            Objects.equals(alignmentWeek, that.alignmentWeek) &&
            Objects.equals(alignmentMonth, that.alignmentMonth);
  }

  @Override
  public int hashCode() {
    return Objects.hash(temporalUnit, period, alignmentDay, alignmentWeek, alignmentMonth);
  }

  @Override
  public String toString() {
    return "PaymentCycle{" +
            "temporalUnit=" + temporalUnit +
            ", period=" + period +
            ", alignmentDay=" + alignmentDay +
            ", alignmentWeek=" + alignmentWeek +
            ", alignmentMonth=" + alignmentMonth +
            '}';
  }
}
