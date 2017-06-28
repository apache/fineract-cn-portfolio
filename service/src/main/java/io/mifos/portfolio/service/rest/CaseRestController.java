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
package io.mifos.portfolio.service.rest;

import io.mifos.anubis.annotation.AcceptedTokenType;
import io.mifos.anubis.annotation.Permittable;
import io.mifos.core.api.util.UserContextHolder;
import io.mifos.core.command.gateway.CommandGateway;
import io.mifos.core.lang.ServiceException;
import io.mifos.portfolio.api.v1.PermittableGroupIds;
import io.mifos.portfolio.api.v1.domain.Case;
import io.mifos.portfolio.api.v1.domain.CasePage;
import io.mifos.portfolio.api.v1.domain.Command;
import io.mifos.portfolio.api.v1.domain.CostComponent;
import io.mifos.portfolio.service.internal.command.ChangeCaseCommand;
import io.mifos.portfolio.service.internal.command.CreateCaseCommand;
import io.mifos.portfolio.service.internal.service.CaseService;
import io.mifos.portfolio.service.internal.service.ProductService;
import io.mifos.products.spi.ProductCommandDispatcher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * @author Myrle Krantz
 */
@SuppressWarnings("unused")
@RestController
@RequestMapping("/products/{productidentifier}/cases/")
public class CaseRestController {

  private final CommandGateway commandGateway;
  private final CaseService caseService;
  private final ProductService productService;

  @Autowired public CaseRestController(
          final CommandGateway commandGateway,
          final CaseService caseService,
          final ProductService productService) {
    super();
    this.commandGateway = commandGateway;
    this.caseService = caseService;
    this.productService = productService;
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

    caseService.findByIdentifier(productIdentifier, instance.getIdentifier())
            .ifPresent(x -> {throw ServiceException.conflict("Duplicate identifier: " + productIdentifier + "." + x.getIdentifier());});

    final Optional<Boolean> productEnabled = productService.findEnabledByIdentifier(productIdentifier);
    productEnabled.orElseThrow(() -> ServiceException.internalError("Product should exist, but doesn't"));
    productEnabled.ifPresent(x -> {
      if (!x) throw ServiceException.badRequest("Product must be enabled before cases for it can be created: " + productIdentifier);});

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
                    "Instance with identifier " + productIdentifier + "." + caseIdentifier + " doesn't exist."));
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

    this.commandGateway.process(new ChangeCaseCommand(instance));
    return new ResponseEntity<>(HttpStatus.ACCEPTED);
    //TODO: Make sure case can't be changed from certain states.
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
  List<CostComponent> getCostComponentsForAction(@PathVariable("productidentifier") final String productIdentifier,
                                                 @PathVariable("caseidentifier") final String caseIdentifier,
                                                 @PathVariable("actionidentifier") final String actionIdentifier)
  {
    checkThatCaseExists(productIdentifier, caseIdentifier);

    return caseService.getActionCostComponentsForCase(productIdentifier, caseIdentifier, actionIdentifier);
  }

  @Permittable(value = AcceptedTokenType.TENANT, groupId = PermittableGroupIds.CASE_MANAGEMENT)
  @RequestMapping(
          value = "{caseidentifier}/commands/{actionidentifier}",
          method = RequestMethod.POST,
          produces = MediaType.APPLICATION_JSON_VALUE,
          consumes = MediaType.APPLICATION_JSON_VALUE
  )
  public @ResponseBody ResponseEntity<Void> executeCaseCommand(@PathVariable("productidentifier") final String productIdentifier,
                                                               @PathVariable("caseidentifier") final String caseIdentifier,
                                                               @PathVariable("actionidentifier") final String actionIdentifier,
                                                               @RequestBody @Valid final Command command)
  {
    checkThatCaseExists(productIdentifier, caseIdentifier);
    final Set<String> nextActions = caseService.getNextActionsForCase(productIdentifier, caseIdentifier);
    if (!nextActions.contains(actionIdentifier))
      throw ServiceException.badRequest("Action " + actionIdentifier + " cannot be taken from current state.");

    final ProductCommandDispatcher productCommandDispatcher = caseService.getProductCommandDispatcher(productIdentifier);
    productCommandDispatcher.dispatch(productIdentifier, caseIdentifier, actionIdentifier, command);

    return new ResponseEntity<>(HttpStatus.ACCEPTED);
  }

  private Case checkThatCaseExists(final String productIdentifier, final String caseIdentifier) {
    checkThatProductExists(productIdentifier);

    return caseService.findByIdentifier(productIdentifier, caseIdentifier)
            .orElseThrow(() -> ServiceException.notFound("Case with identifier " + productIdentifier + "." + caseIdentifier + " doesn't exist."));
  }

  private void checkThatProductExists(final String productIdentifier) {
    productService.findByIdentifier(productIdentifier)
            .orElseThrow(() -> ServiceException.notFound("Product with identifier " + productIdentifier + " doesn't exist."));
  }

  //TODO: check that case parameters are within product parameters in put and post.
}