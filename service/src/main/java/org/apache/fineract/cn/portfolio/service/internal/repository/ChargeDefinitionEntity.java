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
package org.apache.fineract.cn.portfolio.service.internal.repository;

import org.apache.fineract.cn.portfolio.api.v1.domain.ChargeDefinition;

import javax.persistence.*;
import java.math.BigDecimal;
import java.time.temporal.ChronoUnit;
import java.util.Objects;

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

  @Column(name = "proportional_to")
  private String proportionalTo;

  @Column(name = "from_account_designator")
  private String fromAccountDesignator;

  @Column(name = "accru_account_designator")
  private String accrualAccountDesignator;

  @Column(name = "to_account_designator")
  private String toAccountDesignator;

  @Enumerated(EnumType.STRING)
  @Column(name = "for_cycle_size_unit")
  private ChronoUnit forCycleSizeUnit;

  @Column(name = "read_only")
  private Boolean readOnly;

  @Column(name = "segment_set")
  private String segmentSet;

  @Column(name = "from_segment")
  private String fromSegment;

  @Column(name = "to_segment")
  private String toSegment;

  @Column(name = "on_top")
  private Boolean onTop;

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

  public ChronoUnit getForCycleSizeUnit() {
    return forCycleSizeUnit;
  }

  public void setForCycleSizeUnit(ChronoUnit forCycleSizeUnit) {
    this.forCycleSizeUnit = forCycleSizeUnit;
  }

  public Boolean getReadOnly() {
    return readOnly;
  }

  public void setReadOnly(Boolean readOnly) {
    this.readOnly = readOnly;
  }

  public String getSegmentSet() {
    return segmentSet;
  }

  public void setSegmentSet(String segmentSet) {
    this.segmentSet = segmentSet;
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

  public Boolean getOnTop() {
    return onTop;
  }

  public void setOnTop(Boolean onTop) {
    this.onTop = onTop;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    ChargeDefinitionEntity that = (ChargeDefinitionEntity) o;
    return Objects.equals(identifier, that.identifier) &&
            Objects.equals(product, that.product);
  }

  @Override
  public int hashCode() {
    return Objects.hash(identifier, product);
  }
}
