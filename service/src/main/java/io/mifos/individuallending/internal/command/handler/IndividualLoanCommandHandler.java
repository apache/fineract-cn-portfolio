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
import io.mifos.individuallending.api.v1.domain.product.AccountDesignators;
import io.mifos.individuallending.api.v1.domain.workflow.Action;
import io.mifos.individuallending.api.v1.events.IndividualLoanCommandEvent;
import io.mifos.individuallending.api.v1.events.IndividualLoanEventConstants;
import io.mifos.individuallending.internal.command.*;
import io.mifos.individuallending.internal.service.*;
import io.mifos.portfolio.api.v1.domain.AccountAssignment;
import io.mifos.portfolio.api.v1.domain.Case;
import io.mifos.portfolio.api.v1.events.EventConstants;
import io.mifos.portfolio.service.internal.mapper.CaseMapper;
import io.mifos.portfolio.service.internal.repository.*;
import io.mifos.portfolio.service.internal.util.AccountingAdapter;
import io.mifos.portfolio.service.internal.util.ChargeInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @author Myrle Krantz
 */
@SuppressWarnings("unused")
@Aggregate
public class IndividualLoanCommandHandler {
  private final CaseRepository caseRepository;
  private final CostComponentService costComponentService;
  private final IndividualLoanService individualLoanService;
  private final AccountingAdapter accountingAdapter;

  @Autowired
  public IndividualLoanCommandHandler(
          final CaseRepository caseRepository,
          final CostComponentService costComponentService,
          final IndividualLoanService individualLoanService,
          final AccountingAdapter accountingAdapter) {
    this.caseRepository = caseRepository;
    this.costComponentService = costComponentService;
    this.individualLoanService = individualLoanService;
    this.accountingAdapter = accountingAdapter;
  }

  @Transactional
  @CommandHandler(logStart = CommandLogLevel.INFO, logFinish = CommandLogLevel.INFO)
  @EventEmitter(selectorName = EventConstants.SELECTOR_NAME, selectorValue = IndividualLoanEventConstants.OPEN_INDIVIDUALLOAN_CASE)
  public IndividualLoanCommandEvent process(final OpenCommand command) {
    final String productIdentifier = command.getProductIdentifier();
    final String caseIdentifier = command.getCaseIdentifier();
    final DataContextOfAction dataContextOfAction = costComponentService.checkedGetDataContext(
            productIdentifier, caseIdentifier, command.getCommand().getOneTimeAccountAssignments());
    checkActionCanBeExecuted(Case.State.valueOf(dataContextOfAction.getCustomerCase().getCurrentState()), Action.OPEN);


    final CostComponentsForRepaymentPeriod costComponentsForRepaymentPeriod =
            individualLoanService.getCostComponentsForRepaymentPeriod(productIdentifier, dataContextOfAction.getCaseParameters(), BigDecimal.ZERO, Action.OPEN, today(), LocalDate.now());


    final DesignatorToAccountIdentifierMapper designatorToAccountIdentifierMapper
            = new DesignatorToAccountIdentifierMapper(dataContextOfAction);

    final List<ChargeInstance> charges = costComponentsForRepaymentPeriod.stream().map(x -> new ChargeInstance(
            designatorToAccountIdentifierMapper.mapOrThrow(x.getKey().getFromAccountDesignator()),
            designatorToAccountIdentifierMapper.mapOrThrow(x.getKey().getToAccountDesignator()),
            x.getValue().getAmount())).collect(Collectors.toList());
    //TODO: Accrual

    accountingAdapter.bookCharges(charges,
            command.getCommand().getNote(),
            productIdentifier + "." + caseIdentifier + "." + Action.OPEN.name(),
            Action.OPEN.getTransactionType());
    //Only move to new state if book charges command was accepted.
    updateCaseState(dataContextOfAction.getCustomerCase(), Case.State.PENDING);

    return new IndividualLoanCommandEvent(productIdentifier, caseIdentifier, Action.OPEN.name());
  }

  @Transactional
  @CommandHandler(logStart = CommandLogLevel.INFO, logFinish = CommandLogLevel.INFO)
  @EventEmitter(selectorName = EventConstants.SELECTOR_NAME, selectorValue = IndividualLoanEventConstants.DENY_INDIVIDUALLOAN_CASE)
  public IndividualLoanCommandEvent process(final DenyCommand command) {
    final String productIdentifier = command.getProductIdentifier();
    final String caseIdentifier = command.getCaseIdentifier();
    final DataContextOfAction dataContextOfAction = costComponentService.checkedGetDataContext(productIdentifier, caseIdentifier, command.getCommand().getOneTimeAccountAssignments());
    checkActionCanBeExecuted(Case.State.valueOf(dataContextOfAction.getCustomerCase().getCurrentState()), Action.DENY);
    updateCaseState(dataContextOfAction.getCustomerCase(), Case.State.CLOSED);
    return new IndividualLoanCommandEvent(command.getProductIdentifier(), command.getCaseIdentifier(), "x");
  }

  @Transactional
  @CommandHandler(logStart = CommandLogLevel.INFO, logFinish = CommandLogLevel.INFO)
  @EventEmitter(selectorName = EventConstants.SELECTOR_NAME, selectorValue = IndividualLoanEventConstants.APPROVE_INDIVIDUALLOAN_CASE)
  public IndividualLoanCommandEvent process(final ApproveCommand command) {
    final String productIdentifier = command.getProductIdentifier();
    final String caseIdentifier = command.getCaseIdentifier();
    final DataContextOfAction dataContextOfAction = costComponentService.checkedGetDataContext(productIdentifier, caseIdentifier, command.getCommand().getOneTimeAccountAssignments());
    checkActionCanBeExecuted(Case.State.valueOf(dataContextOfAction.getCustomerCase().getCurrentState()), Action.APPROVE);

    //TODO: Check for incomplete task instances.

    final DesignatorToAccountIdentifierMapper designatorToAccountIdentifierMapper
            = new DesignatorToAccountIdentifierMapper(dataContextOfAction);

    //Create the needed account assignments and persist them for the case.
    designatorToAccountIdentifierMapper.getLedgersNeedingAccounts()
            .map(ledger ->
                    new AccountAssignment(ledger.getDesignator(),
                            accountingAdapter.createAccountForLedgerAssignment(dataContextOfAction.getCaseParameters().getCustomerIdentifier(), ledger)))
            .map(accountAssignment -> CaseMapper.map(accountAssignment, dataContextOfAction.getCustomerCase()))
            .forEach(caseAccountAssignmentEntity ->
              dataContextOfAction.getCustomerCase().getAccountAssignments().add(caseAccountAssignmentEntity)
            );
    caseRepository.save(dataContextOfAction.getCustomerCase());

    //Charge the approval fee if applicable.
    final CostComponentsForRepaymentPeriod costComponentsForRepaymentPeriod =
            individualLoanService.getCostComponentsForRepaymentPeriod(productIdentifier, dataContextOfAction.getCaseParameters(), BigDecimal.ZERO, Action.APPROVE, today(), LocalDate.now());

    final List<ChargeInstance> charges = costComponentsForRepaymentPeriod.stream().map(x -> new ChargeInstance(
            designatorToAccountIdentifierMapper.mapOrThrow(x.getKey().getFromAccountDesignator()),
            designatorToAccountIdentifierMapper.mapOrThrow(x.getKey().getToAccountDesignator()),
            x.getValue().getAmount())).collect(Collectors.toList());

    accountingAdapter.bookCharges(charges,
            command.getCommand().getNote(),
            productIdentifier + "." + caseIdentifier + "." + Action.APPROVE.name(),
            Action.APPROVE.getTransactionType());

    //Only move to new state if book charges command was accepted.
    updateCaseState(dataContextOfAction.getCustomerCase(), Case.State.APPROVED);

    return new IndividualLoanCommandEvent(productIdentifier, caseIdentifier, Action.APPROVE.name());
  }

  @Transactional
  @CommandHandler(logStart = CommandLogLevel.INFO, logFinish = CommandLogLevel.INFO)
  @EventEmitter(selectorName = EventConstants.SELECTOR_NAME, selectorValue = IndividualLoanEventConstants.DISBURSE_INDIVIDUALLOAN_CASE)
  public IndividualLoanCommandEvent process(final DisburseCommand command) {
    final String productIdentifier = command.getProductIdentifier();
    final String caseIdentifier = command.getCaseIdentifier();
    final DataContextOfAction dataContextOfAction = costComponentService.checkedGetDataContext(
        productIdentifier, caseIdentifier, command.getCommand().getOneTimeAccountAssignments());
    checkActionCanBeExecuted(Case.State.valueOf(dataContextOfAction.getCustomerCase().getCurrentState()), Action.DISBURSE);


    final CostComponentsForRepaymentPeriod costComponentsForRepaymentPeriod =
        individualLoanService.getCostComponentsForRepaymentPeriod(productIdentifier, dataContextOfAction.getCaseParameters(), BigDecimal.ZERO, Action.DISBURSE, today(), LocalDate.now());


    final DesignatorToAccountIdentifierMapper designatorToAccountIdentifierMapper
        = new DesignatorToAccountIdentifierMapper(dataContextOfAction);

    final List<ChargeInstance> charges = Stream.concat(costComponentsForRepaymentPeriod.stream().map(x -> new ChargeInstance(
            designatorToAccountIdentifierMapper.mapOrThrow(x.getKey().getFromAccountDesignator()),
            designatorToAccountIdentifierMapper.mapOrThrow(x.getKey().getToAccountDesignator()),
            x.getValue().getAmount())),
        Stream.of(new ChargeInstance(
            designatorToAccountIdentifierMapper.mapOrThrow(AccountDesignators.PENDING_DISBURSAL),
            designatorToAccountIdentifierMapper.mapOrThrow(AccountDesignators.CUSTOMER_LOAN),
                dataContextOfAction.getCaseParameters().getMaximumBalance())))
        .collect(Collectors.toList());

    accountingAdapter.bookCharges(charges,
        command.getCommand().getNote(),
        productIdentifier + "." + caseIdentifier + "." + Action.DISBURSE.name(),
        Action.DISBURSE.getTransactionType());
    //Only move to new state if book charges command was accepted.
    updateCaseState(dataContextOfAction.getCustomerCase(), Case.State.ACTIVE);

    return new IndividualLoanCommandEvent(productIdentifier, caseIdentifier, Action.DISBURSE.name());
  }

  @Transactional
  @CommandHandler(logStart = CommandLogLevel.INFO, logFinish = CommandLogLevel.INFO)
  @EventEmitter(selectorName = EventConstants.SELECTOR_NAME, selectorValue = IndividualLoanEventConstants.ACCEPT_PAYMENT_INDIVIDUALLOAN_CASE)
  public IndividualLoanCommandEvent process(final AcceptPaymentCommand command) {
    final String productIdentifier = command.getProductIdentifier();
    final String caseIdentifier = command.getCaseIdentifier();
    final DataContextOfAction dataContextOfAction = costComponentService.checkedGetDataContext(productIdentifier, caseIdentifier, command.getCommand().getOneTimeAccountAssignments());
    checkActionCanBeExecuted(Case.State.valueOf(dataContextOfAction.getCustomerCase().getCurrentState()), Action.ACCEPT_PAYMENT);
    updateCaseState(dataContextOfAction.getCustomerCase(), Case.State.ACTIVE);
    return new IndividualLoanCommandEvent(command.getProductIdentifier(), command.getCaseIdentifier(), "x");
  }

  @Transactional
  @CommandHandler(logStart = CommandLogLevel.INFO, logFinish = CommandLogLevel.INFO)
  @EventEmitter(selectorName = EventConstants.SELECTOR_NAME, selectorValue = IndividualLoanEventConstants.WRITE_OFF_INDIVIDUALLOAN_CASE)
  public IndividualLoanCommandEvent process(final WriteOffCommand command) {
    final String productIdentifier = command.getProductIdentifier();
    final String caseIdentifier = command.getCaseIdentifier();
    final DataContextOfAction dataContextOfAction = costComponentService.checkedGetDataContext(productIdentifier, caseIdentifier, command.getCommand().getOneTimeAccountAssignments());
    checkActionCanBeExecuted(Case.State.valueOf(dataContextOfAction.getCustomerCase().getCurrentState()), Action.WRITE_OFF);
    updateCaseState(dataContextOfAction.getCustomerCase(), Case.State.CLOSED);
    return new IndividualLoanCommandEvent(command.getProductIdentifier(), command.getCaseIdentifier(), "x");
  }

  @Transactional
  @CommandHandler(logStart = CommandLogLevel.INFO, logFinish = CommandLogLevel.INFO)
  @EventEmitter(selectorName = EventConstants.SELECTOR_NAME, selectorValue = IndividualLoanEventConstants.CLOSE_INDIVIDUALLOAN_CASE)
  public IndividualLoanCommandEvent process(final CloseCommand command) {
    final String productIdentifier = command.getProductIdentifier();
    final String caseIdentifier = command.getCaseIdentifier();
    final DataContextOfAction dataContextOfAction = costComponentService.checkedGetDataContext(productIdentifier, caseIdentifier, command.getCommand().getOneTimeAccountAssignments());
    checkActionCanBeExecuted(Case.State.valueOf(dataContextOfAction.getCustomerCase().getCurrentState()), Action.CLOSE);
    updateCaseState(dataContextOfAction.getCustomerCase(), Case.State.CLOSED);
    return new IndividualLoanCommandEvent(command.getProductIdentifier(), command.getCaseIdentifier(), "x");
  }

  @Transactional
  @CommandHandler(logStart = CommandLogLevel.INFO, logFinish = CommandLogLevel.INFO)
  @EventEmitter(selectorName = EventConstants.SELECTOR_NAME, selectorValue = IndividualLoanEventConstants.RECOVER_INDIVIDUALLOAN_CASE)
  public IndividualLoanCommandEvent process(final RecoverCommand command) {
    final String productIdentifier = command.getProductIdentifier();
    final String caseIdentifier = command.getCaseIdentifier();
    final DataContextOfAction dataContextOfAction = costComponentService.checkedGetDataContext(productIdentifier, caseIdentifier, command.getCommand().getOneTimeAccountAssignments());
    checkActionCanBeExecuted(Case.State.valueOf(dataContextOfAction.getCustomerCase().getCurrentState()), Action.RECOVER);
    updateCaseState(dataContextOfAction.getCustomerCase(), Case.State.CLOSED);
    return new IndividualLoanCommandEvent(command.getProductIdentifier(), command.getCaseIdentifier(), "x");
  }

  private static LocalDate today() {
    return LocalDate.now(ZoneId.of("UTC"));
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
