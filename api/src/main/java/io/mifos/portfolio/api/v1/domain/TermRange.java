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

import org.hibernate.validator.constraints.Range;

import javax.validation.constraints.NotNull;
import java.time.temporal.ChronoUnit;
import java.util.Objects;

/**
 * @author Myrle Krantz
 */
@SuppressWarnings({"WeakerAccess", "unused"})
public class TermRange {
  @NotNull
  private ChronoUnit temporalUnit;
  @Range(min = 1)
  private Integer maximum;

  public TermRange() {
  }

  public TermRange(ChronoUnit temporalUnit, Integer length) {
    this.temporalUnit = temporalUnit;
    this.maximum = length;
  }

  public ChronoUnit getTemporalUnit() {
    return temporalUnit;
  }

  public void setTemporalUnit(ChronoUnit temporalUnit) {
    this.temporalUnit = temporalUnit;
  }

  public Integer getMaximum() {
    return maximum;
  }

  public void setMaximum(Integer maximum) {
    this.maximum = maximum;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    TermRange termRange = (TermRange) o;
    return temporalUnit == termRange.temporalUnit &&
            Objects.equals(maximum, termRange.maximum);
  }

  @Override
  public int hashCode() {
    return Objects.hash(temporalUnit, maximum);
  }

  @Override
  public String toString() {
    return "TermRange{" +
            "temporalUnit=" + temporalUnit +
            ", maximum=" + maximum +
            '}';
  }
}
