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
import java.util.Set;

/**
 * @author Myrle Krantz
 */
@SuppressWarnings({"WeakerAccess", "unused"})
public class Pattern {
  private String parameterPackage;
  private Set<String> accountAssignmentGroups;
  private Set<RequiredAccountAssignment> accountAssignmentsRequired;

  public Pattern() {
  }

  public String getParameterPackage() {
    return parameterPackage;
  }

  public void setParameterPackage(String parameterPackage) {
    this.parameterPackage = parameterPackage;
  }

  public Set<String> getAccountAssignmentGroups() {
    return accountAssignmentGroups;
  }

  public void setAccountAssignmentGroups(Set<String> accountAssignmentGroups) {
    this.accountAssignmentGroups = accountAssignmentGroups;
  }

  public Set<RequiredAccountAssignment> getAccountAssignmentsRequired() {
    return accountAssignmentsRequired;
  }

  public void setAccountAssignmentsRequired(Set<RequiredAccountAssignment> accountAssignmentsRequired) {
    this.accountAssignmentsRequired = accountAssignmentsRequired;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    Pattern pattern = (Pattern) o;
    return Objects.equals(parameterPackage, pattern.parameterPackage) &&
        Objects.equals(accountAssignmentGroups, pattern.accountAssignmentGroups) &&
        Objects.equals(accountAssignmentsRequired, pattern.accountAssignmentsRequired);
  }

  @Override
  public int hashCode() {
    return Objects.hash(parameterPackage, accountAssignmentGroups, accountAssignmentsRequired);
  }

  @Override
  public String toString() {
    return "Pattern{" +
        "parameterPackage='" + parameterPackage + '\'' +
        ", accountAssignmentGroups=" + accountAssignmentGroups +
        ", accountAssignmentsRequired=" + accountAssignmentsRequired +
        '}';
  }
}
