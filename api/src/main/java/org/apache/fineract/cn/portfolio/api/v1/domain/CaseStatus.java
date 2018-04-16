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

/**
 * @author Myrle Krantz
 */
public class CaseStatus {
  private String startOfTerm;
  private String endOfTerm;
  private Case.State currentState; //The same as Case.currentState.

  public CaseStatus() {
  }

  public String getStartOfTerm() {
    return startOfTerm;
  }

  public void setStartOfTerm(String startOfTerm) {
    this.startOfTerm = startOfTerm;
  }

  public String getEndOfTerm() {
    return endOfTerm;
  }

  public void setEndOfTerm(String endOfTerm) {
    this.endOfTerm = endOfTerm;
  }

  public String getCurrentState() {
    return currentState == null ? null : currentState.name();
  }

  public void setCurrentState(String currentState) {
    this.currentState = Case.State.valueOf(currentState);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    CaseStatus that = (CaseStatus) o;
    return Objects.equals(startOfTerm, that.startOfTerm) &&
        Objects.equals(endOfTerm, that.endOfTerm) &&
        currentState == that.currentState;
  }

  @Override
  public int hashCode() {
    return Objects.hash(startOfTerm, endOfTerm, currentState);
  }

  @Override
  public String toString() {
    return "CaseStatus{" +
        "startOfTerm='" + startOfTerm + '\'' +
        ", endOfTerm='" + endOfTerm + '\'' +
        ", currentState=" + currentState +
        '}';
  }
}
