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
package org.apache.fineract.cn.portfolio.service.internal.service;

import org.apache.fineract.cn.portfolio.api.v1.domain.TaskInstance;
import org.apache.fineract.cn.portfolio.service.internal.mapper.TaskInstanceMapper;
import org.apache.fineract.cn.portfolio.service.internal.repository.TaskInstanceEntity;
import org.apache.fineract.cn.portfolio.service.internal.repository.TaskInstanceRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @author Myrle Krantz
 */
@Service
public class TaskInstanceService {
  private final TaskInstanceRepository taskInstanceRepository;

  @Autowired
  public TaskInstanceService(final TaskInstanceRepository taskInstanceRepository) {
    this.taskInstanceRepository = taskInstanceRepository;
  }

  public List<TaskInstance> findAllEntities(final String productIdentifier,
                                            final String caseIdentifier,
                                            final Boolean includeExecuted) {
    final Stream<TaskInstanceEntity> ret;
    if (includeExecuted)
      ret = taskInstanceRepository.findByProductIdAndCaseId(productIdentifier, caseIdentifier);
    else {
      ret = taskInstanceRepository.findByProductIdAndCaseIdAndExcludeExecuted(productIdentifier, caseIdentifier);
    }

    return ret.map(TaskInstanceMapper::map)
        .collect(Collectors.toList());
  }

  public Optional<TaskInstance> findByIdentifier(final String productIdentifier,
                                                 final String caseIdentifier,
                                                 final String taskIdentifier) {
    return taskInstanceRepository.findByProductIdAndCaseIdAndTaskId(productIdentifier, caseIdentifier, taskIdentifier)
        .map(TaskInstanceMapper::map);
  }

  public boolean areTasksOutstanding(final String productIdentifier,
                                     final String caseIdentifier,
                                     final String actionIdentifier) {
    return taskInstanceRepository.areTasksOutstanding(productIdentifier, caseIdentifier, actionIdentifier);
  }
}