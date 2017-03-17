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

import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * @author Myrle Krantz
 */
@SuppressWarnings({"WeakerAccess", "unused"})
public final class PlannedPaymentPage {
  private Set<ChargeName> chargeNames;
  private List<PlannedPayment> elements;
  private Integer totalPages;
  private Long totalElements;

  public PlannedPaymentPage() {
  }

  public Set<ChargeName> getChargeNames() {
    return chargeNames;
  }

  public void setChargeNames(Set<ChargeName> chargeNames) {
    this.chargeNames = chargeNames;
  }

  public List<PlannedPayment> getElements() {
    return elements;
  }

  public void setElements(List<PlannedPayment> elements) {
    this.elements = elements;
  }

  public Integer getTotalPages() {
    return totalPages;
  }

  public void setTotalPages(Integer totalPages) {
    this.totalPages = totalPages;
  }

  public Long getTotalElements() {
    return totalElements;
  }

  public void setTotalElements(Long totalElements) {
    this.totalElements = totalElements;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    PlannedPaymentPage that = (PlannedPaymentPage) o;
    return Objects.equals(chargeNames, that.chargeNames) &&
            Objects.equals(elements, that.elements) &&
            Objects.equals(totalPages, that.totalPages) &&
            Objects.equals(totalElements, that.totalElements);
  }

  @Override
  public int hashCode() {
    return Objects.hash(chargeNames, elements, totalPages, totalElements);
  }

  @Override
  public String toString() {
    return "PlannedPaymentPage{" +
            "chargeNames=" + chargeNames +
            ", elements=" + elements +
            ", totalPages=" + totalPages +
            ", totalElements=" + totalElements +
            '}';
  }
}
