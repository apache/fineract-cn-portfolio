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
package org.apache.fineract.cn.individuallending.rest;


import org.apache.fineract.cn.individuallending.api.v1.domain.caseinstance.PlannedPaymentPage;
import org.apache.fineract.cn.individuallending.internal.service.DataContextOfAction;
import org.apache.fineract.cn.individuallending.internal.service.DataContextService;
import org.apache.fineract.cn.individuallending.internal.service.IndividualLoanService;
import org.apache.fineract.cn.portfolio.api.v1.PermittableGroupIds;
import org.apache.fineract.cn.portfolio.api.v1.domain.Case;
import java.time.LocalDate;
import java.util.Collections;
import java.util.Optional;
import org.apache.fineract.cn.anubis.annotation.AcceptedTokenType;
import org.apache.fineract.cn.anubis.annotation.Permittable;
import org.apache.fineract.cn.lang.DateConverter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author Myrle Krantz
 */
@SuppressWarnings("unused")
@RestController
@RequestMapping()
public class PlannedPaymentsRestController {
  private final DataContextService dataContextService;
  private final IndividualLoanService individualLoanService;

  @Autowired
  public PlannedPaymentsRestController(
      final DataContextService dataContextService,
      final IndividualLoanService individualLoanService) {
    this.dataContextService = dataContextService;
    this.individualLoanService = individualLoanService;
  }

  @Permittable(value = AcceptedTokenType.TENANT, groupId = PermittableGroupIds.CASE_MANAGEMENT)
  @RequestMapping(
      value = "/individuallending/products/{productidentifier}/cases/{caseidentifier}/plannedpayments",
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
    final DataContextOfAction dataContextOfAction = dataContextService.checkedGetDataContext(
        productIdentifier,
        caseIdentifier,
        Collections.emptyList());

    return individualLoanService.getPlannedPaymentsPage(
        dataContextOfAction,
        getPlannedPaymentWindow(pageIndex, size, initialDisbursalDate));
  }

  @RequestMapping(
      value = "/individuallending/products/{productidentifier}/plannedpayments",
      method = RequestMethod.POST,
      consumes = MediaType.ALL_VALUE,
      produces = MediaType.APPLICATION_JSON_VALUE)
  public @ResponseBody
  PlannedPaymentPage getPaymentScheduleForParameters(
      @PathVariable("productidentifier") final String productIdentifier,
      @RequestParam(value = "pageIndex", required = false) final Integer pageIndex,
      @RequestParam(value = "size", required = false) final Integer size,
      @RequestParam(value = "initialDisbursalDate", required = false) final String initialDisbursalDate,
      @RequestBody final Case caseInstance)
  {
    final DataContextOfAction dataContextOfAction = dataContextService.checkedGetDataContext(
        productIdentifier,
        caseInstance,
        Collections.emptyList());

    return individualLoanService.getPlannedPaymentsPage(
        dataContextOfAction,
        getPlannedPaymentWindow(pageIndex, size, initialDisbursalDate));
  }

  private IndividualLoanService.PlannedPaymentWindow getPlannedPaymentWindow(
      final Integer pageIndex,
      final Integer size,
      final String initialDisbursalDate)
  {
    final Optional<LocalDate> parsedInitialDisbursalDate = initialDisbursalDate == null
        ? Optional.empty()
        : Optional.of(DateConverter.fromIsoString(initialDisbursalDate).toLocalDate());
    final Integer pageIndexToUse = pageIndex != null ? pageIndex : 0;
    final Integer sizeToUse = size != null ? size : 20;

    return new IndividualLoanService.PlannedPaymentWindow(pageIndexToUse, sizeToUse, parsedInitialDisbursalDate);
  }
}