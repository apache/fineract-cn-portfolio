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

import javax.annotation.Nullable;
import java.util.Objects;

@SuppressWarnings("unused")
public class RequiredAccountAssignment {
  private String accountDesignator;
  private String accountType;
  private @Nullable String group;

  public RequiredAccountAssignment(String accountDesignator, String accountType) {
    this.accountDesignator = accountDesignator;
    this.accountType = accountType;
    this.group = null;
  }

  public RequiredAccountAssignment(String accountDesignator, String accountType, String ledger) {
    this.accountDesignator = accountDesignator;
    this.accountType = accountType;
    this.group = ledger;
  }

  public String getAccountDesignator() {
    return accountDesignator;
  }

  public void setAccountDesignator(String accountDesignator) {
    this.accountDesignator = accountDesignator;
  }

  public String getAccountType() {
    return accountType;
  }

  public void setAccountType(String thothType) {
    this.accountType = thothType;
  }

  public String getGroup() {
    return group;
  }

  public void setGroup(String group) {
    this.group = group;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    RequiredAccountAssignment that = (RequiredAccountAssignment) o;
    return Objects.equals(accountDesignator, that.accountDesignator) &&
        Objects.equals(accountType, that.accountType) &&
        Objects.equals(group, that.group);
  }

  @Override
  public int hashCode() {
    return Objects.hash(accountDesignator, accountType, group);
  }

  @Override
  public String toString() {
    return "RequiredAccountAssignment{" +
        "accountDesignator='" + accountDesignator + '\'' +
        ", accountType='" + accountType + '\'' +
        ", group='" + group + '\'' +
        '}';
  }
}
