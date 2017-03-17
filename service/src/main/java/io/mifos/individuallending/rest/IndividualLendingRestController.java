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
import io.mifos.portfolio.api.v1.PermittableGroupIds;
import io.mifos.portfolio.api.v1.domain.CasePage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

/**
 * @author Myrle Krantz
 */
@RestController
public class IndividualLendingRestController {
  private final CaseParametersService caseParametersService;

  @Autowired
  public IndividualLendingRestController(final CaseParametersService caseParametersService) {
    this.caseParametersService = caseParametersService;
  }

  @Permittable(value = AcceptedTokenType.TENANT, groupId = PermittableGroupIds.CASE_MANAGEMENT)
  @RequestMapping(
          value = "/individuallending/customers/{customeridentifier}/cases",
          method = RequestMethod.GET,
          consumes = MediaType.ALL_VALUE,
          produces = MediaType.APPLICATION_JSON_VALUE)
  public @ResponseBody
  CasePage
  getAllCasesForCustomer(@PathVariable(value = "customeridentifier") final String customerIdentifier,
                         @RequestParam("pageIndex") final Integer pageIndex,
                         @RequestParam("size") final Integer size)
  {
    return caseParametersService.findByCustomerIdentifier(
            customerIdentifier,
            pageIndex, size);
  }
}
