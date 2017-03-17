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

import java.util.Set;

/**
 * @author Myrle Krantz
 */
@SuppressWarnings({"WeakerAccess", "unused"})
public class Pattern {
  private String parameterPackage;
  private Set<String> accountAssignmentsRequired;

  public Pattern() {
  }

  public Pattern(String parametersNameSpace, Set<String> accountAssignmentsRequired) {
    this.parameterPackage = parametersNameSpace;
    this.accountAssignmentsRequired = accountAssignmentsRequired;
  }

  public String getParameterPackage() {
    return parameterPackage;
  }

  public void setParameterPackage(String parameterPackage) {
    this.parameterPackage = parameterPackage;
  }

  public Set<String> getAccountAssignmentsRequired() {
    return accountAssignmentsRequired;
  }

  public void setAccountAssignmentsRequired(Set<String> accountAssignmentsRequired) {
    this.accountAssignmentsRequired = accountAssignmentsRequired;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    Pattern pattern = (Pattern) o;

    return parameterPackage != null ? parameterPackage.equals(pattern.parameterPackage) : pattern.parameterPackage == null && (accountAssignmentsRequired != null ? accountAssignmentsRequired.equals(pattern.accountAssignmentsRequired) : pattern.accountAssignmentsRequired == null);

  }

  @Override
  public int hashCode() {
    int result = parameterPackage != null ? parameterPackage.hashCode() : 0;
    result = 31 * result + (accountAssignmentsRequired != null ? accountAssignmentsRequired.hashCode() : 0);
    return result;
  }

  @Override
  public String toString() {
    return "Pattern{" +
            "parameterPackage='" + parameterPackage + '\'' +
            ", accountAssignmentsRequired=" + accountAssignmentsRequired +
            '}';
  }
}
