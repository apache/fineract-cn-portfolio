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
package io.mifos.portfolio.api.v1.events;

import java.util.Objects;

/**
 * @author Myrle Krantz
 */
@SuppressWarnings({"WeakerAccess", "unused"})
public class ChargeDefinitionEvent {
  private String productIdentifier;
  private String chargeDefinitionIdentifier;

  public ChargeDefinitionEvent(String productIdentifier, String chargeDefinitionIdentifier) {
    this.productIdentifier = productIdentifier;
    this.chargeDefinitionIdentifier = chargeDefinitionIdentifier;
  }

  public String getProductIdentifier() {
    return productIdentifier;
  }

  public void setProductIdentifier(String productIdentifier) {
    this.productIdentifier = productIdentifier;
  }

  public String getChargeDefinitionIdentifier() {
    return chargeDefinitionIdentifier;
  }

  public void setChargeDefinitionIdentifier(String chargeDefinitionIdentifier) {
    this.chargeDefinitionIdentifier = chargeDefinitionIdentifier;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    ChargeDefinitionEvent that = (ChargeDefinitionEvent) o;
    return Objects.equals(productIdentifier, that.productIdentifier) &&
            Objects.equals(chargeDefinitionIdentifier, that.chargeDefinitionIdentifier);
  }

  @Override
  public int hashCode() {
    return Objects.hash(productIdentifier, chargeDefinitionIdentifier);
  }

  @Override
  public String toString() {
    return "ChargeDefinitionEvent{" +
            "productIdentifier='" + productIdentifier + '\'' +
            ", chargeDefinitionIdentifier='" + chargeDefinitionIdentifier + '\'' +
            '}';
  }
}
