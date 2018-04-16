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
package org.apache.fineract.cn.portfolio.api.v1.domain;

import java.util.Objects;
import org.apache.fineract.cn.lang.validation.constraints.ValidIdentifier;
import org.hibernate.validator.constraints.Length;

/**
 * @author Myrle Krantz
 */
@SuppressWarnings({"WeakerAccess", "unused"})
public final class TaskInstance {
  @ValidIdentifier
  private String taskIdentifier;

  @Length(max = 4096)
  private String comment;

  private String executedOn;

  private String executedBy;

  public TaskInstance() {
  }

  public TaskInstance(String taskIdentifier, String comment, String executedOn, String executedBy) {
    this.taskIdentifier = taskIdentifier;
    this.comment = comment;
    this.executedOn = executedOn;
    this.executedBy = executedBy;
  }

  public String getTaskIdentifier() {
    return taskIdentifier;
  }

  public void setTaskIdentifier(String taskIdentifier) {
    this.taskIdentifier = taskIdentifier;
  }

  public String getComment() {
    return comment;
  }

  public void setComment(String comment) {
    this.comment = comment;
  }

  public String getExecutedOn() {
    return executedOn;
  }

  public void setExecutedOn(String executedOn) {
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
    TaskInstance that = (TaskInstance) o;
    return Objects.equals(taskIdentifier, that.taskIdentifier) &&
        Objects.equals(comment, that.comment) &&
        Objects.equals(executedOn, that.executedOn) &&
        Objects.equals(executedBy, that.executedBy);
  }

  @Override
  public int hashCode() {
    return Objects.hash(taskIdentifier, comment, executedOn, executedBy);
  }

  @Override
  public String toString() {
    return "TaskInstance{" +
        "taskIdentifier='" + taskIdentifier + '\'' +
        ", comment='" + comment + '\'' +
        ", executedOn='" + executedOn + '\'' +
        ", executedBy='" + executedBy + '\'' +
        '}';
  }
}
