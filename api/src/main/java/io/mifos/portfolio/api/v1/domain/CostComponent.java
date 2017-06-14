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

import java.math.BigDecimal;
import java.util.Objects;

/**
 * @author Myrle Krantz
 */
@SuppressWarnings({"WeakerAccess", "unused"})
public class CostComponent {
  private String chargeIdentifier;
  private BigDecimal amount;

  public CostComponent() {
  }

  public CostComponent(String chargeIdentifier, BigDecimal amount) {
    this.chargeIdentifier = chargeIdentifier;
    this.amount = amount;
  }

  public String getChargeIdentifier() {
    return chargeIdentifier;
  }

  public void setChargeIdentifier(String chargeIdentifier) {
    this.chargeIdentifier = chargeIdentifier;
  }

  public BigDecimal getAmount() {
    return amount;
  }

  public void setAmount(BigDecimal amount) {
    this.amount = amount;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    CostComponent that = (CostComponent) o;
    return Objects.equals(chargeIdentifier, that.chargeIdentifier) &&
            Objects.equals(amount, that.amount);
  }

  @Override
  public int hashCode() {
    return Objects.hash(chargeIdentifier, amount);
  }

  @Override
  public String toString() {
    return "CostComponent{" +
            "chargeIdentifier='" + chargeIdentifier + '\'' +
            ", amount=" + amount +
            '}';
  }
}