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
package org.apache.fineract.cn.portfolio.api.v1.events;

import java.util.Objects;

/**
 * @author Myrle Krantz
 */
@SuppressWarnings("unused")
public class TaskInstanceEvent {
  private String productIdentifier;
  private String caseIdentifier;
  private String taskIdentifier;

  public TaskInstanceEvent(String productIdentifier, String caseIdentifier, String taskIdentifier) {
    this.productIdentifier = productIdentifier;
    this.caseIdentifier = caseIdentifier;
    this.taskIdentifier = taskIdentifier;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    TaskInstanceEvent that = (TaskInstanceEvent) o;
    return Objects.equals(productIdentifier, that.productIdentifier) &&
        Objects.equals(caseIdentifier, that.caseIdentifier) &&
        Objects.equals(taskIdentifier, that.taskIdentifier);
  }

  @Override
  public int hashCode() {
    return Objects.hash(productIdentifier, caseIdentifier, taskIdentifier);
  }

  @Override
  public String toString() {
    return "TaskInstanceEvent{" +
        "productIdentifier='" + productIdentifier + '\'' +
        ", caseIdentifier='" + caseIdentifier + '\'' +
        ", taskIdentifier='" + taskIdentifier + '\'' +
        '}';
  }
}
