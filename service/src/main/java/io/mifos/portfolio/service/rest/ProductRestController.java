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
import io.mifos.portfolio.api.v1.domain.AccountAssignment;
import io.mifos.portfolio.api.v1.domain.Pattern;
import io.mifos.portfolio.api.v1.domain.Product;
import io.mifos.portfolio.api.v1.domain.ProductPage;
import io.mifos.portfolio.service.internal.command.ChangeEnablingOfProductCommand;
import io.mifos.portfolio.service.internal.command.ChangeProductCommand;
import io.mifos.portfolio.service.internal.command.CreateProductCommand;
import io.mifos.portfolio.service.internal.service.CaseService;
import io.mifos.portfolio.service.internal.service.PatternService;
import io.mifos.portfolio.service.internal.service.ProductService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Nullable;
import javax.validation.Valid;
import java.util.Set;

/**
 * @author Myrle Krantz
 */
@SuppressWarnings("unused")
@RestController //
@RequestMapping("/products") //
public class ProductRestController {

  private final CommandGateway commandGateway;
  private final CaseService caseService;
  private final ProductService productService;
  private final PatternService patternService;

  @Autowired public ProductRestController(final CommandGateway commandGateway,
                                          final CaseService caseService,
                                          final ProductService productService,
                                          final PatternService patternService) {
    super();
    this.commandGateway = commandGateway;
    this.caseService = caseService;
    this.productService = productService;
    this.patternService = patternService;
  }

  @Permittable(value = AcceptedTokenType.TENANT, groupId = PermittableGroupIds.PRODUCT_MANAGEMENT)
  @RequestMapping(method = RequestMethod.GET) //
  public @ResponseBody
  ProductPage getProducts(@RequestParam(value = "includeDisabled", required = false) final Boolean includeDisabled,
                          @RequestParam(value = "term", required = false) final @Nullable String term,
                          @RequestParam("pageIndex") final Integer pageIndex,
                          @RequestParam("size") final Integer size) {
    return this.productService.findEntities(includeDisabled, term, pageIndex, size);
  }

  @Permittable(value = AcceptedTokenType.TENANT, groupId = PermittableGroupIds.PRODUCT_MANAGEMENT)
  @RequestMapping(method = RequestMethod.POST) //
  public @ResponseBody ResponseEntity<Void> createEntity(@RequestBody @Valid final Product instance) {
    productService.findByIdentifier(instance.getIdentifier())
            .ifPresent(product -> {throw ServiceException.conflict("Duplicate identifier: " + product.getIdentifier());});

    final Pattern pattern = patternService.findByIdentifier(instance.getPatternPackage())
            .orElseThrow(() -> ServiceException.badRequest("Invalid pattern package referenced."));

    final String user = UserContextHolder.checkedGetUser();
    if (!(instance.getCreatedBy() == null || instance.getCreatedBy().equals(user)))
      throw ServiceException.badRequest("CreatedBy must be either 'null', or the creating user upon initial creation.");

    if (!(instance.getLastModifiedBy() == null || instance.getLastModifiedBy().equals(user)))
      throw ServiceException.badRequest("LastModifiedBy must be either 'null', or the creating user upon initial creation.");

    if (instance.getCreatedOn() != null)
      throw ServiceException.badRequest("CreatedOn must be 'null' upon initial creation.");

    if (instance.getLastModifiedOn() != null)
      throw ServiceException.badRequest("LastModifiedOn must 'null' be upon initial creation.");

    this.commandGateway.process(new CreateProductCommand(instance));
    return new ResponseEntity<>(HttpStatus.ACCEPTED);
  }

  @Permittable(value = AcceptedTokenType.TENANT, groupId = PermittableGroupIds.PRODUCT_MANAGEMENT)
  @RequestMapping(
          value = "/{productidentifier}",
          method = RequestMethod.GET,
          consumes = MediaType.ALL_VALUE,
          produces = MediaType.APPLICATION_JSON_VALUE)
  public  @ResponseBody ResponseEntity<Product> getProduct(@PathVariable("productidentifier") final String productIdentifier)
  {
    return productService.findByIdentifier(productIdentifier)
            .map(ResponseEntity::ok)
            .orElseThrow(() -> ServiceException.notFound("Instance with identifier " + productIdentifier + " doesn't exist."));
  }

  @Permittable(value = AcceptedTokenType.TENANT, groupId = PermittableGroupIds.PRODUCT_MANAGEMENT)
  @RequestMapping(
          value = "/{productidentifier}",
          method = RequestMethod.PUT,
          consumes = MediaType.ALL_VALUE,
          produces = MediaType.APPLICATION_JSON_VALUE)
  public @ResponseBody ResponseEntity<Void> changeProduct(@PathVariable("productidentifier") final String productIdentifier,
                                                          @RequestBody @Valid final Product instance)
  {
    productService.findByIdentifier(productIdentifier)
            .orElseThrow(() -> ServiceException.notFound("Instance with identifier " + productIdentifier + " doesn't exist."));

    if (!productIdentifier.equals(instance.getIdentifier()))
      throw ServiceException.badRequest("Instance identifier may not be changed. Identifier provided in instance = " + instance.getIdentifier() + ". Instance referenced in path = " + productIdentifier + ".");

    if (caseService.existsByProductIdentifier(productIdentifier))
      throw ServiceException.conflict("Cases exist for product with the identifier '" + productIdentifier + "'. Product cannot be changed.");

    commandGateway.process(new ChangeProductCommand(instance));

    return ResponseEntity.accepted().build();
  }

  @Permittable(value = AcceptedTokenType.TENANT, groupId = PermittableGroupIds.PRODUCT_OPERATIONS_MANAGEMENT)
  @RequestMapping(
          value = "/{productidentifier}/incompleteaccountassignments",
          method = RequestMethod.GET,
          consumes = MediaType.APPLICATION_JSON_VALUE,
          produces = MediaType.APPLICATION_JSON_VALUE)
  public @ResponseBody ResponseEntity<Set<AccountAssignment>> getIncompleteAccountAssignments(@PathVariable("productidentifier") final String productIdentifier)
  {
    productService.findByIdentifier(productIdentifier)
            .orElseThrow(() -> ServiceException.notFound("Instance with identifier " + productIdentifier + " doesn't exist."));

    return ResponseEntity.ok(productService.getIncompleteAccountAssignments(productIdentifier));
  }

  @Permittable(value = AcceptedTokenType.TENANT, groupId = PermittableGroupIds.PRODUCT_OPERATIONS_MANAGEMENT)
  @RequestMapping(
          value = "/{productidentifier}/enabled",
          method = RequestMethod.PUT,
          consumes = MediaType.APPLICATION_JSON_VALUE,
          produces = MediaType.APPLICATION_JSON_VALUE)
  public @ResponseBody ResponseEntity<Void> enableProduct(
          @PathVariable("productidentifier") final String productIdentifier,
          @RequestBody final Boolean enabled)
  {
    productService.findByIdentifier(productIdentifier)
            .orElseThrow(() -> ServiceException.notFound("Instance with identifier " + productIdentifier + " doesn't exist."));

    if (enabled) {
      if (!productService.areChargeDefinitionsCoveredByAccountAssignments(productIdentifier))
        throw ServiceException.conflict("Product with identifier " + productIdentifier + " is not ready to be enabled.");
    }

    commandGateway.process(new ChangeEnablingOfProductCommand(productIdentifier, enabled));

    return ResponseEntity.accepted().build();
  }

  @Permittable(value = AcceptedTokenType.TENANT, groupId = PermittableGroupIds.PRODUCT_MANAGEMENT)
  @Permittable(value = AcceptedTokenType.TENANT, groupId = PermittableGroupIds.PRODUCT_OPERATIONS_MANAGEMENT)
  @RequestMapping(
          value = "/{productidentifier}/enabled",
          method = RequestMethod.GET,
          consumes = MediaType.APPLICATION_JSON_VALUE,
          produces = MediaType.APPLICATION_JSON_VALUE)
  public  @ResponseBody ResponseEntity<Boolean> getProductEnabled(@PathVariable("productidentifier") final String productIdentifier)
  {
    return ResponseEntity.ok(productService.findEnabledByIdentifier(productIdentifier)
            .orElseThrow(() -> ServiceException.notFound("Instance with identifier " + productIdentifier + " doesn't exist.")));

  }
}
