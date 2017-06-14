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

import io.mifos.core.mariadb.util.LocalDateTimeConverter;
import io.mifos.portfolio.api.v1.domain.InterestBasis;

import javax.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Objects;
import java.util.Set;

/**
 * @author Myrle Krantz
 */
@Entity
@Table(name = "bastet_products")
public class ProductEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "id")
  private Long id;

  @Column(name = "identifier")
  private String identifier;

  @Column(name = "a_name")
  private String name;

  @Enumerated(EnumType.STRING)
  @Column(name = "term_range_temporal_unit")
  private ChronoUnit termRangeTemporalUnit;

  @Column(name = "term_range_minimum")
  private Integer termRangeMinimum;

  @Column(name = "term_range_maximum")
  private Integer termRangeMaximum;

  @Column(name = "balance_range_minimum")
  private BigDecimal balanceRangeMinimum;

  @Column(name = "balance_range_maximum")
  private BigDecimal balanceRangeMaximum;

  @Column(name = "interest_range_minimum")
  private BigDecimal interestRangeMinimum;

  @Column(name = "interest_range_maximum")
  private BigDecimal interestRangeMaximum;

  @Enumerated(EnumType.STRING)
  @Column(name = "interest_basis")
  private InterestBasis interestBasis;

  @Column(name = "pattern_package")
  private String patternPackage;

  @Column(name = "description")
  private String description;

  @Column(name = "enabled")
  private Boolean enabled;

  @Column(name = "currency_code")
  private String currencyCode;

  @Column(name = "minor_currency_unit_digits")
  private Integer minorCurrencyUnitDigits;

  @OneToMany(targetEntity = ProductAccountAssignmentEntity.class, cascade = CascadeType.ALL, orphanRemoval = true, mappedBy = "product")
  private Set<ProductAccountAssignmentEntity> accountAssignments;

  @OneToMany(targetEntity = ChargeDefinitionEntity.class, cascade = CascadeType.ALL, orphanRemoval = true, mappedBy = "product")
  private Set<ChargeDefinitionEntity> chargeDefinitions;

  @OneToMany(targetEntity = TaskDefinitionEntity.class, cascade = CascadeType.ALL, orphanRemoval = true, mappedBy = "product")
  private Set<TaskDefinitionEntity> taskDefinitions;

  @Column(name = "parameters")
  private String parameters;

  @Column(name = "created_on")
  @Convert(converter = LocalDateTimeConverter.class)
  private LocalDateTime createdOn;

  @Column(name = "created_by")
  private String createdBy;

  @Column(name = "last_modified_on")
  @Convert(converter = LocalDateTimeConverter.class)
  private LocalDateTime lastModifiedOn;

  @Column(name = "last_modified_by")
  private String lastModifiedBy;

  public ProductEntity() {
    super();
  }

  public Long getId() {
    return id;
  }

  @SuppressWarnings("unused")
  public void setId(Long id) {
    this.id = id;
  }

  public String getIdentifier() {
    return this.identifier;
  }

  public void setIdentifier(final String identifier) {
    this.identifier = identifier;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public ChronoUnit getTermRangeTemporalUnit() {
    return termRangeTemporalUnit;
  }

  public void setTermRangeTemporalUnit(ChronoUnit termRangeTemporalUnit) {
    this.termRangeTemporalUnit = termRangeTemporalUnit;
  }

  @SuppressWarnings("unused")
  public Integer getTermRangeMinimum() {
    return termRangeMinimum;
  }

  public void setTermRangeMinimum(Integer termRangeMinimum) {
    this.termRangeMinimum = termRangeMinimum;
  }

  public Integer getTermRangeMaximum() {
    return termRangeMaximum;
  }

  public void setTermRangeMaximum(Integer termRangeMaximum) {
    this.termRangeMaximum = termRangeMaximum;
  }

  public BigDecimal getBalanceRangeMinimum() {
    return balanceRangeMinimum;
  }

  public void setBalanceRangeMinimum(BigDecimal balanceRangeMinimum) {
    this.balanceRangeMinimum = balanceRangeMinimum;
  }

  public BigDecimal getBalanceRangeMaximum() {
    return balanceRangeMaximum;
  }

  public void setBalanceRangeMaximum(BigDecimal balanceRangeMaximum) {
    this.balanceRangeMaximum = balanceRangeMaximum;
  }

  public BigDecimal getInterestRangeMinimum() {
    return interestRangeMinimum;
  }

  public void setInterestRangeMinimum(BigDecimal interestRangeMinimum) {
    this.interestRangeMinimum = interestRangeMinimum;
  }

  public BigDecimal getInterestRangeMaximum() {
    return interestRangeMaximum;
  }

  public void setInterestRangeMaximum(BigDecimal interestRangeMaximum) {
    this.interestRangeMaximum = interestRangeMaximum;
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

  public Boolean getEnabled() {
    return enabled;
  }

  public void setEnabled(Boolean enabled) {
    this.enabled = enabled;
  }

  public String getCurrencyCode() {
    return currencyCode;
  }

  public void setCurrencyCode(String currencyCode) {
    this.currencyCode = currencyCode;
  }

  public Integer getMinorCurrencyUnitDigits() {
    return minorCurrencyUnitDigits;
  }

  public void setMinorCurrencyUnitDigits(Integer minorCurrencyUnitDigits) {
    this.minorCurrencyUnitDigits = minorCurrencyUnitDigits;
  }

  public Set<ProductAccountAssignmentEntity> getAccountAssignments() {
    return accountAssignments;
  }

  public void setAccountAssignments(Set<ProductAccountAssignmentEntity> accountAssignments) {
    this.accountAssignments = accountAssignments;
  }

  public Set<ChargeDefinitionEntity> getChargeDefinitions() {
    return chargeDefinitions;
  }

  public void setChargeDefinitions(Set<ChargeDefinitionEntity> chargeDefinitions) {
    this.chargeDefinitions = chargeDefinitions;
  }

  public Set<TaskDefinitionEntity> getTaskDefinitions() {
    return taskDefinitions;
  }

  public void setTaskDefinitions(Set<TaskDefinitionEntity> taskDefinitions) {
    this.taskDefinitions = taskDefinitions;
  }

  public String getParameters() {
    return parameters;
  }

  public void setParameters(String parameters) {
    this.parameters = parameters;
  }

  public LocalDateTime getCreatedOn() {
    return createdOn;
  }

  public void setCreatedOn(LocalDateTime createdOn) {
    this.createdOn = createdOn;
  }

  public String getCreatedBy() {
    return createdBy;
  }

  public void setCreatedBy(String createdBy) {
    this.createdBy = createdBy;
  }

  public LocalDateTime getLastModifiedOn() {
    return lastModifiedOn;
  }

  public void setLastModifiedOn(LocalDateTime lastModifiedOn) {
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
    if (o == null || !(o instanceof ProductEntity)) return false;
    ProductEntity that = (ProductEntity) o;
    return Objects.equals(getIdentifier(), that.getIdentifier());
  }

  @Override
  public int hashCode() {
    return Objects.hash(getIdentifier());
  }
}
