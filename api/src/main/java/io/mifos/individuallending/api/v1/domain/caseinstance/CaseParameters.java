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
import io.mifos.portfolio.api.v1.domain.PaymentCycle;
import io.mifos.portfolio.api.v1.domain.TermRange;
import org.hibernate.validator.constraints.Range;
import org.hibernate.validator.constraints.ScriptAssert;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;

/**
 * @author Myrle Krantz
 */
@SuppressWarnings({"WeakerAccess", "unused"})
@ScriptAssert(lang = "javascript", script = "_this.maximumBalance !== null && _this.maximumBalance.scale() <= 4")
public final class CaseParameters {
  @ValidIdentifier
  private String customerIdentifier;

  @NotNull
  @Valid
  private List<CreditWorthinessSnapshot> creditWorthinessSnapshots;

  @Range(min = 0)
  private BigDecimal maximumBalance;

  @NotNull
  @Valid
  private TermRange termRange;
  @NotNull
  @Valid
  private PaymentCycle paymentCycle;

  public CaseParameters() {
  }

  public CaseParameters(final String customerIdentifier) {
    this.customerIdentifier = customerIdentifier;
  }

  public String getCustomerIdentifier() {
    return customerIdentifier;
  }

  public void setCustomerIdentifier(String customerIdentifier) {
    this.customerIdentifier = customerIdentifier;
  }

  public List<CreditWorthinessSnapshot> getCreditWorthinessSnapshots() {
    return creditWorthinessSnapshots;
  }

  public void setCreditWorthinessSnapshots(List<CreditWorthinessSnapshot> creditWorthinessSnapshots) {
    this.creditWorthinessSnapshots = creditWorthinessSnapshots;
  }

  public BigDecimal getMaximumBalance() {
    return maximumBalance;
  }

  public void setMaximumBalance(BigDecimal maximumBalance) {
    this.maximumBalance = maximumBalance;
  }

  public TermRange getTermRange() {
    return termRange;
  }

  public void setTermRange(TermRange termRange) {
    this.termRange = termRange;
  }

  public PaymentCycle getPaymentCycle() {
    return paymentCycle;
  }

  public void setPaymentCycle(PaymentCycle paymentCycle) {
    this.paymentCycle = paymentCycle;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    CaseParameters that = (CaseParameters) o;
    return Objects.equals(customerIdentifier, that.customerIdentifier) &&
            Objects.equals(creditWorthinessSnapshots, that.creditWorthinessSnapshots) &&
            Objects.equals(maximumBalance, that.maximumBalance) &&
            Objects.equals(termRange, that.termRange) &&
            Objects.equals(paymentCycle, that.paymentCycle);
  }

  @Override
  public int hashCode() {
    return Objects.hash(customerIdentifier, creditWorthinessSnapshots, maximumBalance, termRange, paymentCycle);
  }

  @Override
  public String toString() {
    return "CaseParameters{" +
            "customerIdentifier='" + customerIdentifier + '\'' +
            ", creditWorthinessSnapshots=" + creditWorthinessSnapshots +
            ", maximumBalance=" + maximumBalance +
            ", termRange=" + termRange +
            ", paymentCycle=" + paymentCycle +
            '}';
  }
}