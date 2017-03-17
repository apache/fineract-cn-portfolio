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
package io.mifos.portfolio.listener;

import io.mifos.individuallending.api.v1.events.IndividualLoanCommandEvent;
import io.mifos.individuallending.api.v1.events.IndividualLoanEventConstants;
import io.mifos.core.lang.config.TenantHeaderFilter;
import io.mifos.core.test.listener.EventRecorder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

/**
 * @author Myrle Krantz
 */
@SuppressWarnings("unused")
@Component
public class IndividualLoanCaseCommandEventListener {
  private final EventRecorder eventRecorder;

  @Autowired
  public IndividualLoanCaseCommandEventListener(final EventRecorder eventRecorder) {
    super();
    this.eventRecorder = eventRecorder;
  }

  @JmsListener(
          subscription = IndividualLoanEventConstants.DESTINATION,
          destination = IndividualLoanEventConstants.DESTINATION,
          selector = IndividualLoanEventConstants.SELECTOR_OPEN_INDIVIDUALLOAN_CASE
  )
  public void onOpen(@Header(TenantHeaderFilter.TENANT_HEADER) final String tenant,
                     final String payload) {
    this.eventRecorder.event(tenant, IndividualLoanEventConstants.OPEN_INDIVIDUALLOAN_CASE, payload, IndividualLoanCommandEvent.class);
  }

  @JmsListener(
          subscription = IndividualLoanEventConstants.DESTINATION,
          destination = IndividualLoanEventConstants.DESTINATION,
          selector = IndividualLoanEventConstants.SELECTOR_DENY_INDIVIDUALLOAN_CASE
  )
  public void onDeny(@Header(TenantHeaderFilter.TENANT_HEADER) final String tenant,
                     final String payload) {
    this.eventRecorder.event(tenant, IndividualLoanEventConstants.DENY_INDIVIDUALLOAN_CASE, payload, IndividualLoanCommandEvent.class);
  }

  @JmsListener(
          subscription = IndividualLoanEventConstants.DESTINATION,
          destination = IndividualLoanEventConstants.DESTINATION,
          selector = IndividualLoanEventConstants.SELECTOR_APPROVE_INDIVIDUALLOAN_CASE
  )
  public void onApprove(@Header(TenantHeaderFilter.TENANT_HEADER) final String tenant,
                        final String payload) {
    this.eventRecorder.event(tenant, IndividualLoanEventConstants.APPROVE_INDIVIDUALLOAN_CASE, payload, IndividualLoanCommandEvent.class);
  }

  @JmsListener(
          subscription = IndividualLoanEventConstants.DESTINATION,
          destination = IndividualLoanEventConstants.DESTINATION,
          selector = IndividualLoanEventConstants.SELECTOR_DISBURSE_INDIVIDUALLOAN_CASE
  )
  public void onDisburse(@Header(TenantHeaderFilter.TENANT_HEADER) final String tenant,
                         final String payload) {
    this.eventRecorder.event(tenant, IndividualLoanEventConstants.DISBURSE_INDIVIDUALLOAN_CASE, payload, IndividualLoanCommandEvent.class);
  }

  @JmsListener(
          subscription = IndividualLoanEventConstants.DESTINATION,
          destination = IndividualLoanEventConstants.DESTINATION,
          selector = IndividualLoanEventConstants.SELECTOR_APPLY_INTEREST_INDIVIDUALLOAN_CASE
  )
  public void onApplyInterest(@Header(TenantHeaderFilter.TENANT_HEADER) final String tenant,
                              final String payload) {
    this.eventRecorder.event(tenant, IndividualLoanEventConstants.APPLY_INTEREST_INDIVIDUALLOAN_CASE, payload, IndividualLoanCommandEvent.class);
  }

  @JmsListener(
          subscription = IndividualLoanEventConstants.DESTINATION,
          destination = IndividualLoanEventConstants.DESTINATION,
          selector = IndividualLoanEventConstants.SELECTOR_ACCEPT_PAYMENT_INDIVIDUALLOAN_CASE
  )
  public void onAcceptPayment(@Header(TenantHeaderFilter.TENANT_HEADER) final String tenant,
                              final String payload) {
    this.eventRecorder.event(tenant, IndividualLoanEventConstants.ACCEPT_PAYMENT_INDIVIDUALLOAN_CASE, payload, IndividualLoanCommandEvent.class);
  }

  @JmsListener(
          subscription = IndividualLoanEventConstants.DESTINATION,
          destination = IndividualLoanEventConstants.DESTINATION,
          selector = IndividualLoanEventConstants.SELECTOR_MARK_LATE_INDIVIDUALLOAN_CASE
  )
  public void onMarkLate(@Header(TenantHeaderFilter.TENANT_HEADER) final String tenant,
                         final String payload) {
    this.eventRecorder.event(tenant, IndividualLoanEventConstants.MARK_LATE_INDIVIDUALLOAN_CASE, payload, IndividualLoanCommandEvent.class);
  }

  @JmsListener(
          subscription = IndividualLoanEventConstants.DESTINATION,
          destination = IndividualLoanEventConstants.DESTINATION,
          selector = IndividualLoanEventConstants.SELECTOR_WRITE_OFF_INDIVIDUALLOAN_CASE
  )
  public void onWriteOff(@Header(TenantHeaderFilter.TENANT_HEADER) final String tenant,
                         final String payload) {
    this.eventRecorder.event(tenant, IndividualLoanEventConstants.WRITE_OFF_INDIVIDUALLOAN_CASE, payload, IndividualLoanCommandEvent.class);
  }

  @JmsListener(
          subscription = IndividualLoanEventConstants.DESTINATION,
          destination = IndividualLoanEventConstants.DESTINATION,
          selector = IndividualLoanEventConstants.SELECTOR_CLOSE_INDIVIDUALLOAN_CASE
  )
  public void onClose(@Header(TenantHeaderFilter.TENANT_HEADER) final String tenant,
                        final String payload) {
    this.eventRecorder.event(tenant, IndividualLoanEventConstants.CLOSE_INDIVIDUALLOAN_CASE, payload, IndividualLoanCommandEvent.class);
  }

  @JmsListener(
          subscription = IndividualLoanEventConstants.DESTINATION,
          destination = IndividualLoanEventConstants.DESTINATION,
          selector = IndividualLoanEventConstants.SELECTOR_RECOVER_INDIVIDUALLOAN_CASE
  )
  public void onRecover(@Header(TenantHeaderFilter.TENANT_HEADER) final String tenant,
                        final String payload) {
    this.eventRecorder.event(tenant, IndividualLoanEventConstants.RECOVER_INDIVIDUALLOAN_CASE, payload, IndividualLoanCommandEvent.class);
  }
}
