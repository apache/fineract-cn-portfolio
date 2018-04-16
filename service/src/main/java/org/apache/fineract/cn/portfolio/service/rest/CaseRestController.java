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
package org.apache.fineract.cn.portfolio.service.rest;

import org.apache.fineract.cn.individuallending.api.v1.domain.workflow.Action;
import org.apache.fineract.cn.portfolio.api.v1.PermittableGroupIds;
import org.apache.fineract.cn.portfolio.api.v1.domain.Case;
import org.apache.fineract.cn.portfolio.api.v1.domain.CasePage;
import org.apache.fineract.cn.portfolio.api.v1.domain.CaseStatus;
import org.apache.fineract.cn.portfolio.api.v1.domain.Command;
import org.apache.fineract.cn.portfolio.api.v1.domain.ImportParameters;
import org.apache.fineract.cn.portfolio.api.v1.domain.Payment;
import org.apache.fineract.cn.portfolio.service.internal.checker.CaseChecker;
import org.apache.fineract.cn.portfolio.service.internal.command.ChangeCaseCommand;
import org.apache.fineract.cn.portfolio.service.internal.command.CreateCaseCommand;
import org.apache.fineract.cn.portfolio.service.internal.service.CaseService;
import org.apache.fineract.cn.portfolio.service.internal.service.ProductService;
import org.apache.fineract.cn.portfolio.service.internal.service.TaskInstanceService;
import org.apache.fineract.cn.products.spi.ProductCommandDispatcher;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Set;
import javax.validation.Valid;
import org.apache.fineract.cn.anubis.annotation.AcceptedTokenType;
import org.apache.fineract.cn.anubis.annotation.Permittable;
import org.apache.fineract.cn.api.util.UserContextHolder;
import org.apache.fineract.cn.command.gateway.CommandGateway;
import org.apache.fineract.cn.lang.DateConverter;
import org.apache.fineract.cn.lang.ServiceException;
import org.apache.fineract.cn.lang.validation.constraints.ValidLocalDateTimeString;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
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
@RequestMapping("/products/{productidentifier}/cases/")
public class CaseRestController {

  private final CommandGateway commandGateway;
  private final CaseService caseService;
  private final CaseChecker caseChecker;
  private final ProductService productService;
  private final TaskInstanceService taskInstanceService;

  @Autowired
  public CaseRestController(
      final CommandGateway commandGateway,
      final CaseService caseService,
      final CaseChecker caseChecker,
      final ProductService productService,
      final TaskInstanceService taskInstanceService) {
    super();
    this.commandGateway = commandGateway;
    this.caseService = caseService;
    this.caseChecker = caseChecker;
    this.productService = productService;
    this.taskInstanceService = taskInstanceService;
  }

  @Permittable(value = AcceptedTokenType.TENANT, groupId = PermittableGroupIds.CASE_MANAGEMENT)
  @RequestMapping(
          method = RequestMethod.GET,
          consumes = MediaType.ALL_VALUE,
          produces = MediaType.APPLICATION_JSON_VALUE
  )
  public @ResponseBody CasePage getAllCasesForProduct(@PathVariable("productidentifier") final String productIdentifier,
                                 @RequestParam(value = "includeClosed", required = false) final Boolean includeClosed,
                                 @RequestParam("pageIndex") final Integer pageIndex,
                                 @RequestParam("size") final Integer size)
  {
    return caseService.findAllEntities(productIdentifier, includeClosed,  pageIndex, size);
  }

  @Permittable(value = AcceptedTokenType.TENANT, groupId = PermittableGroupIds.CASE_MANAGEMENT)
  @RequestMapping(
          method = RequestMethod.POST,
          consumes = MediaType.APPLICATION_JSON_VALUE,
          produces = MediaType.APPLICATION_JSON_VALUE
  )
  public @ResponseBody ResponseEntity<Void> createCase(
          @PathVariable("productidentifier") final String productIdentifier,
          @RequestBody @Valid final Case instance)
  {
    checkThatProductExists(productIdentifier);

    if (!instance.getProductIdentifier().equals(productIdentifier))
      throw ServiceException.badRequest("Product identifier in request body must match product identifier in request path.");

    if (!(instance.getCurrentState() == null || instance.getCurrentState().equals(Case.State.CREATED.name())))
      throw ServiceException.badRequest("Current state must be either 'null', or CREATED upon initial creation.");

    final String user = UserContextHolder.checkedGetUser();
    if (!(instance.getCreatedBy() == null || instance.getCreatedBy().equals(user)))
      throw ServiceException.badRequest("CreatedBy must be either 'null', or the creating user upon initial creation.");

    if (!(instance.getLastModifiedBy() == null || instance.getLastModifiedBy().equals(user)))
      throw ServiceException.badRequest("LastModifiedBy must be either 'null', or the creating user upon initial creation.");

    if (instance.getCreatedOn() != null)
      throw ServiceException.badRequest("CreatedOn must be 'null' upon initial creation.");

    if (instance.getLastModifiedOn() != null)
      throw ServiceException.badRequest("LastModifiedOn must 'null' be upon initial creation.");

    caseChecker.checkForCreate(productIdentifier, instance);

    this.commandGateway.process(new CreateCaseCommand(instance));
    return new ResponseEntity<>(HttpStatus.ACCEPTED);
  }

  @Permittable(value = AcceptedTokenType.TENANT, groupId = PermittableGroupIds.CASE_MANAGEMENT)
  @RequestMapping(
          value = "{caseidentifier}",
          method = RequestMethod.GET,
          consumes = MediaType.ALL_VALUE,
          produces = MediaType.APPLICATION_JSON_VALUE)
  public @ResponseBody Case getCase(
      @PathVariable("productidentifier") final String productIdentifier,
      @PathVariable("caseidentifier") final String caseIdentifier)
  {
    return caseService.findByIdentifier(productIdentifier, caseIdentifier)
        .orElseThrow(() -> ServiceException.notFound(
            "Instance with identifier ''{0}.{1}'' doesn''t exist.", productIdentifier, caseIdentifier));
  }

  @Permittable(value = AcceptedTokenType.TENANT, groupId = PermittableGroupIds.CASE_STATUS)
  @RequestMapping(
      value = "{caseidentifier}/status",
      method = RequestMethod.GET,
      consumes = MediaType.ALL_VALUE,
      produces = MediaType.APPLICATION_JSON_VALUE)
  public @ResponseBody CaseStatus getCaseStatus(
      @PathVariable("productidentifier") final String productIdentifier,
      @PathVariable("caseidentifier") final String caseIdentifier)
  {
    return caseService.findStatusByIdentifier(productIdentifier, caseIdentifier)
        .orElseThrow(() -> ServiceException.notFound(
            "Instance with identifier ''{0}.{1}'' doesn''t exist.", productIdentifier, caseIdentifier));
  }

  @Permittable(value = AcceptedTokenType.TENANT, groupId = PermittableGroupIds.CASE_MANAGEMENT)
  @RequestMapping(
          value = "{caseidentifier}",
          method = RequestMethod.PUT,
          consumes = MediaType.APPLICATION_JSON_VALUE,
          produces = MediaType.APPLICATION_JSON_VALUE
  )
  public @ResponseBody ResponseEntity<Void> changeCase(
          @PathVariable("productidentifier") final String productIdentifier,
          @PathVariable("caseidentifier") final String caseIdentifier,
          @RequestBody @Valid final Case instance)
  {
    checkThatCaseExists(productIdentifier, caseIdentifier);

    if (!productIdentifier.equals(instance.getProductIdentifier()))
      throw ServiceException.badRequest("Product reference may not be changed.");

    if (!caseIdentifier.equals(instance.getIdentifier()))
      throw ServiceException.badRequest("Instance identifier may not be changed.");

    caseChecker.checkForChange(productIdentifier, instance);

    this.commandGateway.process(new ChangeCaseCommand(instance));
    return new ResponseEntity<>(HttpStatus.ACCEPTED);
  }

  @Permittable(value = AcceptedTokenType.TENANT, groupId = PermittableGroupIds.CASE_MANAGEMENT)
  @RequestMapping(
          value = "{caseidentifier}/actions/",
          method = RequestMethod.GET,
          consumes = MediaType.ALL_VALUE,
          produces = MediaType.APPLICATION_JSON_VALUE
  )
  @ResponseBody
  Set<String> getActionsForCase(@PathVariable("productidentifier") final String productIdentifier,
                                @PathVariable("caseidentifier") final String caseIdentifier)
  {
    checkThatCaseExists(productIdentifier, caseIdentifier);

    return caseService.getNextActionsForCase(productIdentifier, caseIdentifier);
  }

  @Permittable(value = AcceptedTokenType.TENANT, groupId = PermittableGroupIds.CASE_MANAGEMENT)
  @RequestMapping(
          value = "{caseidentifier}/actions/{actionidentifier}/costcomponents",
          method = RequestMethod.GET,
          consumes = MediaType.ALL_VALUE,
          produces = MediaType.APPLICATION_JSON_VALUE
  )
  @ResponseBody
  Payment getCostComponentsForAction(
      @PathVariable("productidentifier") final String productIdentifier,
      @PathVariable("caseidentifier") final String caseIdentifier,
      @PathVariable("actionidentifier") final String actionIdentifier,
      @RequestParam(value="fordatetime", required = false, defaultValue = "") final @ValidLocalDateTimeString String forDateTimeString,
      @RequestParam(value="touchingaccounts", required = false, defaultValue = "") final Set<String> forAccountDesignators,
      @RequestParam(value="forpaymentsize", required = false, defaultValue = "") final BigDecimal forPaymentSize)
  {
    actionChecks(productIdentifier, caseIdentifier, actionIdentifier);

    if (forPaymentSize != null && forPaymentSize.compareTo(BigDecimal.ZERO) < 0)
      throw ServiceException.badRequest("forpaymentsize can''t be negative.");

    final LocalDateTime forDateTime = StringUtils.isEmpty(forDateTimeString) ? LocalDateTime.now(Clock.systemUTC()) : DateConverter
        .fromIsoString(forDateTimeString);

    return caseService.getActionCostComponentsForCase(
        productIdentifier,
        caseIdentifier,
        actionIdentifier,
        forDateTime,
        forAccountDesignators,
        forPaymentSize);
  }

  @Permittable(value = AcceptedTokenType.TENANT, groupId = PermittableGroupIds.CASE_MANAGEMENT)
  @RequestMapping(
      value = "{caseidentifier}/commands/{actionidentifier}",
      method = RequestMethod.POST,
      produces = MediaType.APPLICATION_JSON_VALUE,
      consumes = MediaType.APPLICATION_JSON_VALUE
  )
  public @ResponseBody ResponseEntity<Void> executeCaseCommand(
      @PathVariable("productidentifier") final String productIdentifier,
      @PathVariable("caseidentifier") final String caseIdentifier,
      @PathVariable("actionidentifier") final String actionIdentifier,
      @RequestBody @Valid final Command command)
  {
    actionChecks(productIdentifier, caseIdentifier, actionIdentifier);

    final ProductCommandDispatcher productCommandDispatcher = caseService.getProductCommandDispatcher(productIdentifier);
    productCommandDispatcher.dispatch(productIdentifier, caseIdentifier, actionIdentifier, command);

    return new ResponseEntity<>(HttpStatus.ACCEPTED);
  }

  @Permittable(value = AcceptedTokenType.TENANT, groupId = PermittableGroupIds.CASE_STATUS)
  @RequestMapping(
      value = "{caseidentifier}/commands/IMPORT",
      method = RequestMethod.POST,
      produces = MediaType.APPLICATION_JSON_VALUE,
      consumes = MediaType.APPLICATION_JSON_VALUE
  )
  public @ResponseBody ResponseEntity<Void> executeImportCommand(
      @PathVariable("productidentifier") final String productIdentifier,
      @PathVariable("caseidentifier") final String caseIdentifier,
      @RequestBody @Valid final ImportParameters command)
  {
    actionChecks(productIdentifier, caseIdentifier, Action.IMPORT.name());

    final ProductCommandDispatcher productCommandDispatcher = caseService.getProductCommandDispatcher(productIdentifier);
    productCommandDispatcher.importCase(productIdentifier, caseIdentifier, command);

    return new ResponseEntity<>(HttpStatus.ACCEPTED);
  }

  private void actionChecks(
      final String productIdentifier,
      final String caseIdentifier,
      final String actionIdentifier) {
    checkThatCaseExists(productIdentifier, caseIdentifier);
    final Set<String> nextActions = caseService.getNextActionsForCase(productIdentifier, caseIdentifier);
    if (!nextActions.contains(actionIdentifier))
      throw ServiceException.badRequest("Action " + actionIdentifier + " cannot be taken from current state.");


    final boolean tasksOutstanding = taskInstanceService.areTasksOutstanding(
        productIdentifier, caseIdentifier, actionIdentifier);
    if (tasksOutstanding)
      throw ServiceException.conflict("Cannot execute action ''{0}'' for case ''{1}.{2}'' because tasks are incomplete.",
          actionIdentifier, productIdentifier, caseIdentifier);
  }

  private void checkThatCaseExists(final String productIdentifier, final String caseIdentifier) {
    checkThatProductExists(productIdentifier);

    if (!caseService.existsByIdentifier(productIdentifier, caseIdentifier))
      throw ServiceException.notFound("Case with identifier ''{0}.{1}'' doesn''t exist.", productIdentifier, caseIdentifier);
  }

  private void checkThatProductExists(final String productIdentifier) {
    if (!productService.existsByIdentifier(productIdentifier))
      throw ServiceException.notFound("Product with identifier ''{0}'' doesn''t exist.", productIdentifier);
  }

  //TODO: createCheck that case parameters are within product parameters in put and post.
}