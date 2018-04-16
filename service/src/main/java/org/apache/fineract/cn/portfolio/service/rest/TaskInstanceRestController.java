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
import org.apache.fineract.cn.portfolio.api.v1.domain.Case;
import org.apache.fineract.cn.portfolio.api.v1.domain.TaskDefinition;
import org.apache.fineract.cn.portfolio.api.v1.domain.TaskInstance;
import org.apache.fineract.cn.portfolio.service.internal.command.ChangeTaskInstanceCommand;
import org.apache.fineract.cn.portfolio.service.internal.command.ExecuteTaskInstanceCommand;
import org.apache.fineract.cn.portfolio.service.internal.service.CaseService;
import org.apache.fineract.cn.portfolio.service.internal.service.TaskDefinitionService;
import org.apache.fineract.cn.portfolio.service.internal.service.TaskInstanceService;
import java.util.List;
import javax.validation.Valid;
import org.apache.fineract.cn.anubis.annotation.AcceptedTokenType;
import org.apache.fineract.cn.anubis.annotation.Permittable;
import org.apache.fineract.cn.api.util.UserContextHolder;
import org.apache.fineract.cn.command.gateway.CommandGateway;
import org.apache.fineract.cn.lang.ServiceException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
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
@RequestMapping("/products/{productidentifier}/cases/{caseidentifier}/tasks/")
public class TaskInstanceRestController {
  private final CommandGateway commandGateway;
  private final TaskDefinitionService taskDefinitionService;
  private final TaskInstanceService taskInstanceService;
  private final CaseService caseService;

  @Autowired
  public TaskInstanceRestController(
      final CommandGateway commandGateway,
      final TaskDefinitionService taskDefinitionService,
      final TaskInstanceService taskInstanceService,
      final CaseService caseService)
  {
    super();
    this.commandGateway = commandGateway;
    this.taskDefinitionService = taskDefinitionService;
    this.taskInstanceService = taskInstanceService;
    this.caseService = caseService;
  }

  @Permittable(value = AcceptedTokenType.TENANT, groupId = PermittableGroupIds.CASE_MANAGEMENT)
  @RequestMapping(
      method = RequestMethod.GET,
      consumes = MediaType.ALL_VALUE,
      produces = MediaType.APPLICATION_JSON_VALUE
  )
  public @ResponseBody
  List<TaskInstance> getAllTasksForCase(@PathVariable("productidentifier") final String productIdentifier,
                                        @PathVariable("caseidentifier") final String caseIdentifier,
                                        @RequestParam(value = "includeExecuted", required = false, defaultValue = "true") final Boolean includeExecuted)
  {
    checkedGetCase(productIdentifier, caseIdentifier);

    return taskInstanceService.findAllEntities(productIdentifier, caseIdentifier, includeExecuted);
  }

  @Permittable(value = AcceptedTokenType.TENANT, groupId = PermittableGroupIds.CASE_MANAGEMENT)
  @RequestMapping(
      value = "{taskidentifier}",
      method = RequestMethod.GET,
      consumes = MediaType.APPLICATION_JSON_VALUE,
      produces = MediaType.APPLICATION_JSON_VALUE
  )
  public @ResponseBody
  TaskInstance getTaskForCase(@PathVariable("productidentifier") final String productIdentifier,
                              @PathVariable("caseidentifier") final String caseIdentifier,
                              @PathVariable("taskidentifier") final String taskIdentifier)
  {
    checkedGetCase(productIdentifier, caseIdentifier);

    return taskInstanceService.findByIdentifier(productIdentifier, caseIdentifier, taskIdentifier).orElseThrow(
        () -> ServiceException.notFound("No task instance ''{0}.{1}.{2}'' found.", productIdentifier, caseIdentifier, taskIdentifier));
  }

  @Permittable(value = AcceptedTokenType.TENANT, groupId = PermittableGroupIds.CASE_MANAGEMENT)
  @RequestMapping(
      value = "{taskidentifier}",
      method = RequestMethod.PUT,
      consumes = MediaType.ALL_VALUE,
      produces = MediaType.APPLICATION_JSON_VALUE
  )
  public @ResponseBody
  ResponseEntity<Void> changeTaskForCase(@PathVariable("productidentifier") final String productIdentifier,
                                         @PathVariable("caseidentifier") final String caseIdentifier,
                                         @PathVariable("taskidentifier") final String taskIdentifier,
                                         @RequestBody @Valid final TaskInstance instance)
  {
    checkedGetCase(productIdentifier, caseIdentifier);

    checkedGetTaskDefinition(productIdentifier, taskIdentifier);

    if (!taskIdentifier.equals(instance.getTaskIdentifier()))
      throw ServiceException.badRequest("Instance identifiers may not be changed.");

    commandGateway.process(new ChangeTaskInstanceCommand(productIdentifier, caseIdentifier, instance));

    return ResponseEntity.accepted().build();
  }

  @Permittable(value = AcceptedTokenType.TENANT, groupId = PermittableGroupIds.CASE_MANAGEMENT)
  @RequestMapping(
      value = "{taskidentifier}/executed",
      method = RequestMethod.PUT,
      consumes = MediaType.ALL_VALUE,
      produces = MediaType.APPLICATION_JSON_VALUE
  )
  public @ResponseBody
  ResponseEntity<Void> markTaskExecution(@PathVariable("productidentifier") final String productIdentifier,
                                         @PathVariable("caseidentifier") final String caseIdentifier,
                                         @PathVariable("taskidentifier") final String taskIdentifier,
                                         @RequestBody @Valid final Boolean executed)
  {
    final TaskDefinition taskDefinition = checkedGetTaskDefinition(productIdentifier, taskIdentifier);
    if (taskDefinition.getFourEyes()) {
      final Case customerCase = checkedGetCase(productIdentifier, caseIdentifier);
      if (UserContextHolder.checkedGetUser().equals(customerCase.getCreatedBy()))
        throw ServiceException.conflict("Signing user must be different than case creator.");
    }

    commandGateway.process(new ExecuteTaskInstanceCommand(productIdentifier, caseIdentifier, taskIdentifier, executed));

    return ResponseEntity.accepted().build();
  }

  private TaskDefinition checkedGetTaskDefinition(final String productIdentifier,
                                                  final String taskDefinitionIdentifier) throws ServiceException {
    return taskDefinitionService.findByIdentifier(productIdentifier, taskDefinitionIdentifier)
        .orElseThrow(() -> ServiceException
            .notFound("No task with the identifier ''{0}.{1}'' exists.", productIdentifier, taskDefinitionIdentifier));
  }

  private Case checkedGetCase(final String productIdentifier, final String caseIdentifier) throws ServiceException {
    return caseService.findByIdentifier(productIdentifier, caseIdentifier)
        .orElseThrow(() -> ServiceException.notFound("Case ''{0}.{1}'' does not exist.", productIdentifier, caseIdentifier));
  }
}
