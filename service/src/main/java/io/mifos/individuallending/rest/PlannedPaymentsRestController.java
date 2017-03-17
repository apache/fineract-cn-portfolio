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
package io.mifos.individuallending.rest;


import io.mifos.anubis.annotation.AcceptedTokenType;
import io.mifos.anubis.annotation.Permittable;
import io.mifos.individuallending.internal.service.CaseParametersService;
import io.mifos.individuallending.internal.service.IndividualLoanService;
import io.mifos.core.lang.DateConverter;
import io.mifos.core.lang.ServiceException;
import io.mifos.individuallending.api.v1.domain.caseinstance.CaseParameters;
import io.mifos.individuallending.api.v1.domain.caseinstance.PlannedPaymentPage;
import io.mifos.portfolio.api.v1.PermittableGroupIds;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.ZoneId;

/**
 * @author Myrle Krantz
 */
@SuppressWarnings("unused")
@RestController
@RequestMapping("/individuallending/products/{productidentifier}/cases/{caseidentifier}/plannedpayments")
public class PlannedPaymentsRestController {
  private final CaseParametersService caseParametersService;
  private final IndividualLoanService individualLoanService;

  @Autowired
  public PlannedPaymentsRestController(
          final CaseParametersService caseParametersService,
          final IndividualLoanService individualLoanService) {
    this.caseParametersService = caseParametersService;
    this.individualLoanService = individualLoanService;
  }

  @Permittable(value = AcceptedTokenType.TENANT, groupId = PermittableGroupIds.CASE_MANAGEMENT)
  @RequestMapping(
          method = RequestMethod.GET,
          consumes = MediaType.ALL_VALUE,
          produces = MediaType.APPLICATION_JSON_VALUE)
  public @ResponseBody
  PlannedPaymentPage getPaymentScheduleForCase(
          @PathVariable("productidentifier") final String productIdentifier,
          @PathVariable("caseidentifier") final String caseIdentifier,
          @RequestParam(value = "pageIndex", required = false) final Integer pageIndex,
          @RequestParam(value = "size", required = false) final Integer size,
          @RequestParam(value = "initialDisbursalDate", required = false) final String initialDisbursalDate)
  {
    final CaseParameters caseParameters = caseParametersService
            .findByIdentifier(productIdentifier, caseIdentifier)
            .orElseThrow(() -> ServiceException.notFound(
                    "Instance with identifier " + productIdentifier + "." + caseIdentifier + " doesn't exist or it is not an individual loan."));


    final LocalDate parsedInitialDisbursalDate = initialDisbursalDate == null
            ? LocalDate.now(ZoneId.of("UTC"))
            : DateConverter.fromIsoString(initialDisbursalDate).toLocalDate();
    final Integer pageIndexToUse = pageIndex != null ? pageIndex : 0;
    final Integer sizeToUse = size != null ? size : 20;

    return individualLoanService.getPlannedPaymentsPage(productIdentifier, caseParameters, pageIndexToUse, sizeToUse, parsedInitialDisbursalDate);
  }
}