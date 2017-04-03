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
import io.mifos.portfolio.api.v1.domain.TaskDefinition;
import io.mifos.portfolio.service.internal.command.ChangeTaskDefinitionCommand;
import io.mifos.portfolio.service.internal.command.CreateTaskDefinitionCommand;
import io.mifos.portfolio.service.internal.service.ProductService;
import io.mifos.portfolio.service.internal.service.TaskDefinitionService;
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
@RequestMapping("/products/{productidentifier}/tasks/")
public class TaskDefinitionRestController {
  private final CommandGateway commandGateway;
  private final TaskDefinitionService taskDefinitionService;
  private final ProductService productService;

  @Autowired
  public TaskDefinitionRestController(
          final CommandGateway commandGateway,
          final TaskDefinitionService taskDefinitionService,
          final ProductService productService)
  {
    super();
    this.commandGateway = commandGateway;
    this.taskDefinitionService = taskDefinitionService;
    this.productService = productService;
  }

  @Permittable(value = AcceptedTokenType.TENANT, groupId = PermittableGroupIds.PRODUCT_MANAGEMENT)
  @RequestMapping(
          method = RequestMethod.GET,
          consumes = MediaType.ALL_VALUE,
          produces = MediaType.APPLICATION_JSON_VALUE
  )
  public @ResponseBody List<TaskDefinition> getAllTaskDefinitionsForProduct(
          @PathVariable("productidentifier") final String productIdentifier)
  {
    checkProductExists(productIdentifier);

    return taskDefinitionService.findAllEntities(productIdentifier);
  }

  @Permittable(value = AcceptedTokenType.TENANT, groupId = PermittableGroupIds.PRODUCT_MANAGEMENT)
  @RequestMapping(
          method = RequestMethod.POST,
          consumes = MediaType.APPLICATION_JSON_VALUE,
          produces = MediaType.APPLICATION_JSON_VALUE
  )
  public @ResponseBody
  ResponseEntity<Void> createTaskDefinition(
          @PathVariable("productidentifier") final String productIdentifier,
          @RequestBody @Valid final TaskDefinition instance)
  {
    checkProductExists(productIdentifier);

    taskDefinitionService.findByIdentifier(productIdentifier, instance.getIdentifier())
            .ifPresent(taskDefinition -> {throw ServiceException.conflict("Duplicate identifier: " + taskDefinition.getIdentifier());});

    this.commandGateway.process(new CreateTaskDefinitionCommand(productIdentifier, instance));
    return new ResponseEntity<>(HttpStatus.ACCEPTED);
  }

  @Permittable(value = AcceptedTokenType.TENANT, groupId = PermittableGroupIds.PRODUCT_MANAGEMENT)
  @RequestMapping(
          value = "{taskdefinitionidentifier}",
          method = RequestMethod.GET,
          consumes = MediaType.APPLICATION_JSON_VALUE,
          produces = MediaType.APPLICATION_JSON_VALUE
  )
  public @ResponseBody TaskDefinition getTaskDefinition(
          @PathVariable("productidentifier") final String productIdentifier,
          @PathVariable("taskdefinitionidentifier") final String taskDefinitionIdentifier)
  {
    checkProductExists(productIdentifier);

    return taskDefinitionService.findByIdentifier(productIdentifier, taskDefinitionIdentifier).orElseThrow(
            () -> ServiceException.notFound("No task definition with the identifier '" + taskDefinitionIdentifier  + "' found."));
  }

  @Permittable(value = AcceptedTokenType.TENANT, groupId = PermittableGroupIds.PRODUCT_MANAGEMENT)
  @RequestMapping(
          value = "{taskdefinitionidentifier}",
          method = RequestMethod.PUT,
          consumes = MediaType.APPLICATION_JSON_VALUE,
          produces = MediaType.ALL_VALUE
  )
  public ResponseEntity<Void> changeTaskDefinition(
          @PathVariable("productidentifier") final String productIdentifier,
          @PathVariable("taskdefinitionidentifier") final String taskDefinitionIdentifier,
          @RequestBody @Valid final TaskDefinition instance)
  {
    checkProductExists(productIdentifier);

    if (!taskDefinitionIdentifier.equals(instance.getIdentifier()))
      throw ServiceException.badRequest("Instance identifiers may not be changed.");

    commandGateway.process(new ChangeTaskDefinitionCommand(productIdentifier, instance));

    return ResponseEntity.accepted().build();
  }

  private void checkProductExists(@PathVariable("productidentifier") String productIdentifier) {
    productService.findByIdentifier(productIdentifier)
            .orElseThrow(() -> ServiceException.notFound("Invalid product referenced."));
  }
}