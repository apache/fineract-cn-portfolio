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

import java.util.Locale;

/**
 * @author Myrle Krantz
 */
@SuppressWarnings("unused")
public interface ChargeIdentifiers {
  String INTEREST_NAME = "Interest";
  String INTEREST_ID = "interest";
  String LATE_FEE_NAME = "Late fee";
  String LATE_FEE_ID = "late-fee";
  String DISBURSEMENT_FEE_NAME = "Disbursement fee";
  String DISBURSEMENT_FEE_ID = "disbursement-fee";
  String DISBURSE_PAYMENT_NAME = "Disburse payment";
  String DISBURSE_PAYMENT_ID = "disburse-payment";
  String LOAN_ORIGINATION_FEE_NAME = "Loan origination fee";
  String LOAN_ORIGINATION_FEE_ID = "loan-origination-fee";
  String PROCESSING_FEE_NAME = "Processing fee";
  String PROCESSING_FEE_ID = "processing-fee";
  String REPAY_PRINCIPAL_NAME = "Repay principal";
  String REPAY_PRINCIPAL_ID = "repay-principal";
  String REPAY_INTEREST_NAME = "Repay interest";
  String REPAY_INTEREST_ID = "repay-interest";
  String REPAY_FEES_NAME = "Repay fees";
  String REPAY_FEES_ID = "repay-fees";
  String PROVISION_FOR_LOSSES_NAME = "Provision for losses";
  String PROVISION_FOR_LOSSES_ID = "loss-provisioning";
  String WRITE_OFF_NAME = "Write off";
  String WRITE_OFF_ID = "write-off";

  static String nameToIdentifier(String name) {
    return name.toLowerCase(Locale.US).replace(" ", "-");
  }
}
