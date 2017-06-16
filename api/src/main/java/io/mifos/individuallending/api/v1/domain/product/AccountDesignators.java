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
package io.mifos.individuallending.api.v1.domain.product;

/**
 * @author Myrle Krantz
 */
@SuppressWarnings("unused")
public interface AccountDesignators {
  String CUSTOMER_LOAN = "customer-loan";
  String PENDING_DISBURSAL = "pending-disbursal";
  String LOAN_FUNDS_SOURCE = "loan-funds-source";
  String PROCESSING_FEE_INCOME = "processing-fee-income";
  String ORIGINATION_FEE_INCOME = "origination-fee-income";
  String DISBURSEMENT_FEE_INCOME = "disbursement-fee-income";
  String INTEREST_INCOME = "interest-income";
  String INTEREST_ACCRUAL = "interest-accrual";
  String LATE_FEE_INCOME = "late-fee-income";
  String LATE_FEE_ACCRUAL = "late-fee-accrual";
  String ARREARS_ALLOWANCE = "arrears-allowance";
  String ENTRY = "entry";
}
