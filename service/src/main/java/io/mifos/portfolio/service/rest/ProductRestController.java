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
import io.mifos.portfolio.api.v1.PermittableGroupIds;
import io.mifos.portfolio.api.v1.domain.Pattern;
import io.mifos.portfolio.api.v1.domain.Product;
import io.mifos.portfolio.service.internal.command.ChangeEnablingOfProductCommand;
import io.mifos.portfolio.service.internal.command.CreateProductCommand;
import io.mifos.portfolio.service.internal.service.PatternService;
import io.mifos.portfolio.service.internal.service.ProductService;
import io.mifos.core.command.gateway.CommandGateway;
import io.mifos.core.lang.ServiceException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;

/**
 * @author Myrle Krantz
 */
@SuppressWarnings("unused")
@RestController //
@RequestMapping("/products") //
public class ProductRestController {

  private final CommandGateway commandGateway;
  private final ProductService productService;
  private final PatternService patternService;

  @Autowired public ProductRestController(final CommandGateway commandGateway,
      final ProductService productService, final PatternService patternService) {
    super();
    this.commandGateway = commandGateway;
    this.productService = productService;
    this.patternService = patternService;
  }

  @Permittable(value = AcceptedTokenType.TENANT, groupId = PermittableGroupIds.PRODUCT_MANAGEMENT)
  @RequestMapping(method = RequestMethod.GET) //
  public @ResponseBody List<Product> findAllEntities() {
    return this.productService.findAllEntities();
  }

  @Permittable(AcceptedTokenType.TENANT)
  @RequestMapping(method = RequestMethod.POST) //
  public @ResponseBody ResponseEntity<Void> createEntity(@RequestBody @Valid final Product instance) {
    productService.findByIdentifier(instance.getIdentifier())
            .ifPresent(product -> {throw ServiceException.conflict("Duplicate identifier: " + product.getIdentifier());});

    final Pattern pattern = patternService.findByIdentifier(instance.getPatternPackage())
            .orElseThrow(() -> ServiceException.badRequest("Invalid pattern package referenced."));

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

    if (!productService.isProductReadyToBeEnabled(productIdentifier))
      throw ServiceException.conflict("Product with identifier " + productIdentifier + " is not ready to be enabled.");

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
