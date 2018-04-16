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

import org.apache.fineract.cn.portfolio.api.v1.validation.ValidChargeDefinition;
import org.apache.fineract.cn.portfolio.api.v1.validation.ValidChargeReference;
import org.apache.fineract.cn.portfolio.api.v1.validation.ValidPaymentCycleUnit;
import java.math.BigDecimal;
import java.time.temporal.ChronoUnit;
import java.util.Objects;
import javax.annotation.Nullable;
import javax.validation.constraints.NotNull;
import org.apache.fineract.cn.lang.validation.constraints.ValidIdentifier;
import org.hibernate.validator.constraints.NotBlank;

/**
 * @author Myrle Krantz
 */
@SuppressWarnings({"unused", "WeakerAccess"})
@ValidChargeDefinition
public class ChargeDefinition {
  @SuppressWarnings("WeakerAccess")
  public enum ChargeMethod {
    FIXED,
    PROPORTIONAL,
    INTEREST
  }

  @ValidIdentifier
  private String identifier;

  @NotBlank
  private String name;

  @NotBlank
  private String description;

  private String accrueAction;

  @NotBlank
  private String chargeAction;

  @NotNull
  private BigDecimal amount;

  @NotNull
  private ChargeMethod chargeMethod;

  @ValidChargeReference
  private String proportionalTo;

  @ValidIdentifier
  private String fromAccountDesignator; //Where it's going.

  @ValidIdentifier(optional = true)
  private String accrualAccountDesignator;

  @ValidIdentifier
  private String toAccountDesignator; //Where it's coming from.

  //If this is set, and the charge is applied more frequently than the unit, then the charge amount is divided by the
  //number of days in the unit, and then compounded by the number of days since the last time this charge was applied.
  @ValidPaymentCycleUnit
  private ChronoUnit forCycleSizeUnit;

  private boolean readOnly;

  @ValidIdentifier(optional = true)
  private String forSegmentSet;

  @ValidIdentifier(optional = true)
  private String fromSegment;

  @ValidIdentifier(optional = true)
  private String toSegment;

  private Boolean chargeOnTop;

  public ChargeDefinition() {
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

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public String getAccrueAction() {
    return accrueAction;
  }

  public void setAccrueAction(String accrueAction) {
    this.accrueAction = accrueAction;
  }

  public String getChargeAction() {
    return chargeAction;
  }

  public void setChargeAction(String chargeAction) {
    this.chargeAction = chargeAction;
  }

  public BigDecimal getAmount() {
    return amount;
  }

  public void setAmount(BigDecimal amount) {
    this.amount = amount;
  }

  public ChargeMethod getChargeMethod() {
    return chargeMethod;
  }

  public void setChargeMethod(ChargeMethod chargeMethod) {
    this.chargeMethod = chargeMethod;
  }

  public String getProportionalTo() {
    return proportionalTo;
  }

  public void setProportionalTo(String proportionalTo) {
    this.proportionalTo = proportionalTo;
  }

  public String getFromAccountDesignator() {
    return fromAccountDesignator;
  }

  public void setFromAccountDesignator(String fromAccountDesignator) {
    this.fromAccountDesignator = fromAccountDesignator;
  }

  public String getAccrualAccountDesignator() {
    return accrualAccountDesignator;
  }

  public void setAccrualAccountDesignator(String accrualAccountDesignator) {
    this.accrualAccountDesignator = accrualAccountDesignator;
  }

  public String getToAccountDesignator() {
    return toAccountDesignator;
  }

  public void setToAccountDesignator(String toAccountDesignator) {
    this.toAccountDesignator = toAccountDesignator;
  }

  @Nullable
  public ChronoUnit getForCycleSizeUnit() {
    return forCycleSizeUnit;
  }

  public void setForCycleSizeUnit(@Nullable ChronoUnit forCycleSizeUnit) {
    this.forCycleSizeUnit = forCycleSizeUnit;
  }

  public boolean isReadOnly() {
    return readOnly;
  }

  public void setReadOnly(boolean readOnly) {
    this.readOnly = readOnly;
  }

  public String getForSegmentSet() {
    return forSegmentSet;
  }

  public void setForSegmentSet(String forSegmentSet) {
    this.forSegmentSet = forSegmentSet;
  }

  public String getFromSegment() {
    return fromSegment;
  }

  public void setFromSegment(String fromSegment) {
    this.fromSegment = fromSegment;
  }

  public String getToSegment() {
    return toSegment;
  }

  public void setToSegment(String toSegment) {
    this.toSegment = toSegment;
  }

  public Boolean getChargeOnTop() {
    return chargeOnTop;
  }

  public void setChargeOnTop(Boolean chargeOnTop) {
    this.chargeOnTop = chargeOnTop;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    ChargeDefinition that = (ChargeDefinition) o;
    return readOnly == that.readOnly &&
        Objects.equals(identifier, that.identifier) &&
        Objects.equals(name, that.name) &&
        Objects.equals(description, that.description) &&
        Objects.equals(accrueAction, that.accrueAction) &&
        Objects.equals(chargeAction, that.chargeAction) &&
        Objects.equals(amount, that.amount) &&
        chargeMethod == that.chargeMethod &&
        Objects.equals(proportionalTo, that.proportionalTo) &&
        Objects.equals(fromAccountDesignator, that.fromAccountDesignator) &&
        Objects.equals(accrualAccountDesignator, that.accrualAccountDesignator) &&
        Objects.equals(toAccountDesignator, that.toAccountDesignator) &&
        forCycleSizeUnit == that.forCycleSizeUnit &&
        Objects.equals(forSegmentSet, that.forSegmentSet) &&
        Objects.equals(fromSegment, that.fromSegment) &&
        Objects.equals(toSegment, that.toSegment) &&
        Objects.equals(chargeOnTop, that.chargeOnTop);
  }

  @Override
  public int hashCode() {
    return Objects.hash(identifier, name, description, accrueAction, chargeAction, amount, chargeMethod, proportionalTo, fromAccountDesignator, accrualAccountDesignator, toAccountDesignator, forCycleSizeUnit, readOnly, forSegmentSet, fromSegment, toSegment, chargeOnTop);
  }

  @Override
  public String toString() {
    return "ChargeDefinition{" +
        "identifier='" + identifier + '\'' +
        ", name='" + name + '\'' +
        ", description='" + description + '\'' +
        ", accrueAction='" + accrueAction + '\'' +
        ", chargeAction='" + chargeAction + '\'' +
        ", amount=" + amount +
        ", chargeMethod=" + chargeMethod +
        ", proportionalTo='" + proportionalTo + '\'' +
        ", fromAccountDesignator='" + fromAccountDesignator + '\'' +
        ", accrualAccountDesignator='" + accrualAccountDesignator + '\'' +
        ", toAccountDesignator='" + toAccountDesignator + '\'' +
        ", forCycleSizeUnit=" + forCycleSizeUnit +
        ", readOnly=" + readOnly +
        ", forSegmentSet='" + forSegmentSet + '\'' +
        ", fromSegment='" + fromSegment + '\'' +
        ", toSegment='" + toSegment + '\'' +
        ", chargeOnTop='" + chargeOnTop + '\'' +
        '}';
  }
}
