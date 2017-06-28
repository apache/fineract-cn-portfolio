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


import io.mifos.core.command.annotation.Aggregate;
import io.mifos.core.command.annotation.CommandHandler;
import io.mifos.core.command.annotation.CommandLogLevel;
import io.mifos.core.command.annotation.EventEmitter;
import io.mifos.core.lang.ServiceException;
import io.mifos.individuallending.IndividualLendingPatternFactory;
import io.mifos.individuallending.api.v1.domain.caseinstance.CaseParameters;
import io.mifos.individuallending.api.v1.domain.workflow.Action;
import io.mifos.individuallending.api.v1.events.IndividualLoanCommandEvent;
import io.mifos.individuallending.api.v1.events.IndividualLoanEventConstants;
import io.mifos.individuallending.internal.command.*;
import io.mifos.individuallending.internal.mapper.CaseParametersMapper;
import io.mifos.individuallending.internal.repository.CaseParametersRepository;
import io.mifos.individuallending.internal.service.IndividualLoanService;
import io.mifos.portfolio.api.v1.domain.AccountAssignment;
import io.mifos.portfolio.api.v1.domain.Case;
import io.mifos.portfolio.api.v1.events.EventConstants;
import io.mifos.portfolio.service.internal.mapper.CaseMapper;
import io.mifos.portfolio.service.internal.mapper.ProductMapper;
import io.mifos.portfolio.service.internal.repository.*;
import io.mifos.portfolio.service.internal.util.AccountingAdapter;
import io.mifos.portfolio.service.internal.util.ChargeInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @author Myrle Krantz
 */
@SuppressWarnings("unused")
@Aggregate
public class IndividualLoanCommandHandler {

  private final ProductRepository productRepository;
  private final CaseRepository caseRepository;
  private final CaseParametersRepository caseParametersRepository;
  private final AccountingAdapter accountingAdapter;
  private final IndividualLoanService individualLoanService;

  @Autowired
  public IndividualLoanCommandHandler(final ProductRepository productRepository,
                                      final CaseRepository caseRepository,
                                      final CaseParametersRepository caseParametersRepository,
                                      final AccountingAdapter accountingAdapter,
                                      final IndividualLoanService individualLoanService) {
    this.productRepository = productRepository;
    this.caseRepository = caseRepository;
    this.caseParametersRepository = caseParametersRepository;
    this.accountingAdapter = accountingAdapter;
    this.individualLoanService = individualLoanService;
  }

  @Transactional
  @CommandHandler(logStart = CommandLogLevel.INFO, logFinish = CommandLogLevel.INFO)
  @EventEmitter(selectorName = EventConstants.SELECTOR_NAME, selectorValue = IndividualLoanEventConstants.OPEN_INDIVIDUALLOAN_CASE)
  public IndividualLoanCommandEvent process(final OpenCommand command) {
    final ProductEntity product = getProductOrThrow(command.getProductIdentifier());
    final CaseEntity customerCase = getCaseOrThrow(command.getProductIdentifier(), command.getCaseIdentifier());
    checkActionCanBeExecuted(Case.State.valueOf(customerCase.getCurrentState()), Action.OPEN);

    final CaseParameters caseParameters =
            caseParametersRepository.findByCaseId(customerCase.getId())
            .map(CaseParametersMapper::mapEntity)
            .orElseThrow(() -> ServiceException.notFound(
                    "Individual loan with identifier ''{0}''.''{1}'' doesn''t exist.",
                    command.getProductIdentifier(), command.getCaseIdentifier()));

    final Set<ProductAccountAssignmentEntity> productAccountAssignments = product.getAccountAssignments();
    final Set<CaseAccountAssignmentEntity> caseAccountAssignments = customerCase.getAccountAssignments();

    final List<ChargeInstance> chargesNamedViaAccountDesignators =
            individualLoanService.getChargeInstances(command.getProductIdentifier(), caseParameters, BigDecimal.ZERO, Action.OPEN, today(), LocalDate.now());
    final List<ChargeInstance> chargesNamedViaAccountIdentifier = chargesNamedViaAccountDesignators.stream().map(x -> new ChargeInstance(
            designatorToAccountIdentifierOrThrow(x.getFromAccount(), command.getCommand().getOneTimeAccountAssignments(), caseAccountAssignments, productAccountAssignments),
            designatorToAccountIdentifierOrThrow(x.getToAccount(), command.getCommand().getOneTimeAccountAssignments(), caseAccountAssignments, productAccountAssignments),
            x.getAmount())).collect(Collectors.toList());
    //TODO: Accrual

    accountingAdapter.bookCharges(chargesNamedViaAccountIdentifier,
            command.getCommand().getNote(),
            command.getProductIdentifier() + "." + command.getCaseIdentifier() + "." + Action.OPEN.name(),
            Action.OPEN.getTransactionType());
    //Only move to pending if book charges command was accepted.
    updateCaseState(customerCase, Case.State.PENDING);

    return new IndividualLoanCommandEvent(command.getProductIdentifier(), command.getCaseIdentifier(), "x");
  }

  private static LocalDate today() {
    return LocalDate.now(ZoneId.of("UTC"));
  }

  private String designatorToAccountIdentifierOrThrow(final String accountDesignator,
                                                      final List<AccountAssignment> oneTimeAccountAssignments,
                                                      final Set<CaseAccountAssignmentEntity> caseAccountAssignments,
                                                      final Set<ProductAccountAssignmentEntity> productAccountAssignments) {
    return allAccountAssignmentsAsStream(oneTimeAccountAssignments, caseAccountAssignments, productAccountAssignments)
            .filter(x -> x.getDesignator().equals(accountDesignator))
            .findFirst()
            .map(AccountAssignment::getAccountIdentifier)
            .orElseThrow(() -> ServiceException.badRequest("A required account designator was not set ''{0}''.", accountDesignator));
  }

  private Stream<AccountAssignment> allAccountAssignmentsAsStream(
          final List<AccountAssignment> oneTimeAccountAssignments,
          final Set<CaseAccountAssignmentEntity> caseAccountAssignments,
          final Set<ProductAccountAssignmentEntity> productAccountAssignments) {
    return Stream.concat(Stream.concat(
            oneTimeAccountAssignments.stream(),
            caseAccountAssignments.stream().map(CaseMapper::mapAccountAssignmentEntity)),
            productAccountAssignments.stream().map(ProductMapper::mapAccountAssignmentEntity));
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
            .orElseThrow(() -> ServiceException.notFound("Case not found ''{0}.{1}''.", productIdentifier, caseIdentifier));
  }

  private ProductEntity getProductOrThrow(final String productIdentifier) {
    return productRepository.findByIdentifier(productIdentifier)
            .orElseThrow(() -> ServiceException.notFound("Product not found ''{0}''.", productIdentifier));
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
