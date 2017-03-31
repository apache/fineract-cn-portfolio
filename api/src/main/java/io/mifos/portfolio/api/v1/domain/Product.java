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

import io.mifos.portfolio.api.v1.validation.ValidAccountAssignments;
import io.mifos.portfolio.api.v1.validation.ValidCurrencyCode;
import io.mifos.core.lang.validation.constraints.ValidIdentifier;
import org.hibernate.validator.constraints.Length;
import org.hibernate.validator.constraints.Range;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import java.util.Objects;
import java.util.Set;

/**
 * @author Myrle Krantz
 */
@SuppressWarnings({"WeakerAccess", "unused"})
public final class Product {
  @ValidIdentifier
  private String identifier;
  @NotNull
  @Length(min = 2, max = 256)
  private String name;
  @NotNull
  @Valid
  private TermRange termRange;
  @NotNull
  @Valid
  private BalanceRange balanceRange;
  @NotNull
  @Valid
  private InterestRange interestRange;
  @NotNull
  private InterestBasis interestBasis;
  @NotNull
  @ValidIdentifier(maxLength = 512)
  private String patternPackage;
  @Length(max = 4096)
  private String description;
  @ValidCurrencyCode
  private String currencyCode;
  @Range(min = 0, max = 4)
  private int minorCurrencyUnitDigits;

  @ValidAccountAssignments
  private Set<AccountAssignment> accountAssignments;

  @NotNull
  @Length(max = 8092)
  private String parameters; //json serialization of product specific parameters.

  private String createdOn;
  private String createdBy;
  private String lastModifiedOn;
  private String lastModifiedBy;

  public Product() {
  }

  public String getIdentifier() {
    return identifier;
  }

  public void setIdentifier(String identifier) {
    this.identifier = identifier;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public TermRange getTermRange() {
    return termRange;
  }

  public void setTermRange(TermRange termRange) {
    this.termRange = termRange;
  }

  public BalanceRange getBalanceRange() {
    return balanceRange;
  }

  public void setBalanceRange(BalanceRange balanceRange) {
    this.balanceRange = balanceRange;
  }

  public InterestRange getInterestRange() {
    return interestRange;
  }

  public void setInterestRange(InterestRange interestRange) {
    this.interestRange = interestRange;
  }

  public InterestBasis getInterestBasis() {
    return interestBasis;
  }

  public void setInterestBasis(InterestBasis interestBasis) {
    this.interestBasis = interestBasis;
  }

  public String getPatternPackage() {
    return patternPackage;
  }

  public void setPatternPackage(String patternPackage) {
    this.patternPackage = patternPackage;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public String getCurrencyCode() {
    return currencyCode;
  }

  public void setCurrencyCode(String currencyCode) {
    this.currencyCode = currencyCode;
  }

  public int getMinorCurrencyUnitDigits() {
    return minorCurrencyUnitDigits;
  }

  public void setMinorCurrencyUnitDigits(int minorCurrencyUnitDigits) {
    this.minorCurrencyUnitDigits = minorCurrencyUnitDigits;
  }

  public Set<AccountAssignment> getAccountAssignments() {
    return accountAssignments;
  }

  public void setAccountAssignments(Set<AccountAssignment> accountAssignments) {
    this.accountAssignments = accountAssignments;
  }

  public String getParameters() {
    return parameters;
  }

  public void setParameters(String parameters) {
    this.parameters = parameters;
  }

  public String getCreatedOn() {
    return createdOn;
  }

  public void setCreatedOn(String createdOn) {
    this.createdOn = createdOn;
  }

  public String getCreatedBy() {
    return createdBy;
  }

  public void setCreatedBy(String createdBy) {
    this.createdBy = createdBy;
  }

  public String getLastModifiedOn() {
    return lastModifiedOn;
  }

  public void setLastModifiedOn(String lastModifiedOn) {
    this.lastModifiedOn = lastModifiedOn;
  }

  public String getLastModifiedBy() {
    return lastModifiedBy;
  }

  public void setLastModifiedBy(String lastModifiedBy) {
    this.lastModifiedBy = lastModifiedBy;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    Product product = (Product) o;
    return minorCurrencyUnitDigits == product.minorCurrencyUnitDigits &&
            Objects.equals(identifier, product.identifier) &&
            Objects.equals(name, product.name) &&
            Objects.equals(termRange, product.termRange) &&
            Objects.equals(balanceRange, product.balanceRange) &&
            Objects.equals(interestRange, product.interestRange) &&
            interestBasis == product.interestBasis &&
            Objects.equals(patternPackage, product.patternPackage) &&
            Objects.equals(description, product.description) &&
            Objects.equals(currencyCode, product.currencyCode) &&
            Objects.equals(accountAssignments, product.accountAssignments) &&
            Objects.equals(parameters, product.parameters);
  }

  @Override
  public int hashCode() {
    return Objects.hash(identifier, name, termRange, balanceRange, interestRange, interestBasis, patternPackage, description, currencyCode, minorCurrencyUnitDigits, accountAssignments, parameters);
  }

  @Override
  public String toString() {
    return "Product{" +
            "identifier='" + identifier + '\'' +
            ", name='" + name + '\'' +
            ", termRange=" + termRange +
            ", balanceRange=" + balanceRange +
            ", interestRange=" + interestRange +
            ", interestBasis=" + interestBasis +
            ", patternPackage='" + patternPackage + '\'' +
            ", description='" + description + '\'' +
            ", currencyCode='" + currencyCode + '\'' +
            ", minorCurrencyUnitDigits=" + minorCurrencyUnitDigits +
            ", accountAssignments=" + accountAssignments +
            ", parameters='" + parameters + '\'' +
            ", createdOn='" + createdOn + '\'' +
            ", createdBy='" + createdBy + '\'' +
            ", lastModifiedOn='" + lastModifiedOn + '\'' +
            ", lastModifiedBy='" + lastModifiedBy + '\'' +
            '}';
  }
}
