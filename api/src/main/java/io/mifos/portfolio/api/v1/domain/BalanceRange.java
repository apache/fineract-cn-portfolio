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
import org.hibernate.validator.constraints.ScriptAssert;

import java.math.BigDecimal;
import java.util.Objects;

/**
 * @author Myrle Krantz
 */
@SuppressWarnings({"WeakerAccess", "unused"})
@ScriptAssert(lang = "javascript", script = "_this.maximum != null && _this.minimum != null && _this.maximum.compareTo(_this.minimum) >= 0 && _this.minimum.scale() <= 4 && _this.maximum.scale() <= 4")
public final class BalanceRange {
  @Range(min = 0)
  private BigDecimal minimum;

  @Range(min = 0)
  private BigDecimal maximum;

  public BalanceRange() {
  }

  public BalanceRange(BigDecimal minimum, BigDecimal maximum) {
    this.minimum = minimum;
    this.maximum = maximum;
  }

  public BigDecimal getMinimum() {
    return minimum;
  }

  public void setMinimum(BigDecimal minimum) {
    this.minimum = minimum;
  }

  public BigDecimal getMaximum() {
    return maximum;
  }

  public void setMaximum(BigDecimal maximum) {
    this.maximum = maximum;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    BalanceRange that = (BalanceRange) o;
    return Objects.equals(minimum, that.minimum) &&
            Objects.equals(maximum, that.maximum);
  }

  @Override
  public int hashCode() {
    return Objects.hash(minimum, maximum);
  }

  @Override
  public String toString() {
    return "BalanceRange{" +
            "minimum=" + minimum +
            ", maximum=" + maximum +
            '}';
  }
}
