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
package org.apache.fineract.cn.individuallending.api.v1.domain.product;

/**
 * @author Myrle Krantz
 */
@SuppressWarnings("unused")
public interface AccountDesignators {
  //These are maximum 3 characters because they are used to create account and ledger identifiers.
  //Account and ledger identifiers are limited to 34 characters, and 32 characters respectively.
  //These accounting identifiers are composed of the customer identifier, this identifier, and a counter.
  String CUSTOMER_LOAN_GROUP = "cll";
  String CUSTOMER_LOAN_PRINCIPAL = "clp";
  String CUSTOMER_LOAN_INTEREST = "cli";
  String CUSTOMER_LOAN_FEES = "clf";
  String LOAN_FUNDS_SOURCE = "ls";
  String PROCESSING_FEE_INCOME = "pfi";
  String ORIGINATION_FEE_INCOME = "ofi";
  String DISBURSEMENT_FEE_INCOME = "dfi";
  String INTEREST_INCOME = "ii";
  String INTEREST_ACCRUAL = "ia";
  String LATE_FEE_INCOME = "lfi";
  String LATE_FEE_ACCRUAL = "lfa";
  String PRODUCT_LOSS_ALLOWANCE = "pa";
  String GENERAL_LOSS_ALLOWANCE = "aa";
  String EXPENSE = "ee";
  String ENTRY = "ey";
}
