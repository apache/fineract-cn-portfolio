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
package io.mifos.individuallending.api.v1.domain.product;

import org.hibernate.validator.constraints.Range;

import javax.validation.constraints.NotNull;
import java.time.temporal.ChronoUnit;
import java.util.Objects;

/**
 * @author Myrle Krantz
 */
@SuppressWarnings({"WeakerAccess", "unused"})
public final class Moratorium {
  @NotNull
  private String chargeTask;

  @NotNull
  private ChronoUnit temporalUnit;

  @Range(min = 1)
  private Integer period;

  public Moratorium() {
  }

  public Moratorium(String chargeTask, ChronoUnit temporalUnit, Integer period) {
    this.chargeTask = chargeTask;
    this.temporalUnit = temporalUnit;
    this.period = period;
  }

  public String getChargeTask() {
    return chargeTask;
  }

  public void setChargeTask(String chargeTask) {
    this.chargeTask = chargeTask;
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

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    Moratorium that = (Moratorium) o;
    return Objects.equals(chargeTask, that.chargeTask) &&
            Objects.equals(temporalUnit, that.temporalUnit) &&
            Objects.equals(period, that.period);
  }

  @Override
  public int hashCode() {
    return Objects.hash(chargeTask, temporalUnit, period);
  }

  @Override
  public String toString() {
    return "Moratorium{" +
            "chargeTask=" + chargeTask +
            ", temporalUnit=" + temporalUnit +
            ", period=" + period +
            '}';
  }
}
