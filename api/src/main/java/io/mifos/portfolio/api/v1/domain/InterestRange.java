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

import org.hibernate.validator.constraints.ScriptAssert;

import javax.validation.constraints.DecimalMax;
import javax.validation.constraints.DecimalMin;
import java.math.BigDecimal;

/**
 * @author Myrle Krantz
 */
@SuppressWarnings({"WeakerAccess", "unused"})
@ScriptAssert(lang = "javascript", script = "_this.maximum != null && _this.minimum != null && _this.maximum.compareTo(_this.minimum) >= 0 && _this.minimum.scale() <= 2 && _this.maximum.scale() <= 2")
public class InterestRange {
  @DecimalMin(value = "0.00")
  @DecimalMax(value = "999.99")
  private BigDecimal minimum;
  @DecimalMin(value = "0.00")
  @DecimalMax(value = "999.99")
  private BigDecimal maximum;

  public InterestRange() {
  }

  public InterestRange(BigDecimal minimum, BigDecimal maximum) {
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

    InterestRange that = (InterestRange) o;

    return minimum != null ? minimum.equals(that.minimum) : that.minimum == null && (maximum != null ? maximum.equals(that.maximum) : that.maximum == null);

  }

  @Override
  public int hashCode() {
    int result = minimum != null ? minimum.hashCode() : 0;
    result = 31 * result + (maximum != null ? maximum.hashCode() : 0);
    return result;
  }

  @Override
  public String toString() {
    return "InterestRange{" +
            "minimum=" + minimum +
            ", maximum=" + maximum +
            '}';
  }
}
