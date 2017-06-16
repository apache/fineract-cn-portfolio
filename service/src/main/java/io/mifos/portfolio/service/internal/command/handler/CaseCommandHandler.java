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

import io.mifos.core.command.annotation.Aggregate;
import io.mifos.core.command.annotation.CommandHandler;
import io.mifos.core.command.annotation.CommandLogLevel;
import io.mifos.core.command.annotation.EventEmitter;
import io.mifos.core.lang.ServiceException;
import io.mifos.portfolio.api.v1.domain.Case;
import io.mifos.portfolio.api.v1.events.CaseEvent;
import io.mifos.portfolio.api.v1.events.EventConstants;
import io.mifos.portfolio.service.internal.command.ChangeCaseCommand;
import io.mifos.portfolio.service.internal.command.CreateCaseCommand;
import io.mifos.portfolio.service.internal.mapper.CaseMapper;
import io.mifos.portfolio.service.internal.pattern.PatternFactoryRegistry;
import io.mifos.portfolio.service.internal.repository.*;
import io.mifos.products.spi.PatternFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * @author Myrle Krantz
 */
@SuppressWarnings("unused")
@Aggregate
public class CaseCommandHandler {
  private final PatternFactoryRegistry patternFactoryRegistry;
  private final ProductRepository productRepository;
  private final CaseRepository caseRepository;

  @Autowired
  public CaseCommandHandler(final PatternFactoryRegistry patternFactoryRegistry,
                            final ProductRepository productRepository,
                            final CaseRepository caseRepository) {
    super();
    this.patternFactoryRegistry = patternFactoryRegistry;
    this.productRepository = productRepository;
    this.caseRepository = caseRepository;
  }

  @Transactional
  @CommandHandler(logStart = CommandLogLevel.INFO, logFinish = CommandLogLevel.INFO)
  @EventEmitter(selectorName = EventConstants.SELECTOR_NAME, selectorValue = EventConstants.POST_CASE)
  public CaseEvent process(final CreateCaseCommand createCaseCommand) {
    //TODO: Check that all designators are assigned to existing accounts.
    //TODO: Create accounts if necessary.

    final Case caseInstance = createCaseCommand.getCase();

    final CaseEntity entity = CaseMapper.map(caseInstance);
    entity.setCurrentState(Case.State.CREATED.name());
    this.caseRepository.save(entity);

    getPatternFactory(caseInstance.getProductIdentifier()).persistParameters(entity.getId(), caseInstance.getParameters());


    return new CaseEvent(caseInstance.getProductIdentifier(), caseInstance.getIdentifier());
  }

  private PatternFactory getPatternFactory(String productIdentifier) {
    return productRepository.findByIdentifier(productIdentifier)
              .map(ProductEntity::getPatternPackage)
              .map(patternFactoryRegistry::getPatternFactoryForPackage)
              .orElse(Optional.empty())
              .orElseThrow(() -> new IllegalArgumentException("Case references unsupported product type."));
  }

  @Transactional
  @CommandHandler(logStart = CommandLogLevel.INFO, logFinish = CommandLogLevel.INFO)
  @EventEmitter(selectorName = EventConstants.SELECTOR_NAME, selectorValue = EventConstants.PUT_CASE)
  public CaseEvent process(final ChangeCaseCommand changeCaseCommand) {
    final Case instance = changeCaseCommand.getInstance();

    final CaseEntity oldEntity = caseRepository
            .findByProductIdentifierAndIdentifier(instance.getProductIdentifier(), instance.getIdentifier())
            .orElseThrow(() -> ServiceException.notFound("Case not found '" + instance.getIdentifier() + "'."));

    final CaseEntity newEntity = CaseMapper.mapOverOldEntity(instance, oldEntity);

    caseRepository.save(newEntity);

    getPatternFactory(instance.getProductIdentifier()).changeParameters(oldEntity.getId(), instance.getParameters());

    return new CaseEvent(instance.getProductIdentifier(), instance.getIdentifier());
  }
}
