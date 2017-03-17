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
package io.mifos.portfolio.api.v1.domain;

import io.mifos.core.lang.validation.constraints.ValidIdentifier;

import javax.validation.constraints.NotNull;

/**
 * @author Myrle Krantz
 */
@SuppressWarnings({"WeakerAccess", "unused"})
public final class TaskInstance {
  @NotNull
  private TaskDefinition taskDefinition;

  private String comment;
  private String executedOn;

  @ValidIdentifier
  private String executedBy;

  public TaskInstance() {
  }

  public TaskInstance(TaskDefinition taskDefinition, String comment, String executedOn, String executedBy) {
    this.taskDefinition = taskDefinition;
    this.comment = comment;
    this.executedOn = executedOn;
    this.executedBy = executedBy;
  }

  public TaskDefinition getTaskDefinition() {
    return taskDefinition;
  }

  public void setTaskDefinition(TaskDefinition taskDefinition) {
    this.taskDefinition = taskDefinition;
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

    return taskDefinition != null ? taskDefinition.equals(that.taskDefinition) : that.taskDefinition == null && (comment != null ? comment.equals(that.comment) : that.comment == null && (executedOn != null ? executedOn.equals(that.executedOn) : that.executedOn == null && (executedBy != null ? executedBy.equals(that.executedBy) : that.executedBy == null)));

  }

  @Override
  public int hashCode() {
    int result = taskDefinition != null ? taskDefinition.hashCode() : 0;
    result = 31 * result + (comment != null ? comment.hashCode() : 0);
    result = 31 * result + (executedOn != null ? executedOn.hashCode() : 0);
    result = 31 * result + (executedBy != null ? executedBy.hashCode() : 0);
    return result;
  }

  @Override
  public String toString() {
    return "TaskInstance{" +
            "taskDefinition=" + taskDefinition +
            ", comment='" + comment + '\'' +
            ", executedOn='" + executedOn + '\'' +
            ", executedBy='" + executedBy + '\'' +
            '}';
  }
}
