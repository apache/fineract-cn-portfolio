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
package io.mifos.individuallending.internal.command.handler;


import io.mifos.core.command.annotation.CommandLogLevel;
import io.mifos.individuallending.api.v1.domain.workflow.Action;
import io.mifos.individuallending.api.v1.events.IndividualLoanCommandEvent;
import io.mifos.individuallending.api.v1.events.IndividualLoanEventConstants;
import io.mifos.individuallending.IndividualLendingPatternFactory;
import io.mifos.individuallending.internal.command.*;
import io.mifos.portfolio.api.v1.domain.Case;
import io.mifos.portfolio.api.v1.events.EventConstants;
import io.mifos.portfolio.service.internal.repository.CaseEntity;
import io.mifos.portfolio.service.internal.repository.CaseRepository;
import io.mifos.core.command.annotation.Aggregate;
import io.mifos.core.command.annotation.CommandHandler;
import io.mifos.core.command.annotation.EventEmitter;
import io.mifos.core.lang.ServiceException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author Myrle Krantz
 */
@SuppressWarnings("unused")
@Aggregate
public class IndividualLoanCommandHandler {

  private final CaseRepository caseRepository;

  @Autowired
  public IndividualLoanCommandHandler(final CaseRepository caseRepository) {
    this.caseRepository = caseRepository;
  }

  @Transactional
  @CommandHandler(logStart = CommandLogLevel.INFO, logFinish = CommandLogLevel.INFO)
  @EventEmitter(selectorName = EventConstants.SELECTOR_NAME, selectorValue = IndividualLoanEventConstants.OPEN_INDIVIDUALLOAN_CASE)
  public IndividualLoanCommandEvent process(final OpenCommand command) {
    final CaseEntity customerCase = getCaseOrThrow(command.getProductIdentifier(), command.getCaseIdentifier());
    checkActionCanBeExecuted(Case.State.valueOf(customerCase.getCurrentState()), Action.OPEN);
    updateCaseState(customerCase, Case.State.PENDING);

    return new IndividualLoanCommandEvent(command.getProductIdentifier(), command.getCaseIdentifier(), "x");
  }

  @Transactional
  @CommandHandler(logStart = CommandLogLevel.INFO, logFinish = CommandLogLevel.INFO)
  @EventEmitter(selectorName = EventConstants.SELECTOR_NAME, selectorValue = IndividualLoanEventConstants.DENY_INDIVIDUALLOAN_CASE)
  public IndividualLoanCommandEvent process(final DenyCommand command) {
    final CaseEntity customerCase = getCaseOrThrow(command.getProductIdentifier(), command.getCaseIdentifier());
    checkActionCanBeExecuted(Case.State.valueOf(customerCase.getCurrentState()), Action.DENY);
    updateCaseState(customerCase, Case.State.CLOSED);
    return new IndividualLoanCommandEvent(command.getProductIdentifier(), command.getCaseIdentifier(), "x");
  }

  @Transactional
  @CommandHandler(logStart = CommandLogLevel.INFO, logFinish = CommandLogLevel.INFO)
  @EventEmitter(selectorName = EventConstants.SELECTOR_NAME, selectorValue = IndividualLoanEventConstants.APPROVE_INDIVIDUALLOAN_CASE)
  public IndividualLoanCommandEvent process(final ApproveCommand command) {
    final CaseEntity customerCase = getCaseOrThrow(command.getProductIdentifier(), command.getCaseIdentifier());
    checkActionCanBeExecuted(Case.State.valueOf(customerCase.getCurrentState()), Action.APPROVE);
    updateCaseState(customerCase, Case.State.APPROVED);
    return new IndividualLoanCommandEvent(command.getProductIdentifier(), command.getCaseIdentifier(), "x");
  }

  @Transactional
  @CommandHandler(logStart = CommandLogLevel.INFO, logFinish = CommandLogLevel.INFO)
  @EventEmitter(selectorName = EventConstants.SELECTOR_NAME, selectorValue = IndividualLoanEventConstants.DISBURSE_INDIVIDUALLOAN_CASE)
  public IndividualLoanCommandEvent process(final DisburseCommand command) {
    final CaseEntity customerCase = getCaseOrThrow(command.getProductIdentifier(), command.getCaseIdentifier());
    checkActionCanBeExecuted(Case.State.valueOf(customerCase.getCurrentState()), Action.DISBURSE);
    updateCaseState(customerCase, Case.State.ACTIVE);
    return new IndividualLoanCommandEvent(command.getProductIdentifier(), command.getCaseIdentifier(), "x");
  }

  @Transactional
  @CommandHandler(logStart = CommandLogLevel.INFO, logFinish = CommandLogLevel.INFO)
  @EventEmitter(selectorName = EventConstants.SELECTOR_NAME, selectorValue = IndividualLoanEventConstants.ACCEPT_PAYMENT_INDIVIDUALLOAN_CASE)
  public IndividualLoanCommandEvent process(final AcceptPaymentCommand command) {
    final CaseEntity customerCase = getCaseOrThrow(command.getProductIdentifier(), command.getCaseIdentifier());
    checkActionCanBeExecuted(Case.State.valueOf(customerCase.getCurrentState()), Action.ACCEPT_PAYMENT);
    updateCaseState(customerCase, Case.State.ACTIVE);
    return new IndividualLoanCommandEvent(command.getProductIdentifier(), command.getCaseIdentifier(), "x");
  }

  @Transactional
  @CommandHandler(logStart = CommandLogLevel.INFO, logFinish = CommandLogLevel.INFO)
  @EventEmitter(selectorName = EventConstants.SELECTOR_NAME, selectorValue = IndividualLoanEventConstants.WRITE_OFF_INDIVIDUALLOAN_CASE)
  public IndividualLoanCommandEvent process(final WriteOffCommand command) {
    final CaseEntity customerCase = getCaseOrThrow(command.getProductIdentifier(), command.getCaseIdentifier());
    checkActionCanBeExecuted(Case.State.valueOf(customerCase.getCurrentState()), Action.WRITE_OFF);
    updateCaseState(customerCase, Case.State.CLOSED);
    return new IndividualLoanCommandEvent(command.getProductIdentifier(), command.getCaseIdentifier(), "x");
  }

  @Transactional
  @CommandHandler(logStart = CommandLogLevel.INFO, logFinish = CommandLogLevel.INFO)
  @EventEmitter(selectorName = EventConstants.SELECTOR_NAME, selectorValue = IndividualLoanEventConstants.CLOSE_INDIVIDUALLOAN_CASE)
  public IndividualLoanCommandEvent process(final CloseCommand command) {
    final CaseEntity customerCase = getCaseOrThrow(command.getProductIdentifier(), command.getCaseIdentifier());
    checkActionCanBeExecuted(Case.State.valueOf(customerCase.getCurrentState()), Action.CLOSE);
    updateCaseState(customerCase, Case.State.CLOSED);
    return new IndividualLoanCommandEvent(command.getProductIdentifier(), command.getCaseIdentifier(), "x");
  }

  @Transactional
  @CommandHandler(logStart = CommandLogLevel.INFO, logFinish = CommandLogLevel.INFO)
  @EventEmitter(selectorName = EventConstants.SELECTOR_NAME, selectorValue = IndividualLoanEventConstants.RECOVER_INDIVIDUALLOAN_CASE)
  public IndividualLoanCommandEvent process(final RecoverCommand command) {
    final CaseEntity customerCase = getCaseOrThrow(command.getProductIdentifier(), command.getCaseIdentifier());
    checkActionCanBeExecuted(Case.State.valueOf(customerCase.getCurrentState()), Action.RECOVER);
    updateCaseState(customerCase, Case.State.CLOSED);
    return new IndividualLoanCommandEvent(command.getProductIdentifier(), command.getCaseIdentifier(), "x");
  }

  private CaseEntity getCaseOrThrow(final String productIdentifier, final String caseIdentifier) {
    return caseRepository.findByProductIdentifierAndIdentifier(productIdentifier, caseIdentifier)
              .orElseThrow(() -> ServiceException.notFound("case not found {0}.{1}", productIdentifier, caseIdentifier));
  }

  private void checkActionCanBeExecuted(final Case.State state, final Action action) {
    if (!IndividualLendingPatternFactory.getAllowedNextActionsForState(state).contains(action))
      throw ServiceException.badRequest("Cannot call action {0} from state {1}", action.name(), state.name());
  }

  private void updateCaseState(final CaseEntity customerCase, final Case.State state) {
    customerCase.setCurrentState(state.name());
    caseRepository.save(customerCase);
  }
}
