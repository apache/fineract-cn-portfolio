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
package io.mifos.individuallending.api.v1.domain.caseinstance;

import io.mifos.portfolio.api.v1.domain.CostComponent;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;

/**
 * @author Myrle Krantz
 */
@SuppressWarnings({"WeakerAccess", "unused"})
public final class PlannedPayment {
  private Double interestRate;
  private List<CostComponent> costComponents;
  private BigDecimal remainingPrincipal;
  private String date;

  public PlannedPayment() {
  }

  public PlannedPayment(Double interestRate, List<CostComponent> costComponents, BigDecimal remainingPrincipal) {
    this.interestRate = interestRate;
    this.costComponents = costComponents;
    this.remainingPrincipal = remainingPrincipal;
  }

  public Double getInterestRate() {
    return interestRate;
  }

  public void setInterestRate(Double interestRate) {
    this.interestRate = interestRate;
  }

  public List<CostComponent> getCostComponents() {
    return costComponents;
  }

  public void setCostComponents(List<CostComponent> costComponents) {
    this.costComponents = costComponents;
  }

  public BigDecimal getRemainingPrincipal() {
    return remainingPrincipal;
  }

  public void setRemainingPrincipal(BigDecimal remainingPrincipal) {
    this.remainingPrincipal = remainingPrincipal;
  }

  public String getDate() {
    return date;
  }

  public void setDate(String date) {
    this.date = date;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    PlannedPayment that = (PlannedPayment) o;
    return Objects.equals(interestRate, that.interestRate) &&
            Objects.equals(costComponents, that.costComponents) &&
            Objects.equals(remainingPrincipal, that.remainingPrincipal) &&
            Objects.equals(date, that.date);
  }

  @Override
  public int hashCode() {
    return Objects.hash(interestRate, costComponents, remainingPrincipal, date);
  }

  @Override
  public String toString() {
    return "PlannedPayment{" +
            "interestRate=" + interestRate +
            ", costComponents=" + costComponents +
            ", remainingPrincipal=" + remainingPrincipal +
            ", date='" + date + '\'' +
            '}';
  }
}
