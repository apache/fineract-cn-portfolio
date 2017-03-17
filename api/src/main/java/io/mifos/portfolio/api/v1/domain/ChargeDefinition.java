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

import io.mifos.portfolio.api.v1.validation.ValidPaymentCycleUnit;
import io.mifos.core.lang.validation.constraints.ValidIdentifier;
import org.hibernate.validator.constraints.NotBlank;
import org.hibernate.validator.constraints.ScriptAssert;

import javax.annotation.Nullable;
import javax.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.temporal.ChronoUnit;
import java.util.Objects;

/**
 * @author Myrle Krantz
 */
@SuppressWarnings({"unused", "WeakerAccess"})
@ScriptAssert(lang = "javascript", script = "_this.amount !== null && _this.amount.scale() <= 4 && ((_this.accrueAction === null && _this.accrualAccountDesignator === null) || (_this.accrueAction !== null && _this.accrualAccountDesignator !== null))")
public class ChargeDefinition {
  @SuppressWarnings("WeakerAccess")
  public enum ChargeMethod {
    FIXED,
    PROPORTIONAL
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

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    ChargeDefinition that = (ChargeDefinition) o;
    return Objects.equals(identifier, that.identifier) &&
            Objects.equals(name, that.name) &&
            Objects.equals(description, that.description) &&
            Objects.equals(accrueAction, that.accrueAction) &&
            Objects.equals(chargeAction, that.chargeAction) &&
            Objects.equals(amount, that.amount) &&
            chargeMethod == that.chargeMethod &&
            Objects.equals(fromAccountDesignator, that.fromAccountDesignator) &&
            Objects.equals(accrualAccountDesignator, that.accrualAccountDesignator) &&
            Objects.equals(toAccountDesignator, that.toAccountDesignator) &&
            forCycleSizeUnit == that.forCycleSizeUnit;
  }

  @Override
  public int hashCode() {
    return Objects.hash(identifier, name, description, accrueAction, chargeAction, amount, chargeMethod, fromAccountDesignator, accrualAccountDesignator, toAccountDesignator, forCycleSizeUnit);
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
            ", fromAccountDesignator='" + fromAccountDesignator + '\'' +
            ", accrualAccountDesignator='" + accrualAccountDesignator + '\'' +
            ", toAccountDesignator='" + toAccountDesignator + '\'' +
            ", forCycleSizeUnit=" + forCycleSizeUnit +
            '}';
  }
}
