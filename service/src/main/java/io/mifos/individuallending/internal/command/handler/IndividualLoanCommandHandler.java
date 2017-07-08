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
import io.mifos.portfolio.api.v1.domain.ChargeDefinition;
import io.mifos.portfolio.api.v1.domain.CostComponent;
import io.mifos.portfolio.api.v1.events.EventConstants;
import io.mifos.portfolio.service.internal.mapper.CaseMapper;
import io.mifos.portfolio.service.internal.repository.CaseEntity;
import io.mifos.portfolio.service.internal.repository.CaseRepository;
import io.mifos.portfolio.service.internal.util.AccountingAdapter;
import io.mifos.portfolio.service.internal.util.ChargeInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
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
  private final AccountingAdapter accountingAdapter;

  @Autowired
  public IndividualLoanCommandHandler(
      final CaseRepository caseRepository,
      final CostComponentService costComponentService,
      final AccountingAdapter accountingAdapter) {
    this.caseRepository = caseRepository;
    this.costComponentService = costComponentService;
    this.accountingAdapter = accountingAdapter;
  }

  @Transactional
  @CommandHandler(logStart = CommandLogLevel.INFO, logFinish = CommandLogLevel.INFO)
  @EventEmitter(
      selectorName = EventConstants.SELECTOR_NAME,
      selectorValue = IndividualLoanEventConstants.OPEN_INDIVIDUALLOAN_CASE)
  public IndividualLoanCommandEvent process(final OpenCommand command) {
    final String productIdentifier = command.getProductIdentifier();
    final String caseIdentifier = command.getCaseIdentifier();
    final DataContextOfAction dataContextOfAction = costComponentService.checkedGetDataContext(
            productIdentifier, caseIdentifier, command.getCommand().getOneTimeAccountAssignments());
    IndividualLendingPatternFactory.checkActionCanBeExecuted(Case.State.valueOf(dataContextOfAction.getCustomerCase().getCurrentState()), Action.OPEN);

    final CostComponentsForRepaymentPeriod costComponents
        = costComponentService.getCostComponentsForOpen(dataContextOfAction);

    final DesignatorToAccountIdentifierMapper designatorToAccountIdentifierMapper
            = new DesignatorToAccountIdentifierMapper(dataContextOfAction);

    final List<ChargeInstance> charges = costComponents.stream()
        .map(x -> mapCostComponentEntryToChargeInstance(Action.OPEN, x, designatorToAccountIdentifierMapper))
        .collect(Collectors.toList());

    accountingAdapter.bookCharges(charges,
            command.getCommand().getNote(),
            productIdentifier + "." + caseIdentifier + "." + Action.OPEN.name(),
            Action.OPEN.getTransactionType());
    //Only move to new state if book charges command was accepted.
    final CaseEntity customerCase = dataContextOfAction.getCustomerCase();
    customerCase.setCurrentState(Case.State.PENDING.name());
    caseRepository.save(customerCase);

    return new IndividualLoanCommandEvent(productIdentifier, caseIdentifier);
  }

  @Transactional
  @CommandHandler(logStart = CommandLogLevel.INFO, logFinish = CommandLogLevel.INFO)
  @EventEmitter(
      selectorName = EventConstants.SELECTOR_NAME,
      selectorValue = IndividualLoanEventConstants.DENY_INDIVIDUALLOAN_CASE)
  public IndividualLoanCommandEvent process(final DenyCommand command) {
    final String productIdentifier = command.getProductIdentifier();
    final String caseIdentifier = command.getCaseIdentifier();
    final DataContextOfAction dataContextOfAction = costComponentService.checkedGetDataContext(
        productIdentifier, caseIdentifier, command.getCommand().getOneTimeAccountAssignments());
    IndividualLendingPatternFactory.checkActionCanBeExecuted(Case.State.valueOf(dataContextOfAction.getCustomerCase().getCurrentState()), Action.DENY);

    final CostComponentsForRepaymentPeriod costComponents
        = costComponentService.getCostComponentsForDeny(dataContextOfAction);

    final DesignatorToAccountIdentifierMapper designatorToAccountIdentifierMapper
        = new DesignatorToAccountIdentifierMapper(dataContextOfAction);

    final List<ChargeInstance> charges = costComponents.stream()
        .map(x -> mapCostComponentEntryToChargeInstance(Action.DENY, x, designatorToAccountIdentifierMapper))
        .collect(Collectors.toList());

    final CaseEntity customerCase = dataContextOfAction.getCustomerCase();
    customerCase.setCurrentState(Case.State.CLOSED.name());
    caseRepository.save(customerCase);

    return new IndividualLoanCommandEvent(productIdentifier, caseIdentifier);
  }

  @Transactional
  @CommandHandler(logStart = CommandLogLevel.INFO, logFinish = CommandLogLevel.INFO)
  @EventEmitter(
      selectorName = EventConstants.SELECTOR_NAME,
      selectorValue = IndividualLoanEventConstants.APPROVE_INDIVIDUALLOAN_CASE)
  public IndividualLoanCommandEvent process(final ApproveCommand command) {
    final String productIdentifier = command.getProductIdentifier();
    final String caseIdentifier = command.getCaseIdentifier();
    final DataContextOfAction dataContextOfAction = costComponentService.checkedGetDataContext(productIdentifier, caseIdentifier, command.getCommand().getOneTimeAccountAssignments());
    IndividualLendingPatternFactory.checkActionCanBeExecuted(Case.State.valueOf(dataContextOfAction.getCustomerCase().getCurrentState()), Action.APPROVE);

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

    final CostComponentsForRepaymentPeriod costComponentsForRepaymentPeriod =
        costComponentService.getCostComponentsForApprove(dataContextOfAction);

    final List<ChargeInstance> charges = costComponentsForRepaymentPeriod.stream()
        .map(x -> mapCostComponentEntryToChargeInstance(Action.APPROVE, x, designatorToAccountIdentifierMapper))
        .collect(Collectors.toList());

    accountingAdapter.bookCharges(charges,
            command.getCommand().getNote(),
            productIdentifier + "." + caseIdentifier + "." + Action.APPROVE.name(),
            Action.APPROVE.getTransactionType());

    //Only move to new state if book charges command was accepted.
    final CaseEntity customerCase = dataContextOfAction.getCustomerCase();
    customerCase.setCurrentState(Case.State.APPROVED.name());
    caseRepository.save(customerCase);

    return new IndividualLoanCommandEvent(productIdentifier, caseIdentifier);
  }

  @Transactional
  @CommandHandler(logStart = CommandLogLevel.INFO, logFinish = CommandLogLevel.INFO)
  @EventEmitter(selectorName = EventConstants.SELECTOR_NAME, selectorValue = IndividualLoanEventConstants.DISBURSE_INDIVIDUALLOAN_CASE)
  public IndividualLoanCommandEvent process(final DisburseCommand command) {
    final String productIdentifier = command.getProductIdentifier();
    final String caseIdentifier = command.getCaseIdentifier();
    final DataContextOfAction dataContextOfAction = costComponentService.checkedGetDataContext(
        productIdentifier, caseIdentifier, command.getCommand().getOneTimeAccountAssignments());
    IndividualLendingPatternFactory.checkActionCanBeExecuted(Case.State.valueOf(dataContextOfAction.getCustomerCase().getCurrentState()), Action.DISBURSE);


    final CostComponentsForRepaymentPeriod costComponentsForRepaymentPeriod =
        costComponentService.getCostComponentsForDisburse(dataContextOfAction);

    final DesignatorToAccountIdentifierMapper designatorToAccountIdentifierMapper
        = new DesignatorToAccountIdentifierMapper(dataContextOfAction);

    final BigDecimal disbursalAmount = dataContextOfAction.getCaseParameters().getMaximumBalance();
    final List<ChargeInstance> charges = Stream.concat(
          costComponentsForRepaymentPeriod.stream().map(x -> mapCostComponentEntryToChargeInstance(Action.DISBURSE, x, designatorToAccountIdentifierMapper)),
          Stream.of(getDisbursalChargeInstance(disbursalAmount, designatorToAccountIdentifierMapper)))
        .collect(Collectors.toList());

    accountingAdapter.bookCharges(charges,
        command.getCommand().getNote(),
        productIdentifier + "." + caseIdentifier + "." + Action.DISBURSE.name(),
        Action.DISBURSE.getTransactionType());
    //Only move to new state if book charges command was accepted.
    if (Case.State.valueOf(dataContextOfAction.getCustomerCase().getCurrentState()) != Case.State.ACTIVE) {
      final CaseEntity customerCase = dataContextOfAction.getCustomerCase();
      final LocalDateTime endOfTerm
          = ScheduledActionHelpers.getRoughEndDate(LocalDate.now(ZoneId.of("UTC")), dataContextOfAction.getCaseParameters())
          .atTime(LocalTime.MIDNIGHT);
      customerCase.setEndOfTerm(endOfTerm);
      customerCase.setCurrentState(Case.State.ACTIVE.name());
      caseRepository.save(customerCase);
    }

    return new IndividualLoanCommandEvent(productIdentifier, caseIdentifier);
  }

  @Transactional
  @CommandHandler(logStart = CommandLogLevel.INFO, logFinish = CommandLogLevel.INFO)
  @EventEmitter(
      selectorName = EventConstants.SELECTOR_NAME,
      selectorValue = IndividualLoanEventConstants.APPLY_INTEREST_INDIVIDUALLOAN_CASE)
  public IndividualLoanCommandEvent process(final ApplyInterestCommand command) {
    final String productIdentifier = command.getProductIdentifier();
    final String caseIdentifier = command.getCaseIdentifier();
    final DataContextOfAction dataContextOfAction = costComponentService.checkedGetDataContext(
        productIdentifier, caseIdentifier, null);
    IndividualLendingPatternFactory.checkActionCanBeExecuted(Case.State.valueOf(dataContextOfAction.getCustomerCase().getCurrentState()), Action.APPLY_INTEREST);
    if (dataContextOfAction.getCustomerCase().getEndOfTerm() == null)
      throw ServiceException.internalError(
          "End of term not set for active case ''{0}.{1}.''", productIdentifier, caseIdentifier);

    final CostComponentsForRepaymentPeriod costComponentsForRepaymentPeriod =
        costComponentService.getCostComponentsForApplyInterest(dataContextOfAction);

    final DesignatorToAccountIdentifierMapper designatorToAccountIdentifierMapper
        = new DesignatorToAccountIdentifierMapper(dataContextOfAction);

    final List<ChargeInstance> charges = costComponentsForRepaymentPeriod.stream()
        .map(x -> mapCostComponentEntryToChargeInstance(Action.APPLY_INTEREST, x, designatorToAccountIdentifierMapper))
        .collect(Collectors.toList());

    accountingAdapter.bookCharges(charges,
        "",
        productIdentifier + "." + caseIdentifier + "." + Action.APPLY_INTEREST.name(),
        Action.APPLY_INTEREST.getTransactionType());

    return new IndividualLoanCommandEvent(productIdentifier, caseIdentifier);
  }

  @Transactional
  @CommandHandler(logStart = CommandLogLevel.INFO, logFinish = CommandLogLevel.INFO)
  @EventEmitter(selectorName = EventConstants.SELECTOR_NAME, selectorValue = IndividualLoanEventConstants.ACCEPT_PAYMENT_INDIVIDUALLOAN_CASE)
  public IndividualLoanCommandEvent process(final AcceptPaymentCommand command) {
    final String productIdentifier = command.getProductIdentifier();
    final String caseIdentifier = command.getCaseIdentifier();
    final DataContextOfAction dataContextOfAction = costComponentService.checkedGetDataContext(productIdentifier, caseIdentifier, command.getCommand().getOneTimeAccountAssignments());
    IndividualLendingPatternFactory.checkActionCanBeExecuted(Case.State.valueOf(dataContextOfAction.getCustomerCase().getCurrentState()), Action.ACCEPT_PAYMENT);
    final CaseEntity customerCase = dataContextOfAction.getCustomerCase();
    customerCase.setCurrentState(Case.State.ACTIVE.name());
    caseRepository.save(customerCase);
    return new IndividualLoanCommandEvent(command.getProductIdentifier(), command.getCaseIdentifier());
  }

  @Transactional
  @CommandHandler(logStart = CommandLogLevel.INFO, logFinish = CommandLogLevel.INFO)
  @EventEmitter(selectorName = EventConstants.SELECTOR_NAME, selectorValue = IndividualLoanEventConstants.WRITE_OFF_INDIVIDUALLOAN_CASE)
  public IndividualLoanCommandEvent process(final WriteOffCommand command) {
    final String productIdentifier = command.getProductIdentifier();
    final String caseIdentifier = command.getCaseIdentifier();
    final DataContextOfAction dataContextOfAction = costComponentService.checkedGetDataContext(productIdentifier, caseIdentifier, command.getCommand().getOneTimeAccountAssignments());
    IndividualLendingPatternFactory.checkActionCanBeExecuted(Case.State.valueOf(dataContextOfAction.getCustomerCase().getCurrentState()), Action.WRITE_OFF);
    final CaseEntity customerCase = dataContextOfAction.getCustomerCase();
    customerCase.setCurrentState(Case.State.CLOSED.name());
    caseRepository.save(customerCase);
    return new IndividualLoanCommandEvent(command.getProductIdentifier(), command.getCaseIdentifier());
  }

  @Transactional
  @CommandHandler(logStart = CommandLogLevel.INFO, logFinish = CommandLogLevel.INFO)
  @EventEmitter(selectorName = EventConstants.SELECTOR_NAME, selectorValue = IndividualLoanEventConstants.CLOSE_INDIVIDUALLOAN_CASE)
  public IndividualLoanCommandEvent process(final CloseCommand command) {
    final String productIdentifier = command.getProductIdentifier();
    final String caseIdentifier = command.getCaseIdentifier();
    final DataContextOfAction dataContextOfAction = costComponentService.checkedGetDataContext(productIdentifier, caseIdentifier, command.getCommand().getOneTimeAccountAssignments());
    IndividualLendingPatternFactory.checkActionCanBeExecuted(Case.State.valueOf(dataContextOfAction.getCustomerCase().getCurrentState()), Action.CLOSE);
    final CaseEntity customerCase = dataContextOfAction.getCustomerCase();
    customerCase.setCurrentState(Case.State.CLOSED.name());
    caseRepository.save(customerCase);
    return new IndividualLoanCommandEvent(command.getProductIdentifier(), command.getCaseIdentifier());
  }

  @Transactional
  @CommandHandler(logStart = CommandLogLevel.INFO, logFinish = CommandLogLevel.INFO)
  @EventEmitter(selectorName = EventConstants.SELECTOR_NAME, selectorValue = IndividualLoanEventConstants.RECOVER_INDIVIDUALLOAN_CASE)
  public IndividualLoanCommandEvent process(final RecoverCommand command) {
    final String productIdentifier = command.getProductIdentifier();
    final String caseIdentifier = command.getCaseIdentifier();
    final DataContextOfAction dataContextOfAction = costComponentService.checkedGetDataContext(productIdentifier, caseIdentifier, command.getCommand().getOneTimeAccountAssignments());
    IndividualLendingPatternFactory.checkActionCanBeExecuted(Case.State.valueOf(dataContextOfAction.getCustomerCase().getCurrentState()), Action.RECOVER);
    final CaseEntity customerCase = dataContextOfAction.getCustomerCase();
    customerCase.setCurrentState(Case.State.CLOSED.name());
    caseRepository.save(customerCase);
    return new IndividualLoanCommandEvent(command.getProductIdentifier(), command.getCaseIdentifier());
  }


  private static ChargeInstance mapCostComponentEntryToChargeInstance(
      final Action action,
      final Map.Entry<ChargeDefinition, CostComponent> costComponentEntry,
      final DesignatorToAccountIdentifierMapper designatorToAccountIdentifierMapper) {
    final ChargeDefinition chargeDefinition = costComponentEntry.getKey();
    if (chargeDefinition.getAccrualAccountDesignator() != null) {
      if (Action.valueOf(chargeDefinition.getAccrueAction()) == action)
        return new ChargeInstance(
            designatorToAccountIdentifierMapper.mapOrThrow(chargeDefinition.getFromAccountDesignator()),
            designatorToAccountIdentifierMapper.mapOrThrow(chargeDefinition.getAccrualAccountDesignator()),
            costComponentEntry.getValue().getAmount());
      else
        return new ChargeInstance(
            designatorToAccountIdentifierMapper.mapOrThrow(chargeDefinition.getToAccountDesignator()),
            designatorToAccountIdentifierMapper.mapOrThrow(chargeDefinition.getAccrualAccountDesignator()),
            costComponentEntry.getValue().getAmount());
    }
    else
      return new ChargeInstance(
          designatorToAccountIdentifierMapper.mapOrThrow(chargeDefinition.getFromAccountDesignator()),
          designatorToAccountIdentifierMapper.mapOrThrow(chargeDefinition.getToAccountDesignator()),
          costComponentEntry.getValue().getAmount());
  }

  private static ChargeInstance getDisbursalChargeInstance(
      final BigDecimal amount,
      final DesignatorToAccountIdentifierMapper designatorToAccountIdentifierMapper) {
    return new ChargeInstance(
        designatorToAccountIdentifierMapper.mapOrThrow(AccountDesignators.PENDING_DISBURSAL),
        designatorToAccountIdentifierMapper.mapOrThrow(AccountDesignators.CUSTOMER_LOAN),
        amount);
  }
}