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
package io.mifos.individuallending.api.v1.events;

/**
 * @author Myrle Krantz
 */
public interface IndividualLoanEventConstants {
  String DESTINATION = "portfolio-v1";
  String SELECTOR_NAME = "action";

  String OPEN_INDIVIDUALLOAN_CASE = "open-individualloan-case";
  String DENY_INDIVIDUALLOAN_CASE = "deny-individualloan-case";
  String APPROVE_INDIVIDUALLOAN_CASE = "approve-individualloan-case";
  String DISBURSE_INDIVIDUALLOAN_CASE = "disburse-individualloan-case";
  String APPLY_INTEREST_INDIVIDUALLOAN_CASE = "apply-interest-individualloan-case";
  String ACCEPT_PAYMENT_INDIVIDUALLOAN_CASE = "accept-payment-individualloan-case";
  String MARK_LATE_INDIVIDUALLOAN_CASE = "mark-late-individualloan-case";
  String WRITE_OFF_INDIVIDUALLOAN_CASE = "write-off-individualloan-case";
  String CLOSE_INDIVIDUALLOAN_CASE = "close-individualloan-case";
  String RECOVER_INDIVIDUALLOAN_CASE = "recover-individualloan-case";

  String SELECTOR_OPEN_INDIVIDUALLOAN_CASE = SELECTOR_NAME + " = '" + OPEN_INDIVIDUALLOAN_CASE + "'";
  String SELECTOR_DENY_INDIVIDUALLOAN_CASE = SELECTOR_NAME + " = '" + DENY_INDIVIDUALLOAN_CASE + "'";
  String SELECTOR_APPROVE_INDIVIDUALLOAN_CASE = SELECTOR_NAME + " = '" + APPROVE_INDIVIDUALLOAN_CASE + "'";
  String SELECTOR_DISBURSE_INDIVIDUALLOAN_CASE = SELECTOR_NAME + " = '" + DISBURSE_INDIVIDUALLOAN_CASE + "'";
  String SELECTOR_APPLY_INTEREST_INDIVIDUALLOAN_CASE = SELECTOR_NAME + " = '" + APPLY_INTEREST_INDIVIDUALLOAN_CASE + "'";
  String SELECTOR_ACCEPT_PAYMENT_INDIVIDUALLOAN_CASE = SELECTOR_NAME + " = '" + ACCEPT_PAYMENT_INDIVIDUALLOAN_CASE + "'";
  String SELECTOR_MARK_LATE_INDIVIDUALLOAN_CASE = SELECTOR_NAME + " = '" + MARK_LATE_INDIVIDUALLOAN_CASE + "'";
  String SELECTOR_WRITE_OFF_INDIVIDUALLOAN_CASE = SELECTOR_NAME + " = '" + WRITE_OFF_INDIVIDUALLOAN_CASE + "'";
  String SELECTOR_CLOSE_INDIVIDUALLOAN_CASE = SELECTOR_NAME + " = '" + CLOSE_INDIVIDUALLOAN_CASE + "'";
  String SELECTOR_RECOVER_INDIVIDUALLOAN_CASE = SELECTOR_NAME + " = '" + RECOVER_INDIVIDUALLOAN_CASE + "'";
}
