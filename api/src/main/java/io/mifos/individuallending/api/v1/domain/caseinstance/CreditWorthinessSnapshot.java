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

import io.mifos.core.lang.validation.constraints.ValidIdentifier;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import java.util.List;
import java.util.Objects;

/**
 * @author Myrle Krantz
 */
@SuppressWarnings({"WeakerAccess", "unused"})
public class CreditWorthinessSnapshot {
  @ValidIdentifier
  private String forCustomer;

  @NotNull
  @Valid
  private List<CreditWorthinessFactor> incomeSources;

  @NotNull
  @Valid
  private List<CreditWorthinessFactor> assets;

  @NotNull
  @Valid
  private List<CreditWorthinessFactor> debts;

  public CreditWorthinessSnapshot() {
  }

  public CreditWorthinessSnapshot(String forCustomer) {
    this.forCustomer = forCustomer;
  }

  public String getForCustomer() {
    return forCustomer;
  }

  public void setForCustomer(String forCustomer) {
    this.forCustomer = forCustomer;
  }

  public List<CreditWorthinessFactor> getIncomeSources() {
    return incomeSources;
  }

  public void setIncomeSources(List<CreditWorthinessFactor> incomeSources) {
    this.incomeSources = incomeSources;
  }

  public List<CreditWorthinessFactor> getAssets() {
    return assets;
  }

  public void setAssets(List<CreditWorthinessFactor> assets) {
    this.assets = assets;
  }

  public List<CreditWorthinessFactor> getDebts() {
    return debts;
  }

  public void setDebts(List<CreditWorthinessFactor> debts) {
    this.debts = debts;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    CreditWorthinessSnapshot that = (CreditWorthinessSnapshot) o;
    return Objects.equals(forCustomer, that.forCustomer) &&
            Objects.equals(incomeSources, that.incomeSources) &&
            Objects.equals(assets, that.assets) &&
            Objects.equals(debts, that.debts);
  }

  @Override
  public int hashCode() {
    return Objects.hash(forCustomer, incomeSources, assets, debts);
  }

  @Override
  public String toString() {
    return "CreditWorthinessSnapshot{" +
            "forCustomer='" + forCustomer + '\'' +
            ", incomeSources=" + incomeSources +
            ", assets=" + assets +
            ", debts=" + debts +
            '}';
  }
}
