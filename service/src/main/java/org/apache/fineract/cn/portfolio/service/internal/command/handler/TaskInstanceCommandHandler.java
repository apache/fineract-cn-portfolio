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
package org.apache.fineract.cn.portfolio.service.internal.command.handler;

import org.apache.fineract.cn.portfolio.api.v1.domain.TaskInstance;
import org.apache.fineract.cn.portfolio.api.v1.events.EventConstants;
import org.apache.fineract.cn.portfolio.api.v1.events.TaskInstanceEvent;
import org.apache.fineract.cn.portfolio.service.internal.command.ChangeTaskInstanceCommand;
import org.apache.fineract.cn.portfolio.service.internal.command.ExecuteTaskInstanceCommand;
import org.apache.fineract.cn.portfolio.service.internal.mapper.TaskInstanceMapper;
import org.apache.fineract.cn.portfolio.service.internal.repository.TaskInstanceEntity;
import org.apache.fineract.cn.portfolio.service.internal.repository.TaskInstanceRepository;
import java.time.Clock;
import java.time.LocalDateTime;
import org.apache.fineract.cn.api.util.UserContextHolder;
import org.apache.fineract.cn.command.annotation.Aggregate;
import org.apache.fineract.cn.command.annotation.CommandHandler;
import org.apache.fineract.cn.command.annotation.CommandLogLevel;
import org.apache.fineract.cn.command.annotation.EventEmitter;
import org.apache.fineract.cn.lang.ServiceException;
import org.springframework.beans.factory.annotation.Autowired;

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
