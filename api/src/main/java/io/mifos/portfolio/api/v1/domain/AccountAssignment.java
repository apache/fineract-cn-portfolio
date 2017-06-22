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

import io.mifos.core.lang.validation.constraints.ValidIdentifier;
import org.hibernate.validator.constraints.ScriptAssert;

import java.util.Objects;

/**
 * @author Myrle Krantz
 */
@SuppressWarnings({"WeakerAccess", "unused"})
@ScriptAssert(lang = "javascript", script = "(_this.accountIdentifier == null && _this.ledgerIdentifier != null) || (_this.accountIdentifier != null && _this.ledgerIdentifier == null)")
public final class AccountAssignment {
  @ValidIdentifier
  private String designator;
  @ValidIdentifier(maxLength = 34, optional = true)
  private String accountIdentifier;
  @SuppressWarnings("DefaultAnnotationParam")
  @ValidIdentifier(maxLength = 32, optional = true)
  private String ledgerIdentifier;

  public AccountAssignment() {

  }

  public AccountAssignment(String designator, String accountIdentifier) {
    this.designator = designator;
    this.accountIdentifier = accountIdentifier;
  }

  public String getDesignator() {
    return designator;
  }

  public void setDesignator(String designator) {
    this.designator = designator;
  }

  public String getAccountIdentifier() {
    return accountIdentifier;
  }

  public void setAccountIdentifier(String accountIdentifier) {
    this.accountIdentifier = accountIdentifier;
  }

  public String getLedgerIdentifier() {
    return ledgerIdentifier;
  }

  public void setLedgerIdentifier(String ledgerIdentifier) {
    this.ledgerIdentifier = ledgerIdentifier;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    AccountAssignment that = (AccountAssignment) o;
    return Objects.equals(designator, that.designator) &&
            Objects.equals(accountIdentifier, that.accountIdentifier) &&
            Objects.equals(ledgerIdentifier, that.ledgerIdentifier);
  }

  @Override
  public int hashCode() {
    return Objects.hash(designator, accountIdentifier, ledgerIdentifier);
  }

  @Override
  public String toString() {
    return "AccountAssignment{" +
            "designator='" + designator + '\'' +
            ", accountIdentifier='" + accountIdentifier + '\'' +
            ", ledgerIdentifier='" + ledgerIdentifier + '\'' +
            '}';
  }
}