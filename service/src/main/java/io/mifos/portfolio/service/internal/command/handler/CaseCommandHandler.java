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

import io.mifos.portfolio.service.internal.command.ChangeCaseCommand;
import io.mifos.portfolio.service.internal.command.CreateCaseCommand;
import io.mifos.portfolio.service.internal.mapper.CaseMapper;
import io.mifos.portfolio.service.internal.repository.*;
import io.mifos.products.spi.PatternFactory;
import io.mifos.portfolio.service.internal.pattern.PatternFactoryRegistry;
import io.mifos.core.api.util.UserContextHolder;
import io.mifos.core.command.annotation.Aggregate;
import io.mifos.core.command.annotation.CommandHandler;
import io.mifos.core.command.annotation.EventEmitter;
import io.mifos.core.lang.ServiceException;
import io.mifos.portfolio.api.v1.domain.AccountAssignment;
import io.mifos.portfolio.api.v1.domain.Case;
import io.mifos.portfolio.api.v1.events.CaseEvent;
import io.mifos.portfolio.api.v1.events.EventConstants;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author Myrle Krantz
 */
@SuppressWarnings("unused")
@Aggregate
public class CaseCommandHandler {
  private final PatternFactoryRegistry patternFactoryRegistry;
  private final ProductRepository productRepository;
  private final CaseRepository caseRepository;
  private final CaseAccountAssignmentRepository caseAccountAssignmentRepository;

  @Autowired
  public CaseCommandHandler(final PatternFactoryRegistry patternFactoryRegistry,
                            final ProductRepository productRepository,
                            final CaseRepository caseRepository,
                            final CaseAccountAssignmentRepository caseAccountAssignmentRepository) {
    super();
    this.patternFactoryRegistry = patternFactoryRegistry;
    this.productRepository = productRepository;
    this.caseRepository = caseRepository;
    this.caseAccountAssignmentRepository = caseAccountAssignmentRepository;
  }

  @Transactional
  @CommandHandler
  @EventEmitter(selectorName = EventConstants.SELECTOR_NAME, selectorValue = EventConstants.POST_CASE)
  public CaseEvent process(final CreateCaseCommand createCaseCommand) {
    //TODO: Check that all designators are assigned to existing accounts.
    //TODO: Create accounts if necessary.
    //TODO: save parameters into their own table.

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
  @CommandHandler
  @EventEmitter(selectorName = EventConstants.SELECTOR_NAME, selectorValue = EventConstants.PUT_CASE)
  public CaseEvent process(final ChangeCaseCommand changeCaseCommand) {
    final Case caseInstance = changeCaseCommand.getInstance();

    final CaseEntity oldEntity = caseRepository
            .findByProductIdentifierAndIdentifier(caseInstance.getProductIdentifier(), caseInstance.getIdentifier())
            .orElseThrow(() -> ServiceException.notFound("Case not found '"
                    + caseInstance.getProductIdentifier() + "." + caseInstance.getIdentifier() + "'."));

    oldEntity.setLastModifiedBy(UserContextHolder.checkedGetUser());
    oldEntity.setLastModifiedOn(LocalDateTime.now(ZoneId.of("UTC")));

    final Set<CaseAccountAssignmentEntity> accountAssignments = oldEntity.getAccountAssignments();
    final Map<String, CaseAccountAssignmentEntity> accountAssignmentsMap
            = accountAssignments.stream()
            .collect(Collectors.toMap(CaseAccountAssignmentEntity::getDesignator, x -> x));

    final Set<AccountAssignment> newAccountAssignments = caseInstance.getAccountAssignments();
    newAccountAssignments.forEach(x -> {
      final String accountDesignator = x.getDesignator();
      final CaseAccountAssignmentEntity accountAssignmentEntity = accountAssignmentsMap.get(accountDesignator);
      if (accountAssignmentEntity != null)
        accountAssignmentEntity.setIdentifier(x.getAccountIdentifier());
      else
        accountAssignments.add(CaseMapper.mapAccountAssignment(x, oldEntity));
    });

    accountAssignments.forEach(caseAccountAssignmentRepository::save);


    caseRepository.save(oldEntity);

    getPatternFactory(caseInstance.getProductIdentifier()).changeParameters(oldEntity.getId(), caseInstance.getParameters());

    return new CaseEvent(caseInstance.getProductIdentifier(), caseInstance.getIdentifier());
  }
}
