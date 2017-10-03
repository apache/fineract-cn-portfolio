/*
 * Copyright 2017 Kuelap, Inc.
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
package io.mifos.portfolio.service.internal.util;

import io.mifos.accounting.api.v1.EventConstants;
import io.mifos.core.lang.TenantContextHolder;
import io.mifos.core.lang.config.TenantHeaderFilter;
import io.mifos.core.lang.listening.EventExpectation;
import io.mifos.core.lang.listening.EventKey;
import io.mifos.core.lang.listening.TenantedEventListener;
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
