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
package org.apache.fineract.cn.individuallending.internal.repository;

import java.time.LocalDateTime;
import java.util.Objects;
import javax.persistence.Column;
import javax.persistence.Convert;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;
import org.apache.fineract.cn.mariadb.util.LocalDateTimeConverter;

/**
 * @author Myrle Krantz
 */
@Entity
@Table(name = "bastet_il_late_cases")
public class LateCaseEntity {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "id")
  private Long id;

  @Column(name = "case_id")
  private Long caseId;

  /** The date after the most recent payment due date at the time at which lateness was determined.
   */
  @Column(name = "late_since")
  @Convert(converter = LocalDateTimeConverter.class)
  private LocalDateTime lateSince;

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public Long getCaseId() {
    return caseId;
  }

  public void setCaseId(Long caseId) {
    this.caseId = caseId;
  }

  public LocalDateTime getLateSince() {
    return lateSince;
  }

  public void setLateSince(LocalDateTime lateSince) {
    this.lateSince = lateSince;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    LateCaseEntity that = (LateCaseEntity) o;
    return Objects.equals(caseId, that.caseId);
  }

  @Override
  public int hashCode() {
    return Objects.hash(caseId);
  }
}
