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

import org.hibernate.validator.constraints.Length;
import org.hibernate.validator.constraints.Range;

import java.math.BigDecimal;
import java.util.Objects;

/**
 * @author Myrle Krantz
 */
@SuppressWarnings({"unused", "WeakerAccess"})
public class CreditWorthinessFactor {
  @Length(max = 4096)
  private String description;

  @Range(min = 0)
  private BigDecimal amount;

  public CreditWorthinessFactor() {
  }

  public CreditWorthinessFactor(String description, BigDecimal amount) {
    this.description = description;
    this.amount = amount;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
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
    CreditWorthinessFactor that = (CreditWorthinessFactor) o;
    return Objects.equals(description, that.description) &&
            Objects.equals(amount, that.amount);
  }

  @Override
  public int hashCode() {
    return Objects.hash(description, amount);
  }

  @Override
  public String toString() {
    return "CreditWorthinessFactor{" +
            "description='" + description + '\'' +
            ", amount=" + amount +
            '}';
  }
}