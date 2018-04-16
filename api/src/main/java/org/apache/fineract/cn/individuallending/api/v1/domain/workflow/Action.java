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
package org.apache.fineract.cn.individuallending.api.v1.domain.workflow;

/**
 * The ordering of the actions in this enum is used to determine the order in which these actions are executed.
 * For example if there is an interest due and a payment due on the same day, interest will be calculated before
 * the payment is applied.
 * @author Myrle Krantz
 */
@SuppressWarnings({"WeakerAccess", "unused"})
public enum Action {
  OPEN("CHRG"),
  DENY("CHRG"),
  IMPORT("CHRG"),
  APPROVE("ACCO"),
  DISBURSE("CDIS"),
  APPLY_INTEREST("INTR"),
  ACCEPT_PAYMENT("PPAY"),
  MARK_LATE("ICCT"),
  MARK_IN_ARREARS("ICCT"),
  WRITE_OFF("ICCT"),
  CLOSE("ICCT"),
  RECOVER("ICCT");

  private final String transactionType;

  Action(final String transactionType) {
    this.transactionType = transactionType;
  }

  public String getTransactionType() {
    return transactionType;
  }
}
