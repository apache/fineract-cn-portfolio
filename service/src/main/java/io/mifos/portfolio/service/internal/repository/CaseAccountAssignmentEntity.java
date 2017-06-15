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

import javax.persistence.*;
import java.util.Objects;

/**
 * @author Myrle Krantz
 */
@SuppressWarnings("unused")
@Entity
@Table(name = "bastet_c_acct_assigns")
public class CaseAccountAssignmentEntity {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "id")
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "case_id", nullable = false)
  private CaseEntity caseEntity;

  @Column(name = "designator")
  private String designator;

  @Column(name = "identifier")
  private String identifier;

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public CaseEntity getCaseEntity() {
    return caseEntity;
  }

  public void setCaseEntity(CaseEntity caseEntity) {
    this.caseEntity = caseEntity;
  }

  public String getDesignator() {
    return designator;
  }

  public void setDesignator(String designator) {
    this.designator = designator;
  }

  public String getIdentifier() {
    return identifier;
  }

  public void setIdentifier(String identifier) {
    this.identifier = identifier;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    CaseAccountAssignmentEntity that = (CaseAccountAssignmentEntity) o;
    return Objects.equals(caseEntity, that.caseEntity) &&
            Objects.equals(designator, that.designator);
  }

  @Override
  public int hashCode() {
    return Objects.hash(caseEntity, designator);
  }
}
