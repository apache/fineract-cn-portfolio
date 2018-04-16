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
package org.apache.fineract.cn.portfolio.listener;

import org.apache.fineract.cn.individuallending.api.v1.events.IndividualLoanEventConstants;
import org.apache.fineract.cn.portfolio.api.v1.events.CaseEvent;
import org.apache.fineract.cn.lang.config.TenantHeaderFilter;
import org.apache.fineract.cn.test.listener.EventRecorder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

/**
 * @author Myrle Krantz
 */
@SuppressWarnings("unused")
@Component
public class CaseDocumentsListener {
  private final EventRecorder eventRecorder;

  @Autowired
  public CaseDocumentsListener(final EventRecorder eventRecorder) {
    this.eventRecorder = eventRecorder;
  }

  @JmsListener(
      subscription = IndividualLoanEventConstants.DESTINATION,
      destination = IndividualLoanEventConstants.DESTINATION,
      selector = IndividualLoanEventConstants.SELECTOR_PUT_DOCUMENT
  )
  public void onCreateCase(@Header(TenantHeaderFilter.TENANT_HEADER) final String tenant,
                           final String payload) {
    this.eventRecorder.event(tenant, IndividualLoanEventConstants.PUT_DOCUMENT, payload, CaseEvent.class);
  }
}
