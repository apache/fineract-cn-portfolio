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
package io.mifos.individuallending.api.v1.client;

import io.mifos.portfolio.api.v1.domain.CasePage;
import io.mifos.individuallending.api.v1.domain.caseinstance.PlannedPaymentPage;
import io.mifos.core.api.util.CustomFeignClientsConfiguration;
import org.springframework.cloud.netflix.feign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * @author Myrle Krantz
 */
@SuppressWarnings("unused")
@FeignClient (value = "portfolio-v1", path = "/portfolio/v1", configuration = CustomFeignClientsConfiguration.class)
public interface IndividualLending {
  @RequestMapping(
          value = "/individuallending/products/{productidentifier}/cases/{caseidentifier}/plannedpayments",
          method = RequestMethod.GET,
          produces = MediaType.ALL_VALUE,
          consumes = MediaType.APPLICATION_JSON_VALUE
  )
  PlannedPaymentPage getPaymentScheduleForCase(@PathVariable("productidentifier") final String productIdentifier,
                                               @PathVariable("caseidentifier") final String caseIdentifier,
                                               @RequestParam(value = "pageIndex", required = false) final Integer pageIndex,
                                               @RequestParam(value = "size", required = false) final Integer size,
                                               @RequestParam(value = "initialDisbursalDate", required = false) final String initialDisbursalDate);

  @RequestMapping(
          value = "/individuallending/customers/{customeridentifier}/cases",
          method = RequestMethod.GET,
          produces = MediaType.ALL_VALUE,
          consumes = MediaType.APPLICATION_JSON_VALUE
  )
  CasePage getAllCasesForCustomer(@PathVariable(value = "customeridentifier") final String customerIdentifier,
                                  @RequestParam("pageIndex") final Integer pageIndex,
                                  @RequestParam("size") final Integer size);
}