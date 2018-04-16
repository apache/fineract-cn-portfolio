/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.fineract.cn.portfolio.api.v1.domain;

import java.util.Objects;
import javax.annotation.Nullable;
import org.apache.fineract.cn.lang.validation.constraints.ValidIdentifier;
import org.hibernate.validator.constraints.Length;
import org.hibernate.validator.constraints.ScriptAssert;

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

  @Length(max = 256)
  @Nullable
  private String alternativeAccountNumber;

  public AccountAssignment() {
  }

  public AccountAssignment(final AccountAssignment toCopy) {
    this.designator = toCopy.designator;
    this.accountIdentifier = toCopy.accountIdentifier;
    this.ledgerIdentifier = toCopy.ledgerIdentifier;
    this.alternativeAccountNumber = toCopy.alternativeAccountNumber;
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

  @Nullable
  public String getAlternativeAccountNumber() {
    return alternativeAccountNumber;
  }

  public void setAlternativeAccountNumber(@Nullable String alternativeAccountNumber) {
    this.alternativeAccountNumber = alternativeAccountNumber;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    AccountAssignment that = (AccountAssignment) o;
    return Objects.equals(designator, that.designator) &&
        Objects.equals(accountIdentifier, that.accountIdentifier) &&
        Objects.equals(ledgerIdentifier, that.ledgerIdentifier) &&
        Objects.equals(alternativeAccountNumber, that.alternativeAccountNumber);
  }

  @Override
  public int hashCode() {
    return Objects.hash(designator, accountIdentifier, ledgerIdentifier, alternativeAccountNumber);
  }

  @Override
  public String toString() {
    return "AccountAssignment{" +
        "designator='" + designator + '\'' +
        ", accountIdentifier='" + accountIdentifier + '\'' +
        ", ledgerIdentifier='" + ledgerIdentifier + '\'' +
        ", alternativeAccountNumber='" + alternativeAccountNumber + '\'' +
        '}';
  }
}