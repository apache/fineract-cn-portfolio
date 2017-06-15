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
package io.mifos.individuallending.internal.repository;

import javax.persistence.*;
import java.math.BigDecimal;
import java.util.Objects;

/**
 * @author Myrle Krantz
 */
@Entity
@Table(name = "bastet_il_c_credit_facts")
public class CaseCreditWorthinessFactorEntity {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "id")
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "case_id", nullable = false)
  private CaseParametersEntity caseId;

  @Column(name = "customer_identifier")
  private String customerIdentifier;

  @Column(name = "position_in_factor")
  private Integer positionInFactor;

  @Column(name = "position_in_customers")
  private Integer positionInCustomers;

  @Enumerated(EnumType.STRING)
  @Column(name = "fact_type")
  private CreditWorthinessFactorType factorType;

  @Column(name = "description")
  private String description;

  @Column(name = "amount")
  private BigDecimal amount;

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public CaseParametersEntity getCaseId() {
    return caseId;
  }

  public void setCaseId(CaseParametersEntity caseId) {
    this.caseId = caseId;
  }

  public String getCustomerIdentifier() {
    return customerIdentifier;
  }

  public void setCustomerIdentifier(String customerIdentifier) {
    this.customerIdentifier = customerIdentifier;
  }

  public Integer getPositionInFactor() {
    return positionInFactor;
  }

  public void setPositionInFactor(Integer positionInFactor) {
    this.positionInFactor = positionInFactor;
  }

  public Integer getPositionInCustomers() {
    return positionInCustomers;
  }

  public void setPositionInCustomers(Integer positionInCustomers) {
    this.positionInCustomers = positionInCustomers;
  }

  public CreditWorthinessFactorType getFactorType() {
    return factorType;
  }

  public void setFactorType(CreditWorthinessFactorType factorType) {
    this.factorType = factorType;
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
    CaseCreditWorthinessFactorEntity that = (CaseCreditWorthinessFactorEntity) o;
    return Objects.equals(caseId, that.caseId) &&
            Objects.equals(customerIdentifier, that.customerIdentifier) &&
            Objects.equals(positionInFactor, that.positionInFactor) &&
            factorType == that.factorType;
  }

  @Override
  public int hashCode() {
    return Objects.hash(caseId, customerIdentifier, positionInFactor, factorType);
  }
}
