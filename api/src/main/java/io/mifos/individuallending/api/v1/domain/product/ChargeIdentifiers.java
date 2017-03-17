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

import java.util.Locale;

/**
 * @author Myrle Krantz
 */
@SuppressWarnings("unused")
public interface ChargeIdentifiers {
  String LOAN_FUNDS_ALLOCATION_NAME = "Loan funds allocation";
  String LOAN_FUNDS_ALLOCATION_ID = nameToIdentifier(LOAN_FUNDS_ALLOCATION_NAME);
  String RETURN_DISBURSEMENT_NAME = "Return disbursement";
  String RETURN_DISBURSEMENT_ID = nameToIdentifier(RETURN_DISBURSEMENT_NAME);
  String INTEREST_NAME = "Interest";
  String INTEREST_ID = nameToIdentifier(INTEREST_NAME);
  String ALLOW_FOR_WRITE_OFF_NAME = "Allow for write-off";
  String ALLOW_FOR_WRITE_OFF_ID = nameToIdentifier(ALLOW_FOR_WRITE_OFF_NAME);
  String LATE_FEE_NAME = "Late fee";
  String LATE_FEE_ID = nameToIdentifier(LATE_FEE_NAME);
  String DISBURSEMENT_FEE_NAME = "Disbursement fee";
  String DISBURSEMENT_FEE_ID = nameToIdentifier(DISBURSEMENT_FEE_NAME);
  String LOAN_ORIGINATION_FEE_NAME = "Loan origination fee";
  String LOAN_ORIGINATION_FEE_ID = nameToIdentifier(LOAN_ORIGINATION_FEE_NAME);
  String PROCESSING_FEE_NAME = "Processing fee";
  String PROCESSING_FEE_ID = nameToIdentifier(PROCESSING_FEE_NAME);
  String PAYMENT_NAME = "Payment";
  String PAYMENT_ID = nameToIdentifier(PAYMENT_NAME);

  static String nameToIdentifier(String name) {
    return name.toLowerCase(Locale.US).replace(" ", "-");
  }
}
