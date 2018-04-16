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

import org.apache.fineract.cn.portfolio.api.v1.validation.ValidAccountAssignments;
import java.math.BigDecimal;
import java.util.Objects;
import java.util.Set;
import javax.validation.constraints.DecimalMax;
import javax.validation.constraints.DecimalMin;
import javax.validation.constraints.NotNull;
import org.apache.fineract.cn.lang.validation.constraints.ValidIdentifier;
import org.hibernate.validator.constraints.NotBlank;

/**
 * @author Myrle Krantz
 */
@SuppressWarnings({"WeakerAccess", "unused"})
public final class Case {
  @ValidIdentifier
  private String identifier;
  @ValidIdentifier
  private String productIdentifier;

  @DecimalMin(value = "0.00")
  @DecimalMax(value = "999.99")
  @NotNull
  private BigDecimal interest;

  @NotBlank
  private String parameters;
  @ValidAccountAssignments
  private Set<AccountAssignment> accountAssignments;

  private State currentState;
  private String createdOn;
  private String createdBy;
  private String lastModifiedOn;
  private String lastModifiedBy;

  public Case() {
  }

  public String getIdentifier() {
    return identifier;
  }

  public void setIdentifier(String identifier) {
    this.identifier = identifier;
  }

  public String getProductIdentifier() {
    return productIdentifier;
  }

  public void setProductIdentifier(String productIdentifier) {
    this.productIdentifier = productIdentifier;
  }

  public BigDecimal getInterest() {
    return interest;
  }

  public void setInterest(BigDecimal interest) {
    this.interest = interest;
  }

  public String getParameters() {
    return parameters;
  }

  public void setParameters(String parameters) {
    this.parameters = parameters;
  }

  public Set<AccountAssignment> getAccountAssignments() {
    return accountAssignments;
  }

  public void setAccountAssignments(Set<AccountAssignment> accountAssignments) {
    this.accountAssignments = accountAssignments;
  }

  public String getCurrentState() {
    return currentState == null ? null : currentState.name();
  }

  public void setCurrentState(String currentState) {
    this.currentState = State.valueOf(currentState);
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
    Case aCase = (Case) o;
    return Objects.equals(identifier, aCase.identifier) &&
        Objects.equals(productIdentifier, aCase.productIdentifier) &&
        Objects.equals(interest, aCase.interest) &&
        Objects.equals(parameters, aCase.parameters) &&
        Objects.equals(accountAssignments, aCase.accountAssignments) &&
        currentState == aCase.currentState;
  }

  @Override
  public int hashCode() {
    return Objects.hash(identifier, productIdentifier, interest, parameters, accountAssignments, currentState);
  }

  @Override
  public String toString() {
    return "Case{" +
        "identifier='" + identifier + '\'' +
        ", productIdentifier='" + productIdentifier + '\'' +
        ", interest=" + interest +
        ", parameters='" + parameters + '\'' +
        ", accountAssignments=" + accountAssignments +
        ", currentState=" + currentState +
        ", createdOn='" + createdOn + '\'' +
        ", createdBy='" + createdBy + '\'' +
        ", lastModifiedOn='" + lastModifiedOn + '\'' +
        ", lastModifiedBy='" + lastModifiedBy + '\'' +
        '}';
  }

  public enum State {
    CREATED,
    PENDING,
    APPROVED,
    ACTIVE,
    CLOSED
  }
}
