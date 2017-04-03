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
import io.mifos.portfolio.api.v1.domain.ChargeDefinition;
import io.mifos.portfolio.service.internal.command.ChangeChargeDefinitionCommand;
import io.mifos.portfolio.service.internal.command.CreateChargeDefinitionCommand;
import io.mifos.portfolio.service.internal.command.DeleteProductChargeDefinitionCommand;
import io.mifos.portfolio.service.internal.service.ChargeDefinitionService;
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
@RestController
@RequestMapping("/products/{productidentifier}/charges/")
public class ChargeDefinitionRestController {
  private final CommandGateway commandGateway;
  private final ChargeDefinitionService chargeDefinitionService;
  private final ProductService productService;

  @Autowired
  public ChargeDefinitionRestController(
          final CommandGateway commandGateway,
          final ChargeDefinitionService chargeDefinitionService, final ProductService productService) {
    this.commandGateway = commandGateway;
    this.chargeDefinitionService = chargeDefinitionService;
    this.productService = productService;
  }

  @Permittable(value = AcceptedTokenType.TENANT, groupId = PermittableGroupIds.PRODUCT_MANAGEMENT)
  @RequestMapping(
          method = RequestMethod.GET,
          consumes = MediaType.ALL_VALUE,
          produces = MediaType.APPLICATION_JSON_VALUE
  )
  public @ResponseBody
  List<ChargeDefinition> getAllChargeDefinitionsForProduct(
          @PathVariable("productidentifier") final String productIdentifier)
  {
    checkProductExists(productIdentifier);

    return chargeDefinitionService.findAllEntities(productIdentifier);
  }

  @Permittable(value = AcceptedTokenType.TENANT, groupId = PermittableGroupIds.PRODUCT_MANAGEMENT)
  @RequestMapping(
          method = RequestMethod.POST,
          consumes = MediaType.APPLICATION_JSON_VALUE,
          produces = MediaType.APPLICATION_JSON_VALUE
  )
  public @ResponseBody
  ResponseEntity<Void> createChargeDefinition(
          @PathVariable("productidentifier") final String productIdentifier,
          @RequestBody @Valid final ChargeDefinition instance)
  {
    checkProductExists(productIdentifier);

    chargeDefinitionService.findByIdentifier(productIdentifier, instance.getIdentifier())
            .ifPresent(taskDefinition -> {throw ServiceException.conflict("Duplicate identifier: " + taskDefinition.getIdentifier());});

    this.commandGateway.process(new CreateChargeDefinitionCommand(productIdentifier, instance));
    return new ResponseEntity<>(HttpStatus.ACCEPTED);
  }

  @Permittable(value = AcceptedTokenType.TENANT, groupId = PermittableGroupIds.PRODUCT_MANAGEMENT)
  @RequestMapping(
          value = "{chargedefinitionidentifier}",
          method = RequestMethod.GET,
          consumes = MediaType.APPLICATION_JSON_VALUE,
          produces = MediaType.APPLICATION_JSON_VALUE
  )
  public @ResponseBody ChargeDefinition getChargeDefinition(
          @PathVariable("productidentifier") final String productIdentifier,
          @PathVariable("chargedefinitionidentifier") final String chargeDefinitionIdentifier)
  {
    checkProductExists(productIdentifier);

    return chargeDefinitionService.findByIdentifier(productIdentifier, chargeDefinitionIdentifier).orElseThrow(
            () -> ServiceException.notFound("No charge definition with the identifier '" + chargeDefinitionIdentifier  + "' found."));
  }

  @Permittable(value = AcceptedTokenType.TENANT, groupId = PermittableGroupIds.PRODUCT_MANAGEMENT)
  @RequestMapping(
          value = "{chargedefinitionidentifier}",
          method = RequestMethod.PUT,
          consumes = MediaType.APPLICATION_JSON_VALUE,
          produces = MediaType.ALL_VALUE
  )
  public ResponseEntity<Void> changeChargeDefinition(
          @PathVariable("productidentifier") final String productIdentifier,
          @PathVariable("chargedefinitionidentifier") final String chargeDefinitionIdentifier,
          @RequestBody @Valid final ChargeDefinition instance)
  {
    checkChargeExistsInProduct(productIdentifier, chargeDefinitionIdentifier);

    if (!chargeDefinitionIdentifier.equals(instance.getIdentifier()))
      throw ServiceException.badRequest("Instance identifiers may not be changed.");

    commandGateway.process(new ChangeChargeDefinitionCommand(productIdentifier, instance));

    return ResponseEntity.accepted().build();
  }

  @Permittable(value = AcceptedTokenType.TENANT, groupId = PermittableGroupIds.PRODUCT_MANAGEMENT)
  @RequestMapping(
          value = "{chargedefinitionidentifier}",
          method = RequestMethod.DELETE,
          consumes = MediaType.ALL_VALUE,
          produces = MediaType.APPLICATION_JSON_VALUE
  )
  public ResponseEntity<Void> deleteChargeDefinition(
          @PathVariable("productidentifier") final String productIdentifier,
          @PathVariable("chargedefinitionidentifier") final String chargeDefinitionIdentifier)
  {
    checkChargeExistsInProduct(productIdentifier, chargeDefinitionIdentifier);

    commandGateway.process(new DeleteProductChargeDefinitionCommand(productIdentifier, chargeDefinitionIdentifier));

    return ResponseEntity.accepted().build();
  }

  private void checkChargeExistsInProduct(final String productIdentifier,
                                          final String chargeDefinitionIdentifier) {
    chargeDefinitionService.findByIdentifier(productIdentifier, chargeDefinitionIdentifier).orElseThrow(
            () -> ServiceException.notFound("No charge definition in the product " + productIdentifier + "with the identifier '" + chargeDefinitionIdentifier  + "' found."));
  }

  private void checkProductExists(final String productIdentifier) {
    productService.findByIdentifier(productIdentifier)
            .orElseThrow(() -> ServiceException.notFound("Invalid product referenced."));
  }
}
