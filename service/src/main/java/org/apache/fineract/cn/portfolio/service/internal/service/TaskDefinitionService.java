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

import org.apache.fineract.cn.portfolio.api.v1.domain.TaskDefinition;
import org.apache.fineract.cn.portfolio.service.internal.mapper.TaskDefinitionMapper;
import org.apache.fineract.cn.portfolio.service.internal.repository.TaskDefinitionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * @author Myrle Krantz
 */
@Service
public class TaskDefinitionService {
  private final TaskDefinitionRepository taskDefinitionRepository;

  @Autowired
  public TaskDefinitionService(
          final TaskDefinitionRepository taskDefinitionRepository) {
    this.taskDefinitionRepository = taskDefinitionRepository;
  }

  public List<TaskDefinition> findAllEntities(final String productIdentifier) {
    return taskDefinitionRepository.findByProductId(productIdentifier)
            .map(TaskDefinitionMapper::map)
            .collect(Collectors.toList());
  }

  public Optional<TaskDefinition> findByIdentifier(final String productIdentifier, final String identifier) {
    return taskDefinitionRepository
            .findByProductIdAndTaskIdentifier(productIdentifier, identifier)
            .map(TaskDefinitionMapper::map);
  }
}