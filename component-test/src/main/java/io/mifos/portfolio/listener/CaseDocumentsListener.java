package io.mifos.portfolio.listener;

import io.mifos.core.lang.config.TenantHeaderFilter;
import io.mifos.core.test.listener.EventRecorder;
import io.mifos.individuallending.api.v1.events.IndividualLoanEventConstants;
import io.mifos.portfolio.api.v1.events.CaseEvent;
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
