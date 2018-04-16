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

import org.apache.fineract.cn.portfolio.api.v1.PermittableGroupIds;
import org.apache.fineract.cn.portfolio.api.v1.domain.TaskDefinition;
import org.apache.fineract.cn.portfolio.service.internal.command.ChangeTaskDefinitionCommand;
import org.apache.fineract.cn.portfolio.service.internal.command.CreateTaskDefinitionCommand;
import org.apache.fineract.cn.portfolio.service.internal.command.DeleteTaskDefinitionCommand;
import org.apache.fineract.cn.portfolio.service.internal.service.CaseService;
import org.apache.fineract.cn.portfolio.service.internal.service.ProductService;
import org.apache.fineract.cn.portfolio.service.internal.service.TaskDefinitionService;
import java.util.List;
import javax.validation.Valid;
import org.apache.fineract.cn.anubis.annotation.AcceptedTokenType;
import org.apache.fineract.cn.anubis.annotation.Permittable;
import org.apache.fineract.cn.command.gateway.CommandGateway;
import org.apache.fineract.cn.lang.ServiceException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

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
  private final CaseService caseService;

  @Autowired
  public TaskDefinitionRestController(
      final CommandGateway commandGateway,
      final TaskDefinitionService taskDefinitionService,
      final ProductService productService,
      final CaseService caseService)
  {
    super();
    this.commandGateway = commandGateway;
    this.taskDefinitionService = taskDefinitionService;
    this.productService = productService;
    this.caseService = caseService;
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
      consumes = MediaType.ALL_VALUE,
      produces = MediaType.APPLICATION_JSON_VALUE
  )
  public @ResponseBody
  ResponseEntity<Void> createTaskDefinition(
          @PathVariable("productidentifier") final String productIdentifier,
          @RequestBody @Valid final TaskDefinition instance)
  {
    checkProductExists(productIdentifier);

    checkProductChangeable(productIdentifier);

    taskDefinitionService.findByIdentifier(productIdentifier, instance.getIdentifier())
            .ifPresent(taskDefinition -> {throw ServiceException.conflict("Duplicate identifier: " + taskDefinition.getIdentifier());});

    this.commandGateway.process(new CreateTaskDefinitionCommand(productIdentifier, instance));
    return new ResponseEntity<>(HttpStatus.ACCEPTED);
  }

  @Permittable(value = AcceptedTokenType.TENANT, groupId = PermittableGroupIds.PRODUCT_MANAGEMENT)
  @RequestMapping(
          value = "{taskidentifier}",
          method = RequestMethod.GET,
          consumes = MediaType.APPLICATION_JSON_VALUE,
          produces = MediaType.APPLICATION_JSON_VALUE
  )
  public @ResponseBody TaskDefinition getTaskDefinition(
          @PathVariable("productidentifier") final String productIdentifier,
          @PathVariable("taskidentifier") final String taskDefinitionIdentifier)
  {
    checkProductExists(productIdentifier);

    return taskDefinitionService.findByIdentifier(productIdentifier, taskDefinitionIdentifier).orElseThrow(
            () -> ServiceException.notFound("No task definition with the identifier '" + taskDefinitionIdentifier  + "' found."));
  }

  @Permittable(value = AcceptedTokenType.TENANT, groupId = PermittableGroupIds.PRODUCT_MANAGEMENT)
  @RequestMapping(
      value = "{taskidentifier}",
      method = RequestMethod.PUT,
      consumes = MediaType.ALL_VALUE,
      produces = MediaType.APPLICATION_JSON_VALUE
  )
  public @ResponseBody ResponseEntity<Void> changeTaskDefinition(
          @PathVariable("productidentifier") final String productIdentifier,
          @PathVariable("taskidentifier") final String taskDefinitionIdentifier,
          @RequestBody @Valid final TaskDefinition instance)
  {
    checkTaskDefinitionExists(productIdentifier, taskDefinitionIdentifier);

    checkProductChangeable(productIdentifier);

    if (!taskDefinitionIdentifier.equals(instance.getIdentifier()))
      throw ServiceException.badRequest("Instance identifiers may not be changed.");

    commandGateway.process(new ChangeTaskDefinitionCommand(productIdentifier, instance));

    return ResponseEntity.accepted().build();
  }

  @Permittable(value = AcceptedTokenType.TENANT, groupId = PermittableGroupIds.PRODUCT_MANAGEMENT)
  @RequestMapping(
      value = "/{taskidentifier}",
      method = RequestMethod.DELETE,
      consumes = MediaType.ALL_VALUE,
      produces = MediaType.APPLICATION_JSON_VALUE
  )
  public @ResponseBody ResponseEntity<Void> deleteTaskDefinition(
      @PathVariable("productidentifier") final String productIdentifier,
      @PathVariable("taskidentifier") final String taskDefinitionIdentifier
  )
  {
    checkTaskDefinitionExists(productIdentifier, taskDefinitionIdentifier);

    checkProductChangeable(productIdentifier);

    commandGateway.process(new DeleteTaskDefinitionCommand(productIdentifier, taskDefinitionIdentifier));

    return ResponseEntity.accepted().build();
  }

  private void checkTaskDefinitionExists(final String productIdentifier,
                                         final String taskDefinitionIdentifier) {
    taskDefinitionService.findByIdentifier(productIdentifier, taskDefinitionIdentifier)
        .orElseThrow(() -> ServiceException.notFound("No task with the identifier ''{0}.{1}'' exists.", productIdentifier, taskDefinitionIdentifier));
  }

  private void checkProductExists(final String productIdentifier) {
    productService.findByIdentifier(productIdentifier)
            .orElseThrow(() -> ServiceException.notFound("Invalid product referenced."));
  }

  private void checkProductChangeable(final String productIdentifier) {
    if (caseService.existsByProductIdentifier(productIdentifier))
      throw ServiceException.conflict("Cases exist for product with the identifier ''{0}''. Product cannot be changed.", productIdentifier);
  }
}