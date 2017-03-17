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
@SuppressWarnings({"unused", "WeakerAccess"})
public class CaseEvent {
  private String productIdentifier;
  private String caseIdentifier;

  public CaseEvent() {
  }

  public CaseEvent(String productIdentifier, String caseIdentifier) {
    this.productIdentifier = productIdentifier;
    this.caseIdentifier = caseIdentifier;
  }

  public String getProductIdentifier() {
    return productIdentifier;
  }

  public void setProductIdentifier(String productIdentifier) {
    this.productIdentifier = productIdentifier;
  }

  public String getCaseIdentifier() {
    return caseIdentifier;
  }

  public void setCaseIdentifier(String caseIdentifier) {
    this.caseIdentifier = caseIdentifier;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    CaseEvent caseEvent = (CaseEvent) o;
    return Objects.equals(productIdentifier, caseEvent.productIdentifier) &&
            Objects.equals(caseIdentifier, caseEvent.caseIdentifier);
  }

  @Override
  public int hashCode() {
    return Objects.hash(productIdentifier, caseIdentifier);
  }

  @Override
  public String toString() {
    return "CaseEvent{" +
            "productIdentifier='" + productIdentifier + '\'' +
            ", caseIdentifier='" + caseIdentifier + '\'' +
            '}';
  }
}
