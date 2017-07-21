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

import io.mifos.core.api.util.UserContextHolder;
import io.mifos.core.command.annotation.Aggregate;
import io.mifos.core.command.annotation.CommandHandler;
import io.mifos.core.command.annotation.CommandLogLevel;
import io.mifos.core.command.annotation.EventEmitter;
import io.mifos.core.lang.ServiceException;
import io.mifos.portfolio.api.v1.domain.TaskInstance;
import io.mifos.portfolio.api.v1.events.EventConstants;
import io.mifos.portfolio.api.v1.events.TaskInstanceEvent;
import io.mifos.portfolio.service.internal.command.ChangeTaskInstanceCommand;
import io.mifos.portfolio.service.internal.command.ExecuteTaskInstanceCommand;
import io.mifos.portfolio.service.internal.mapper.TaskInstanceMapper;
import io.mifos.portfolio.service.internal.repository.TaskInstanceEntity;
import io.mifos.portfolio.service.internal.repository.TaskInstanceRepository;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.Clock;
import java.time.LocalDateTime;

/**
 * @author Myrle Krantz
 */
@SuppressWarnings("unused")
@Aggregate
public class TaskInstanceCommandHandler {
  private final TaskInstanceRepository taskInstanceRepository;

  @Autowired
  public TaskInstanceCommandHandler(TaskInstanceRepository taskInstanceRepository) {
    this.taskInstanceRepository = taskInstanceRepository;
  }

  @CommandHandler(logStart = CommandLogLevel.INFO, logFinish = CommandLogLevel.INFO)
  @EventEmitter(selectorName = EventConstants.SELECTOR_NAME, selectorValue = EventConstants.PUT_TASK_INSTANCE)
  public TaskInstanceEvent process(final ChangeTaskInstanceCommand changeTaskInstanceCommand) {
    final String productIdentifier = changeTaskInstanceCommand.getProductIdentifier();
    final String caseIdentifier = changeTaskInstanceCommand.getCaseIdentifier();
    final TaskInstance taskInstance = changeTaskInstanceCommand.getInstance();

    final TaskInstanceEntity existingTaskInstance
        = taskInstanceRepository.findByProductIdAndCaseIdAndTaskId(productIdentifier, caseIdentifier, taskInstance.getTaskIdentifier())
        .orElseThrow(() -> ServiceException.notFound("Task instance ''{0}.{1}.{2}'' not found.",
            productIdentifier, caseIdentifier, taskInstance.getTaskIdentifier()));

    final TaskInstanceEntity taskInstanceEntity =
        TaskInstanceMapper.mapOverOldEntity(taskInstance, existingTaskInstance);
    taskInstanceRepository.save(taskInstanceEntity);

    return new TaskInstanceEvent(
        changeTaskInstanceCommand.getProductIdentifier(),
        changeTaskInstanceCommand.getCaseIdentifier(),
        changeTaskInstanceCommand.getInstance().getTaskIdentifier());
  }

  @CommandHandler(logStart = CommandLogLevel.INFO, logFinish = CommandLogLevel.INFO)
  @EventEmitter(selectorName = EventConstants.SELECTOR_NAME, selectorValue = EventConstants.PUT_TASK_INSTANCE_EXECUTION)
  public TaskInstanceEvent process(final ExecuteTaskInstanceCommand changeTaskInstanceExecutionCommand) {
    final String productIdentifier = changeTaskInstanceExecutionCommand.getProductIdentifier();
    final String caseIdentifier = changeTaskInstanceExecutionCommand.getCaseIdentifier();
    final String taskIdentifier = changeTaskInstanceExecutionCommand.getTaskIdentifier();
    final boolean executed = changeTaskInstanceExecutionCommand.getExecuted();

    final TaskInstanceEntity taskInstanceEntity
        = taskInstanceRepository.findByProductIdAndCaseIdAndTaskId(productIdentifier, caseIdentifier, taskIdentifier)
        .orElseThrow(() -> ServiceException.notFound("Task instance ''{0}.{1}.{2}'' not found.",
            productIdentifier, caseIdentifier, taskIdentifier));

    if (executed) {
      taskInstanceEntity.setExecutedOn(LocalDateTime.now(Clock.systemUTC()));
      taskInstanceEntity.setExecutedBy(UserContextHolder.checkedGetUser());
    }
    else {
      taskInstanceEntity.setExecutedOn(null);
      taskInstanceEntity.setExecutedBy(null);
    }

    taskInstanceRepository.save(taskInstanceEntity);

    return new TaskInstanceEvent(productIdentifier, caseIdentifier, taskIdentifier);
  }
}
