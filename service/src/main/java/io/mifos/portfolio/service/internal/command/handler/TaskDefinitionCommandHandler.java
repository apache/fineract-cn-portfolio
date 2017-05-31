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
package io.mifos.portfolio.service.internal.command.handler;

import io.mifos.core.command.annotation.CommandLogLevel;
import io.mifos.portfolio.api.v1.domain.TaskDefinition;
import io.mifos.portfolio.api.v1.events.EventConstants;
import io.mifos.portfolio.api.v1.events.TaskDefinitionEvent;
import io.mifos.portfolio.service.internal.command.ChangeTaskDefinitionCommand;
import io.mifos.portfolio.service.internal.command.CreateTaskDefinitionCommand;
import io.mifos.portfolio.service.internal.mapper.TaskDefinitionMapper;
import io.mifos.portfolio.service.internal.repository.ProductEntity;
import io.mifos.portfolio.service.internal.repository.ProductRepository;
import io.mifos.portfolio.service.internal.repository.TaskDefinitionEntity;
import io.mifos.portfolio.service.internal.repository.TaskDefinitionRepository;
import io.mifos.core.command.annotation.Aggregate;
import io.mifos.core.command.annotation.CommandHandler;
import io.mifos.core.command.annotation.EventEmitter;
import io.mifos.core.lang.ServiceException;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author Myrle Krantz
 */
@SuppressWarnings("unused")
@Aggregate
public class TaskDefinitionCommandHandler {
  private final ProductRepository productRepository;
  private final TaskDefinitionRepository taskDefinitionRepository;

  @Autowired
  public TaskDefinitionCommandHandler(
          final ProductRepository productRepository,
          final TaskDefinitionRepository taskDefinitionRepository) {
    this.productRepository = productRepository;
    this.taskDefinitionRepository = taskDefinitionRepository;
  }

  @CommandHandler(logStart = CommandLogLevel.INFO, logFinish = CommandLogLevel.INFO)
  @EventEmitter(selectorName = EventConstants.SELECTOR_NAME, selectorValue = EventConstants.POST_TASK_DEFINITION)
  public TaskDefinitionEvent process(final CreateTaskDefinitionCommand createTaskDefinitionCommand) {
    final TaskDefinition taskDefinition = createTaskDefinitionCommand.getInstance();
    final String productIdentifier = createTaskDefinitionCommand.getProductIdentifier();

    final ProductEntity productEntity
            = productRepository.findByIdentifier(productIdentifier)
            .orElseThrow(() -> ServiceException.badRequest("The given product identifier does not refer to a product {0}", productIdentifier));

    final TaskDefinitionEntity taskDefinitionEntity =
            TaskDefinitionMapper.map(productEntity, taskDefinition);
    taskDefinitionRepository.save(taskDefinitionEntity);

    return new TaskDefinitionEvent(
            createTaskDefinitionCommand.getProductIdentifier(),
            createTaskDefinitionCommand.getInstance().getIdentifier());
  }

  @CommandHandler(logStart = CommandLogLevel.INFO, logFinish = CommandLogLevel.INFO)
  @EventEmitter(selectorName = EventConstants.SELECTOR_NAME, selectorValue = EventConstants.PUT_TASK_DEFINITION)
  public TaskDefinitionEvent process(final ChangeTaskDefinitionCommand changeTaskDefinitionCommand) {
    final TaskDefinition taskDefinition = changeTaskDefinitionCommand.getInstance();
    final String productIdentifier = changeTaskDefinitionCommand.getProductIdentifier();

    final TaskDefinitionEntity existingTaskDefinition
            = taskDefinitionRepository.findByProductIdAndTaskIdentifier(productIdentifier, taskDefinition.getIdentifier())
            .orElseThrow(() -> ServiceException.internalError("task definition not found."));

    final TaskDefinitionEntity taskDefinitionEntity =
            TaskDefinitionMapper.map(existingTaskDefinition.getProduct(), taskDefinition);
    taskDefinitionEntity.setId(existingTaskDefinition.getId());
    taskDefinitionEntity.setId(existingTaskDefinition.getId());
    taskDefinitionRepository.save(taskDefinitionEntity);

    return new TaskDefinitionEvent(
            changeTaskDefinitionCommand.getProductIdentifier(),
            changeTaskDefinitionCommand.getInstance().getIdentifier());
  }

}
