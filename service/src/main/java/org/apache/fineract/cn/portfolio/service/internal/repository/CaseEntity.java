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

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.Set;
import javax.annotation.Nullable;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Convert;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import org.apache.fineract.cn.mariadb.util.LocalDateTimeConverter;

/**
 * @author Myrle Krantz
 */
@SuppressWarnings("unused")
@Entity
@Table(name = "bastet_cases")
public class CaseEntity {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "id")
  private Long id;

  @Column(name = "identifier", nullable = false)
  private String identifier;

  @Column(name = "product_identifier", nullable = false)
  private String productIdentifier;

  @Column(name = "interest")
  private BigDecimal interest;

  @OneToMany(targetEntity = CaseAccountAssignmentEntity.class, cascade = CascadeType.ALL, orphanRemoval = true, mappedBy = "caseEntity")
  private Set<CaseAccountAssignmentEntity> accountAssignments;

  @OneToMany(targetEntity = TaskInstanceEntity.class, cascade = CascadeType.ALL, orphanRemoval = true, mappedBy = "customerCase")
  private Set<TaskInstanceEntity> taskInstances;

  @Column(name = "current_state", nullable = false)
  private String currentState;

  @Column(name = "start_of_term")
  @Convert(converter = LocalDateTimeConverter.class)
  @Nullable private LocalDateTime startOfTerm;

  @Column(name = "end_of_term")
  @Convert(converter = LocalDateTimeConverter.class)
  @Nullable private LocalDateTime endOfTerm;

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

  public CaseEntity() {
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

  public Set<CaseAccountAssignmentEntity> getAccountAssignments() {
    return accountAssignments;
  }

  public void setAccountAssignments(Set<CaseAccountAssignmentEntity> accountAssignments) {
    this.accountAssignments = accountAssignments;
  }

  public Set<TaskInstanceEntity> getTaskInstances() {
    return taskInstances;
  }

  public void setTaskInstances(Set<TaskInstanceEntity> taskInstances) {
    this.taskInstances = taskInstances;
  }

  public String getCurrentState() {
    return currentState;
  }

  public void setCurrentState(String currentState) {
    this.currentState = currentState;
  }

  @Nullable
  public LocalDateTime getStartOfTerm() {
    return startOfTerm;
  }

  public void setStartOfTerm(@Nullable LocalDateTime startOfTerm) {
    this.startOfTerm = startOfTerm;
  }

  public LocalDateTime getEndOfTerm() {
    return endOfTerm;
  }

  public void setEndOfTerm(LocalDateTime endOfTerm) {
    this.endOfTerm = endOfTerm;
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
    if (o == null || getClass() != o.getClass()) return false;
    CaseEntity that = (CaseEntity) o;
    return Objects.equals(identifier, that.identifier) &&
        Objects.equals(productIdentifier, that.productIdentifier);
  }

  @Override
  public int hashCode() {
    return Objects.hash(identifier, productIdentifier);
  }
}