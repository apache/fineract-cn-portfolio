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
import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;

/**
 * @author Myrle Krantz
 */
@SuppressWarnings({"WeakerAccess", "unused"})
public final class ProductParameters {
  @NotNull
  private List<Moratorium> moratoriums;
  @Range(min = 1)
  private Integer maximumDispersalCount;

  @Range(min = 0)
  private BigDecimal maximumDispersalAmount;

  @Range(min = 0)
  private BigDecimal minimumDispersalAmount;

  public ProductParameters() {
  }

  public ProductParameters(List<Moratorium> moratoriums, Integer maximumDispersalCount, BigDecimal maximumDispersalAmount) {
    this.moratoriums = moratoriums;
    this.maximumDispersalCount = maximumDispersalCount;
    this.maximumDispersalAmount = maximumDispersalAmount;
  }

  public List<Moratorium> getMoratoriums() {
    return moratoriums;
  }

  public void setMoratoriums(List<Moratorium> moratoriums) {
    this.moratoriums = moratoriums;
  }

  public Integer getMaximumDispersalCount() {
    return maximumDispersalCount;
  }

  public void setMaximumDispersalCount(Integer maximumDispersalCount) {
    this.maximumDispersalCount = maximumDispersalCount;
  }

  public BigDecimal getMaximumDispersalAmount() {
    return maximumDispersalAmount;
  }

  public void setMaximumDispersalAmount(BigDecimal maximumDispersalAmount) {
    this.maximumDispersalAmount = maximumDispersalAmount;
  }

  public BigDecimal getMinimumDispersalAmount() {
    return minimumDispersalAmount;
  }

  public void setMinimumDispersalAmount(BigDecimal minimumDispersalAmount) {
    this.minimumDispersalAmount = minimumDispersalAmount;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    ProductParameters that = (ProductParameters) o;
    return Objects.equals(moratoriums, that.moratoriums) &&
            Objects.equals(maximumDispersalCount, that.maximumDispersalCount) &&
            Objects.equals(maximumDispersalAmount, that.maximumDispersalAmount) &&
            Objects.equals(minimumDispersalAmount, that.minimumDispersalAmount);
  }

  @Override
  public int hashCode() {
    return Objects.hash(moratoriums, maximumDispersalCount, maximumDispersalAmount, minimumDispersalAmount);
  }

  @Override
  public String toString() {
    return "ProductParameters{" +
            "moratoriums=" + moratoriums +
            ", maximumDispersalCount=" + maximumDispersalCount +
            ", maximumDispersalAmount=" + maximumDispersalAmount +
            ", minimumDispersalAmount=" + minimumDispersalAmount +
            '}';
  }
}
