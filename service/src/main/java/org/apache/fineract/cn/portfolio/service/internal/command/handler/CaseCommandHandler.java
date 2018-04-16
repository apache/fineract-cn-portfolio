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

import org.apache.fineract.cn.portfolio.api.v1.domain.Case;
import org.apache.fineract.cn.portfolio.api.v1.events.CaseEvent;
import org.apache.fineract.cn.portfolio.api.v1.events.EventConstants;
import org.apache.fineract.cn.portfolio.service.internal.command.ChangeCaseCommand;
import org.apache.fineract.cn.portfolio.service.internal.command.CreateCaseCommand;
import org.apache.fineract.cn.portfolio.service.internal.mapper.CaseMapper;
import org.apache.fineract.cn.portfolio.service.internal.pattern.PatternFactoryRegistry;
import org.apache.fineract.cn.portfolio.service.internal.repository.CaseEntity;
import org.apache.fineract.cn.portfolio.service.internal.repository.CaseRepository;
import org.apache.fineract.cn.portfolio.service.internal.repository.ProductEntity;
import org.apache.fineract.cn.portfolio.service.internal.repository.ProductRepository;
import org.apache.fineract.cn.portfolio.service.internal.repository.TaskDefinitionEntity;
import org.apache.fineract.cn.portfolio.service.internal.repository.TaskDefinitionRepository;
import org.apache.fineract.cn.portfolio.service.internal.repository.TaskInstanceEntity;
import org.apache.fineract.cn.products.spi.PatternFactory;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.fineract.cn.command.annotation.Aggregate;
import org.apache.fineract.cn.command.annotation.CommandHandler;
import org.apache.fineract.cn.command.annotation.CommandLogLevel;
import org.apache.fineract.cn.command.annotation.EventEmitter;
import org.apache.fineract.cn.lang.ServiceException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author Myrle Krantz
 */
@SuppressWarnings("unused")
@Aggregate
public class CaseCommandHandler {
  private final PatternFactoryRegistry patternFactoryRegistry;
  private final ProductRepository productRepository;
  private final CaseRepository caseRepository;
  private final TaskDefinitionRepository taskDefinitionRepository;

  @Autowired
  public CaseCommandHandler(final PatternFactoryRegistry patternFactoryRegistry,
                            final ProductRepository productRepository,
                            final CaseRepository caseRepository,
                            final TaskDefinitionRepository taskDefinitionRepository) {
    super();
    this.patternFactoryRegistry = patternFactoryRegistry;
    this.productRepository = productRepository;
    this.caseRepository = caseRepository;
    this.taskDefinitionRepository = taskDefinitionRepository;
  }

  @Transactional
  @CommandHandler(logStart = CommandLogLevel.INFO, logFinish = CommandLogLevel.INFO)
  @EventEmitter(selectorName = EventConstants.SELECTOR_NAME, selectorValue = EventConstants.POST_CASE)
  public CaseEvent process(final CreateCaseCommand createCaseCommand) {
    final Case caseInstance = createCaseCommand.getCase();

    final Stream<TaskDefinitionEntity> tasksToCreate
        = taskDefinitionRepository.findByProductId(createCaseCommand.getCase().getProductIdentifier());

    final CaseEntity entity = CaseMapper.map(caseInstance);
    entity.setCurrentState(Case.State.CREATED.name());
    entity.setTaskInstances(tasksToCreate
        .map(taskDefinition -> instanceOfDefinition(taskDefinition, entity))
        .collect(Collectors.toSet()));
    this.caseRepository.save(entity);

    getPatternFactory(caseInstance.getProductIdentifier()).persistParameters(entity.getId(), caseInstance.getParameters());


    return new CaseEvent(caseInstance.getProductIdentifier(), caseInstance.getIdentifier());
  }

  private PatternFactory getPatternFactory(final String productIdentifier) {
    return productRepository.findByIdentifier(productIdentifier)
              .map(ProductEntity::getPatternPackage)
              .flatMap(patternFactoryRegistry::getPatternFactoryForPackage)
              .orElseThrow(() -> new IllegalArgumentException("Case references unsupported product type."));
  }

  @Transactional
  @CommandHandler(logStart = CommandLogLevel.INFO, logFinish = CommandLogLevel.INFO)
  @EventEmitter(selectorName = EventConstants.SELECTOR_NAME, selectorValue = EventConstants.PUT_CASE)
  public CaseEvent process(final ChangeCaseCommand changeCaseCommand) {
    final Case instance = changeCaseCommand.getInstance();

    final CaseEntity oldEntity = caseRepository
            .findByProductIdentifierAndIdentifier(instance.getProductIdentifier(), instance.getIdentifier())
            .orElseThrow(() -> ServiceException
                .notFound("Case not found '" + instance.getIdentifier() + "'."));

    final CaseEntity newEntity = CaseMapper.mapOverOldEntity(instance, oldEntity);

    caseRepository.save(newEntity);

    getPatternFactory(instance.getProductIdentifier()).changeParameters(oldEntity.getId(), instance.getParameters());

    return new CaseEvent(instance.getProductIdentifier(), instance.getIdentifier());
  }

  private static TaskInstanceEntity instanceOfDefinition(final TaskDefinitionEntity definition,
                                                         final CaseEntity customerCase) {
    final TaskInstanceEntity ret = new TaskInstanceEntity();
    ret.setCustomerCase(customerCase);
    ret.setTaskDefinition(definition);
    ret.setComment("");
    return ret;
  }
}