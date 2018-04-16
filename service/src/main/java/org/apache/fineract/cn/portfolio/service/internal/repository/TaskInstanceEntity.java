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

import java.time.LocalDateTime;
import java.util.Objects;
import javax.persistence.Column;
import javax.persistence.Convert;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import org.apache.fineract.cn.mariadb.util.LocalDateTimeConverter;

/**
 * @author Myrle Krantz
 */
@SuppressWarnings({"WeakerAccess", "unused"})
@Entity
@Table(name = "bastet_c_task_insts")
public class TaskInstanceEntity {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "id")
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "case_id")
  private CaseEntity customerCase;

  @ManyToOne(fetch = FetchType.EAGER, optional = false)
  @JoinColumn(name = "task_def_id")
  private TaskDefinitionEntity taskDefinition;

  @Column(name = "a_comment")
  private String comment;

  @Column(name = "executed_on")
  @Convert(converter = LocalDateTimeConverter.class)
  private LocalDateTime executedOn;

  @Column(name = "executed_by")
  private String executedBy;

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public CaseEntity getCustomerCase() {
    return customerCase;
  }

  public void setCustomerCase(CaseEntity customerCase) {
    this.customerCase = customerCase;
  }

  public TaskDefinitionEntity getTaskDefinition() {
    return taskDefinition;
  }

  public void setTaskDefinition(TaskDefinitionEntity taskDefinition) {
    this.taskDefinition = taskDefinition;
  }

  public String getComment() {
    return comment;
  }

  public void setComment(String comment) {
    this.comment = comment;
  }

  public LocalDateTime getExecutedOn() {
    return executedOn;
  }

  public void setExecutedOn(LocalDateTime executedOn) {
    this.executedOn = executedOn;
  }

  public String getExecutedBy() {
    return executedBy;
  }

  public void setExecutedBy(String executedBy) {
    this.executedBy = executedBy;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    TaskInstanceEntity that = (TaskInstanceEntity) o;
    return Objects.equals(customerCase, that.customerCase) &&
        Objects.equals(taskDefinition, that.taskDefinition);
  }

  @Override
  public int hashCode() {
    return Objects.hash(customerCase, taskDefinition);
  }
}
