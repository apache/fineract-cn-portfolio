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
package io.mifos.portfolio.service.internal.repository;

import io.mifos.portfolio.api.v1.domain.ChargeDefinition;

import javax.persistence.*;
import java.math.BigDecimal;
import java.time.temporal.ChronoUnit;

/**
 * @author Myrle Krantz
 */
@SuppressWarnings({"WeakerAccess", "unused"})
@Entity
@Table(name = "bastet_p_chrg_defs")
public class ChargeDefinitionEntity {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "id")
  private Long id;

  @Column(name = "identifier")
  private String identifier;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "product_id", nullable = false)
  private ProductEntity product;

  @Column(name = "a_name")
  private String name;

  @Column(name = "description")
  private String description;

  @Column(name = "accrue_action")
  private String accrueAction;

  @Column(name = "charge_action")
  private String chargeAction;

  @Column(name = "amount")
  private BigDecimal amount;

  @Enumerated(EnumType.STRING)
  @Column(name = "charge_method")
  private ChargeDefinition.ChargeMethod chargeMethod;

  @Column(name = "from_account_designator")
  private String fromAccountDesignator;

  @Column(name = "accru_account_designator")
  private String accrualAccountDesignator;

  @Column(name = "to_account_designator")
  private String toAccountDesignator;

  @Enumerated(EnumType.STRING)
  @Column(name = "for_cycle_size_unit")
  private ChronoUnit forCycleSizeUnit;

  public ChargeDefinitionEntity() {
  }

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public String getIdentifier() {
    return identifier;
  }

  public void setIdentifier(String identifier) {
    this.identifier = identifier;
  }

  public ProductEntity getProduct() {
    return product;
  }

  public void setProduct(ProductEntity product) {
    this.product = product;
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

  public ChargeDefinition.ChargeMethod getChargeMethod() {
    return chargeMethod;
  }

  public void setChargeMethod(ChargeDefinition.ChargeMethod chargeMethod) {
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

  public ChronoUnit getForCycleSizeUnit() {
    return forCycleSizeUnit;
  }

  public void setForCycleSizeUnit(ChronoUnit forCycleSizeUnit) {
    this.forCycleSizeUnit = forCycleSizeUnit;
  }
}
