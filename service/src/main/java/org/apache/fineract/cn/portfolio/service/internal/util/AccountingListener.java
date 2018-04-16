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
package org.apache.fineract.cn.portfolio.service.internal.util;

import org.apache.fineract.cn.accounting.api.v1.EventConstants;
import org.apache.fineract.cn.lang.TenantContextHolder;
import org.apache.fineract.cn.lang.config.TenantHeaderFilter;
import org.apache.fineract.cn.lang.listening.EventExpectation;
import org.apache.fineract.cn.lang.listening.EventKey;
import org.apache.fineract.cn.lang.listening.TenantedEventListener;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

/**
 * @author Myrle Krantz
 */
@Component
public class AccountingListener {
  private final TenantedEventListener eventListener = new TenantedEventListener();

  @JmsListener(
      destination = EventConstants.DESTINATION,
      selector = EventConstants.SELECTOR_POST_LEDGER,
      subscription = EventConstants.DESTINATION
  )
  public void onPostLedger(@Header(TenantHeaderFilter.TENANT_HEADER) final String tenant,
                           final String payload) {
    this.eventListener.notify(new EventKey(tenant, EventConstants.POST_LEDGER, payload));
  }


  EventExpectation expectLedgerCreation(final String ledgerIdentifier) {
    return eventListener.expect(new EventKey(TenantContextHolder.checkedGetIdentifier(), EventConstants.POST_LEDGER, ledgerIdentifier));
  }
}
