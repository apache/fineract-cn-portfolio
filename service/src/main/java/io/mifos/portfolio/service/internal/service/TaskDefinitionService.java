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
package io.mifos.portfolio.service.internal.service;

import io.mifos.portfolio.api.v1.domain.TaskDefinition;
import io.mifos.individuallending.api.v1.domain.workflow.Action;
import io.mifos.portfolio.service.internal.mapper.TaskDefinitionMapper;
import io.mifos.portfolio.service.internal.repository.TaskDefinitionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.AbstractMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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
    return taskDefinitionRepository.findByProductId(productIdentifier).stream()
            .map(TaskDefinitionMapper::map)
            .collect(Collectors.toList());
  }

  public Map<Action, List<TaskDefinition>> getTaskDefinitionsMappedByAction(
          final String productIdentifier)
  {
    final List<TaskDefinition> taskDefinitions = findAllEntities(productIdentifier);

    return taskDefinitions.stream().flatMap(this::createMappingsForTaskDefinition)
                    .collect(Collectors.groupingBy(Map.Entry::getKey,
                            Collectors.mapping(Map.Entry::getValue, Collectors.toList())));
  }

  private Stream<AbstractMap.SimpleEntry<Action, TaskDefinition>> createMappingsForTaskDefinition(
          final TaskDefinition taskDefinition)
  {
    return taskDefinition.getActions().stream().map(x -> new AbstractMap.SimpleEntry<>(Action.valueOf(x), taskDefinition));
  }

  public Optional<TaskDefinition> findByIdentifier(final String productIdentifier, final String identifier) {
    return taskDefinitionRepository
            .findByProductIdAndTaskIdentifier(productIdentifier, identifier)
            .map(TaskDefinitionMapper::map);
  }
}