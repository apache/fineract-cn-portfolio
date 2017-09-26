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
import io.mifos.core.lang.DateConverter;
import io.mifos.core.lang.ServiceException;
import io.mifos.individuallending.IndividualLendingPatternFactory;
import io.mifos.individuallending.api.v1.domain.product.AccountDesignators;
import io.mifos.individuallending.api.v1.domain.workflow.Action;
import io.mifos.individuallending.api.v1.events.IndividualLoanCommandEvent;
import io.mifos.individuallending.api.v1.events.IndividualLoanEventConstants;
import io.mifos.individuallending.internal.command.*;
import io.mifos.individuallending.internal.repository.CaseParametersRepository;
import io.mifos.individuallending.internal.service.*;
import io.mifos.individuallending.internal.service.costcomponent.*;
import io.mifos.individuallending.internal.service.DataContextOfAction;
import io.mifos.individuallending.internal.service.schedule.ScheduledActionHelpers;
import io.mifos.portfolio.api.v1.domain.AccountAssignment;
import io.mifos.portfolio.api.v1.domain.Case;
import io.mifos.portfolio.api.v1.domain.CostComponent;
import io.mifos.portfolio.api.v1.events.EventConstants;
import io.mifos.portfolio.service.internal.mapper.CaseMapper;
import io.mifos.portfolio.service.internal.repository.CaseEntity;
import io.mifos.portfolio.service.internal.repository.CaseRepository;
import io.mifos.portfolio.service.internal.repository.TaskInstanceRepository;
import io.mifos.portfolio.service.internal.util.AccountingAdapter;
import io.mifos.portfolio.service.internal.util.ChargeInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Nullable;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * @author Myrle Krantz
 */
@SuppressWarnings("unused")
@Aggregate
public class IndividualLoanCommandHandler {
  private final CaseRepository caseRepository;
  private final DataContextService dataContextService;
  private final OpenPaymentBuilderService openPaymentBuilderService;
  private final ApprovePaymentBuilderService approvePaymentBuilderService;
  private final DenyPaymentBuilderService denyPaymentBuilderService;
  private final DisbursePaymentBuilderService disbursePaymentBuilderService;
  private final ApplyInterestPaymentBuilderService applyInterestPaymentBuilderService;
  private final AcceptPaymentBuilderService acceptPaymentBuilderService;
  private final ClosePaymentBuilderService closePaymentBuilderService;
  private final MarkLatePaymentBuilderService markLatePaymentBuilderService;
  private final WriteOffPaymentBuilderService writeOffPaymentBuilderService;
  private final RecoverPaymentBuilderService recoverPaymentBuilderService;
  private final AccountingAdapter accountingAdapter;
  private final TaskInstanceRepository taskInstanceRepository;
  private final CaseParametersRepository caseParametersRepository;

  @Autowired
  public IndividualLoanCommandHandler(
      final CaseRepository caseRepository,
      final DataContextService dataContextService,
      final OpenPaymentBuilderService openPaymentBuilderService,
      final ApprovePaymentBuilderService approvePaymentBuilderService,
      final DenyPaymentBuilderService denyPaymentBuilderService,
      final DisbursePaymentBuilderService disbursePaymentBuilderService,
      final ApplyInterestPaymentBuilderService applyInterestPaymentBuilderService,
      final AcceptPaymentBuilderService acceptPaymentBuilderService,
      final ClosePaymentBuilderService closePaymentBuilderService,
      final MarkLatePaymentBuilderService markLatePaymentBuilderService,
      final WriteOffPaymentBuilderService writeOffPaymentBuilderService,
      final RecoverPaymentBuilderService recoverPaymentBuilderService,
      final AccountingAdapter accountingAdapter,
      final TaskInstanceRepository taskInstanceRepository,
      final CaseParametersRepository caseParametersRepository) {
    this.caseRepository = caseRepository;
    this.dataContextService = dataContextService;
    this.openPaymentBuilderService = openPaymentBuilderService;
    this.approvePaymentBuilderService = approvePaymentBuilderService;
    this.denyPaymentBuilderService = denyPaymentBuilderService;
    this.disbursePaymentBuilderService = disbursePaymentBuilderService;
    this.applyInterestPaymentBuilderService = applyInterestPaymentBuilderService;
    this.acceptPaymentBuilderService = acceptPaymentBuilderService;
    this.closePaymentBuilderService = closePaymentBuilderService;
    this.markLatePaymentBuilderService = markLatePaymentBuilderService;
    this.writeOffPaymentBuilderService = writeOffPaymentBuilderService;
    this.recoverPaymentBuilderService = recoverPaymentBuilderService;
    this.accountingAdapter = accountingAdapter;
    this.taskInstanceRepository = taskInstanceRepository;
    this.caseParametersRepository = caseParametersRepository;
  }

  @Transactional
  @CommandHandler(logStart = CommandLogLevel.INFO, logFinish = CommandLogLevel.INFO)
  @EventEmitter(
      selectorName = EventConstants.SELECTOR_NAME,
      selectorValue = IndividualLoanEventConstants.OPEN_INDIVIDUALLOAN_CASE)
  public IndividualLoanCommandEvent process(final OpenCommand command) {
    final String productIdentifier = command.getProductIdentifier();
    final String caseIdentifier = command.getCaseIdentifier();
    final DataContextOfAction dataContextOfAction = dataContextService.checkedGetDataContext(
            productIdentifier, caseIdentifier, command.getCommand().getOneTimeAccountAssignments());
    IndividualLendingPatternFactory.checkActionCanBeExecuted(Case.State.valueOf(dataContextOfAction.getCustomerCaseEntity().getCurrentState()), Action.OPEN);

    checkIfTasksAreOutstanding(dataContextOfAction, Action.OPEN);

    final DesignatorToAccountIdentifierMapper designatorToAccountIdentifierMapper
        = new DesignatorToAccountIdentifierMapper(dataContextOfAction);
    final RealRunningBalances runningBalances = new RealRunningBalances(
        accountingAdapter,
        designatorToAccountIdentifierMapper);

    final PaymentBuilder paymentBuilder
        = openPaymentBuilderService.getPaymentBuilder(dataContextOfAction, BigDecimal.ZERO, CostComponentService.today(), runningBalances);

    final List<ChargeInstance> charges = paymentBuilder.buildCharges(Action.OPEN, designatorToAccountIdentifierMapper);

    final LocalDateTime today = today();

    accountingAdapter.bookCharges(charges,
        command.getCommand().getNote(),
        command.getCommand().getCreatedOn(),
        dataContextOfAction.getMessageForCharge(Action.OPEN),
        Action.OPEN.getTransactionType());
    //Only move to new state if book charges command was accepted.
    final CaseEntity customerCase = dataContextOfAction.getCustomerCaseEntity();
    customerCase.setCurrentState(Case.State.PENDING.name());
    caseRepository.save(customerCase);

    return new IndividualLoanCommandEvent(productIdentifier, caseIdentifier, DateConverter.toIsoString(today));
  }

  @Transactional
  @CommandHandler(logStart = CommandLogLevel.INFO, logFinish = CommandLogLevel.INFO)
  @EventEmitter(
      selectorName = EventConstants.SELECTOR_NAME,
      selectorValue = IndividualLoanEventConstants.DENY_INDIVIDUALLOAN_CASE)
  public IndividualLoanCommandEvent process(final DenyCommand command) {
    final String productIdentifier = command.getProductIdentifier();
    final String caseIdentifier = command.getCaseIdentifier();
    final DataContextOfAction dataContextOfAction = dataContextService.checkedGetDataContext(
        productIdentifier, caseIdentifier, command.getCommand().getOneTimeAccountAssignments());
    IndividualLendingPatternFactory.checkActionCanBeExecuted(Case.State.valueOf(dataContextOfAction.getCustomerCaseEntity().getCurrentState()), Action.DENY);

    checkIfTasksAreOutstanding(dataContextOfAction, Action.DENY);

    final DesignatorToAccountIdentifierMapper designatorToAccountIdentifierMapper
        = new DesignatorToAccountIdentifierMapper(dataContextOfAction);
    final RealRunningBalances runningBalances = new RealRunningBalances(
        accountingAdapter,
        designatorToAccountIdentifierMapper);

    final PaymentBuilder paymentBuilder
        = denyPaymentBuilderService.getPaymentBuilder(dataContextOfAction, BigDecimal.ZERO, CostComponentService.today(), runningBalances);

    final List<ChargeInstance> charges = paymentBuilder.buildCharges(Action.DENY, designatorToAccountIdentifierMapper);

    final LocalDateTime today = today();

    final CaseEntity customerCase = dataContextOfAction.getCustomerCaseEntity();
    customerCase.setCurrentState(Case.State.CLOSED.name());
    caseRepository.save(customerCase);

    return new IndividualLoanCommandEvent(productIdentifier, caseIdentifier, DateConverter.toIsoString(today));
  }

  static class InterruptedInALambdaException extends RuntimeException {

    private final InterruptedException interruptedException;

    InterruptedInALambdaException(InterruptedException e) {
      interruptedException = e;
    }

    void throwWrappedException() throws InterruptedException {
      throw interruptedException;
    }
  }

  @Transactional
  @CommandHandler(logStart = CommandLogLevel.INFO, logFinish = CommandLogLevel.INFO)
  @EventEmitter(
      selectorName = EventConstants.SELECTOR_NAME,
      selectorValue = IndividualLoanEventConstants.APPROVE_INDIVIDUALLOAN_CASE)
  public IndividualLoanCommandEvent process(final ApproveCommand command) throws InterruptedException
  {
    final String productIdentifier = command.getProductIdentifier();
    final String caseIdentifier = command.getCaseIdentifier();
    final DataContextOfAction dataContextOfAction = dataContextService.checkedGetDataContext(
        productIdentifier, caseIdentifier, command.getCommand().getOneTimeAccountAssignments());
    IndividualLendingPatternFactory.checkActionCanBeExecuted(Case.State.valueOf(dataContextOfAction.getCustomerCaseEntity().getCurrentState()), Action.APPROVE);

    checkIfTasksAreOutstanding(dataContextOfAction, Action.APPROVE);

    final DesignatorToAccountIdentifierMapper designatorToAccountIdentifierMapper
            = new DesignatorToAccountIdentifierMapper(dataContextOfAction);

    //Create the needed account assignments for groups and persist them for the case.
    try {
      designatorToAccountIdentifierMapper.getGroupsNeedingLedgers()
          .map(groupNeedingLedger -> {
            try {
              final String createdLedgerIdentifier = accountingAdapter.createLedger(
                  dataContextOfAction.getCaseParametersEntity().getCustomerIdentifier(),
                  groupNeedingLedger.getGroupName(),
                  groupNeedingLedger.getParentLedger());
              return new AccountAssignment(groupNeedingLedger.getGroupName(), createdLedgerIdentifier);
            } catch (InterruptedException e) {
              throw new InterruptedInALambdaException(e);
            }
          })
          .map(accountAssignment -> CaseMapper.map(accountAssignment, dataContextOfAction.getCustomerCaseEntity()))
          .forEach(caseAccountAssignmentEntity -> dataContextOfAction.getCustomerCaseEntity().getAccountAssignments().add(caseAccountAssignmentEntity));
    }
    catch (final InterruptedInALambdaException e) {
      e.throwWrappedException();
    }

    //Create the needed account assignments and persist them for the case.
    designatorToAccountIdentifierMapper.getLedgersNeedingAccounts()
        .map(ledger ->
            new AccountAssignment(ledger.getDesignator(),
                accountingAdapter.createAccountForLedgerAssignment(
                    dataContextOfAction.getCaseParametersEntity().getCustomerIdentifier(),
                    ledger)))
        .map(accountAssignment -> CaseMapper.map(accountAssignment, dataContextOfAction.getCustomerCaseEntity()))
        .forEach(caseAccountAssignmentEntity ->
            dataContextOfAction.getCustomerCaseEntity().getAccountAssignments().add(caseAccountAssignmentEntity)
        );
    caseRepository.save(dataContextOfAction.getCustomerCaseEntity());

    final RealRunningBalances runningBalances = new RealRunningBalances(
        accountingAdapter,
        designatorToAccountIdentifierMapper);

    final PaymentBuilder paymentBuilder =
        approvePaymentBuilderService.getPaymentBuilder(dataContextOfAction, BigDecimal.ZERO, CostComponentService.today(), runningBalances);

    final List<ChargeInstance> charges = paymentBuilder.buildCharges(Action.APPROVE, designatorToAccountIdentifierMapper);

    final LocalDateTime today = today();

    accountingAdapter.bookCharges(charges,
        command.getCommand().getNote(),
        command.getCommand().getCreatedOn(),
        dataContextOfAction.getMessageForCharge(Action.APPROVE),
        Action.APPROVE.getTransactionType());

    //Only move to new state if book charges command was accepted.
    final CaseEntity customerCase = dataContextOfAction.getCustomerCaseEntity();
    customerCase.setCurrentState(Case.State.APPROVED.name());
    caseRepository.save(customerCase);

    return new IndividualLoanCommandEvent(productIdentifier, caseIdentifier, DateConverter.toIsoString(today));
  }

  @Transactional
  @CommandHandler(logStart = CommandLogLevel.INFO, logFinish = CommandLogLevel.INFO)
  @EventEmitter(selectorName = EventConstants.SELECTOR_NAME, selectorValue = IndividualLoanEventConstants.DISBURSE_INDIVIDUALLOAN_CASE)
  public IndividualLoanCommandEvent process(final DisburseCommand command) {
    final String productIdentifier = command.getProductIdentifier();
    final String caseIdentifier = command.getCaseIdentifier();
    final DataContextOfAction dataContextOfAction = dataContextService.checkedGetDataContext(
        productIdentifier, caseIdentifier, command.getCommand().getOneTimeAccountAssignments());
    IndividualLendingPatternFactory.checkActionCanBeExecuted(Case.State.valueOf(dataContextOfAction.getCustomerCaseEntity().getCurrentState()), Action.DISBURSE);

    checkIfTasksAreOutstanding(dataContextOfAction, Action.DISBURSE);

    final BigDecimal disbursalAmount = Optional.ofNullable(command.getCommand().getPaymentSize()).orElse(BigDecimal.ZERO);

    final DesignatorToAccountIdentifierMapper designatorToAccountIdentifierMapper
        = new DesignatorToAccountIdentifierMapper(dataContextOfAction);
    final RealRunningBalances runningBalances = new RealRunningBalances(
        accountingAdapter,
        designatorToAccountIdentifierMapper);

    final PaymentBuilder paymentBuilder =
        disbursePaymentBuilderService.getPaymentBuilder(dataContextOfAction, disbursalAmount, CostComponentService.today(), runningBalances);

    final List<ChargeInstance> charges = paymentBuilder.buildCharges(Action.DISBURSE, designatorToAccountIdentifierMapper);

    final LocalDateTime today = today();

    accountingAdapter.bookCharges(charges,
        command.getCommand().getNote(),
        command.getCommand().getCreatedOn(),
        dataContextOfAction.getMessageForCharge(Action.DISBURSE),
        Action.DISBURSE.getTransactionType());
    //Only move to new state if book charges command was accepted.
    if (Case.State.valueOf(dataContextOfAction.getCustomerCaseEntity().getCurrentState()) != Case.State.ACTIVE) {
      final CaseEntity customerCase = dataContextOfAction.getCustomerCaseEntity();
      final LocalDateTime endOfTerm
          = ScheduledActionHelpers.getRoughEndDate(today.toLocalDate(), dataContextOfAction.getCaseParameters())
          .atTime(LocalTime.MIDNIGHT);
      customerCase.setEndOfTerm(endOfTerm);
      customerCase.setCurrentState(Case.State.ACTIVE.name());
      caseRepository.save(customerCase);
    }
    final String customerLoanPrinicipalAccountIdentifier = designatorToAccountIdentifierMapper.mapOrThrow(AccountDesignators.CUSTOMER_LOAN_PRINCIPAL);
    final String customerLoanInterestAccountIdentifier = designatorToAccountIdentifierMapper.mapOrThrow(AccountDesignators.CUSTOMER_LOAN_INTEREST);
    final String customerLoanFeesAccountIdentifier = designatorToAccountIdentifierMapper.mapOrThrow(AccountDesignators.CUSTOMER_LOAN_FEES);
    final BigDecimal currentBalance = accountingAdapter.getTotalOfCurrentAccountBalances(customerLoanPrinicipalAccountIdentifier, customerLoanInterestAccountIdentifier, customerLoanFeesAccountIdentifier);

    final BigDecimal newLoanPaymentSize = disbursePaymentBuilderService.getLoanPaymentSizeForSingleDisbursement(
        currentBalance.add(disbursalAmount),
        dataContextOfAction);

    dataContextOfAction.getCaseParametersEntity().setPaymentSize(newLoanPaymentSize);
    caseParametersRepository.save(dataContextOfAction.getCaseParametersEntity());

    return new IndividualLoanCommandEvent(productIdentifier, caseIdentifier, DateConverter.toIsoString(today));
  }

  @Transactional
  @CommandHandler(logStart = CommandLogLevel.INFO, logFinish = CommandLogLevel.INFO)
  @EventEmitter(
      selectorName = EventConstants.SELECTOR_NAME,
      selectorValue = IndividualLoanEventConstants.APPLY_INTEREST_INDIVIDUALLOAN_CASE)
  public IndividualLoanCommandEvent process(final ApplyInterestCommand command) {
    final String productIdentifier = command.getProductIdentifier();
    final String caseIdentifier = command.getCaseIdentifier();
    final DataContextOfAction dataContextOfAction = dataContextService.checkedGetDataContext(
        productIdentifier, caseIdentifier, null);
    IndividualLendingPatternFactory.checkActionCanBeExecuted(Case.State.valueOf(dataContextOfAction.getCustomerCaseEntity().getCurrentState()), Action.APPLY_INTEREST);

    if (dataContextOfAction.getCustomerCaseEntity().getEndOfTerm() == null)
      throw ServiceException.internalError(
          "End of term not set for active case ''{0}.{1}.''", productIdentifier, caseIdentifier);

    final DesignatorToAccountIdentifierMapper designatorToAccountIdentifierMapper
        = new DesignatorToAccountIdentifierMapper(dataContextOfAction);
    final RealRunningBalances runningBalances = new RealRunningBalances(
        accountingAdapter,
        designatorToAccountIdentifierMapper);

    final PaymentBuilder paymentBuilder =
        applyInterestPaymentBuilderService.getPaymentBuilder(dataContextOfAction, BigDecimal.ZERO, CostComponentService.today(), runningBalances);

    final List<ChargeInstance> charges = paymentBuilder.buildCharges(Action.APPLY_INTEREST, designatorToAccountIdentifierMapper);

    accountingAdapter.bookCharges(charges,
        "Applied interest on " + command.getForTime(),
        command.getForTime(),
        dataContextOfAction.getMessageForCharge(Action.APPLY_INTEREST),
        Action.APPLY_INTEREST.getTransactionType());

    return new IndividualLoanCommandEvent(productIdentifier, caseIdentifier, command.getForTime());
  }

  @Transactional
  @CommandHandler(logStart = CommandLogLevel.INFO, logFinish = CommandLogLevel.INFO)
  @EventEmitter(
      selectorName = EventConstants.SELECTOR_NAME,
      selectorValue = IndividualLoanEventConstants.ACCEPT_PAYMENT_INDIVIDUALLOAN_CASE)
  public IndividualLoanCommandEvent process(final AcceptPaymentCommand command) {
    final String productIdentifier = command.getProductIdentifier();
    final String caseIdentifier = command.getCaseIdentifier();
    final DataContextOfAction dataContextOfAction = dataContextService.checkedGetDataContext(
        productIdentifier, caseIdentifier, command.getCommand().getOneTimeAccountAssignments());
    IndividualLendingPatternFactory.checkActionCanBeExecuted(Case.State.valueOf(dataContextOfAction.getCustomerCaseEntity().getCurrentState()), Action.ACCEPT_PAYMENT);

    checkIfTasksAreOutstanding(dataContextOfAction, Action.ACCEPT_PAYMENT);

    if (dataContextOfAction.getCustomerCaseEntity().getEndOfTerm() == null)
      throw ServiceException.internalError(
          "End of term not set for active case ''{0}.{1}.''", productIdentifier, caseIdentifier);


    final DesignatorToAccountIdentifierMapper designatorToAccountIdentifierMapper
        = new DesignatorToAccountIdentifierMapper(dataContextOfAction);
    final RealRunningBalances runningBalances = new RealRunningBalances(
        accountingAdapter,
        designatorToAccountIdentifierMapper);

    final PaymentBuilder paymentBuilder =
        acceptPaymentBuilderService.getPaymentBuilder(
            dataContextOfAction,
            command.getCommand().getPaymentSize(),
            DateConverter.fromIsoString(command.getCommand().getCreatedOn()).toLocalDate(), runningBalances);

    final List<ChargeInstance> charges = paymentBuilder.buildCharges(Action.ACCEPT_PAYMENT, designatorToAccountIdentifierMapper);

    final LocalDateTime today = today();

    accountingAdapter.bookCharges(charges,
        command.getCommand().getNote(),
        command.getCommand().getCreatedOn(),
        dataContextOfAction.getMessageForCharge(Action.ACCEPT_PAYMENT),
        Action.ACCEPT_PAYMENT.getTransactionType());

    return new IndividualLoanCommandEvent(productIdentifier, caseIdentifier, DateConverter.toIsoString(today));
  }

  @Transactional
  @CommandHandler(logStart = CommandLogLevel.INFO, logFinish = CommandLogLevel.INFO)
  @EventEmitter(
      selectorName = EventConstants.SELECTOR_NAME,
      selectorValue = IndividualLoanEventConstants.MARK_LATE_INDIVIDUALLOAN_CASE)
  public IndividualLoanCommandEvent process(final MarkLateCommand command) {
    final String productIdentifier = command.getProductIdentifier();
    final String caseIdentifier = command.getCaseIdentifier();
    final DataContextOfAction dataContextOfAction = dataContextService.checkedGetDataContext(
        productIdentifier, caseIdentifier, Collections.emptyList());
    IndividualLendingPatternFactory.checkActionCanBeExecuted(Case.State.valueOf(dataContextOfAction.getCustomerCaseEntity().getCurrentState()), Action.MARK_LATE);

    checkIfTasksAreOutstanding(dataContextOfAction, Action.MARK_LATE);

    if (dataContextOfAction.getCustomerCaseEntity().getEndOfTerm() == null)
      throw ServiceException.internalError(
          "End of term not set for active case ''{0}.{1}.''", productIdentifier, caseIdentifier);

    final DesignatorToAccountIdentifierMapper designatorToAccountIdentifierMapper
        = new DesignatorToAccountIdentifierMapper(dataContextOfAction);
    final RealRunningBalances runningBalances = new RealRunningBalances(
        accountingAdapter,
        designatorToAccountIdentifierMapper);

    final PaymentBuilder paymentBuilder =
        markLatePaymentBuilderService.getPaymentBuilder(dataContextOfAction, BigDecimal.ZERO, DateConverter.fromIsoString(command.getForTime()).toLocalDate(),
            runningBalances);

    final List<ChargeInstance> charges = paymentBuilder.buildCharges(Action.MARK_LATE, designatorToAccountIdentifierMapper);

    final LocalDateTime today = today();

    accountingAdapter.bookCharges(charges,
        "Marked late on " + command.getForTime(),
        command.getForTime(),
        dataContextOfAction.getMessageForCharge(Action.MARK_LATE),
        Action.MARK_LATE.getTransactionType());

    return new IndividualLoanCommandEvent(productIdentifier, caseIdentifier, DateConverter.toIsoString(today));
  }

  @Transactional
  @CommandHandler(logStart = CommandLogLevel.INFO, logFinish = CommandLogLevel.INFO)
  @EventEmitter(selectorName = EventConstants.SELECTOR_NAME, selectorValue = IndividualLoanEventConstants.WRITE_OFF_INDIVIDUALLOAN_CASE)
  public IndividualLoanCommandEvent process(final WriteOffCommand command) {
    final String productIdentifier = command.getProductIdentifier();
    final String caseIdentifier = command.getCaseIdentifier();
    final DataContextOfAction dataContextOfAction = dataContextService.checkedGetDataContext(
        productIdentifier, caseIdentifier, command.getCommand().getOneTimeAccountAssignments());
    IndividualLendingPatternFactory.checkActionCanBeExecuted(Case.State.valueOf(dataContextOfAction.getCustomerCaseEntity().getCurrentState()), Action.WRITE_OFF);

    checkIfTasksAreOutstanding(dataContextOfAction, Action.WRITE_OFF);

    final LocalDateTime today = today();

    final CaseEntity customerCase = dataContextOfAction.getCustomerCaseEntity();
    customerCase.setCurrentState(Case.State.CLOSED.name());
    caseRepository.save(customerCase);

    return new IndividualLoanCommandEvent(productIdentifier, caseIdentifier, DateConverter.toIsoString(today));
  }

  @Transactional
  @CommandHandler(logStart = CommandLogLevel.INFO, logFinish = CommandLogLevel.INFO)
  @EventEmitter(selectorName = EventConstants.SELECTOR_NAME, selectorValue = IndividualLoanEventConstants.CLOSE_INDIVIDUALLOAN_CASE)
  public IndividualLoanCommandEvent process(final CloseCommand command) {
    final String productIdentifier = command.getProductIdentifier();
    final String caseIdentifier = command.getCaseIdentifier();
    final DataContextOfAction dataContextOfAction = dataContextService.checkedGetDataContext(
        productIdentifier, caseIdentifier, command.getCommand().getOneTimeAccountAssignments());
    IndividualLendingPatternFactory.checkActionCanBeExecuted(Case.State.valueOf(dataContextOfAction.getCustomerCaseEntity().getCurrentState()), Action.CLOSE);

    checkIfTasksAreOutstanding(dataContextOfAction, Action.CLOSE);

    final DesignatorToAccountIdentifierMapper designatorToAccountIdentifierMapper
        = new DesignatorToAccountIdentifierMapper(dataContextOfAction);
    final RealRunningBalances runningBalances = new RealRunningBalances(
        accountingAdapter,
        designatorToAccountIdentifierMapper);

    final PaymentBuilder paymentBuilder =
        closePaymentBuilderService.getPaymentBuilder(dataContextOfAction, BigDecimal.ZERO, CostComponentService.today(), runningBalances);

    final List<ChargeInstance> charges = paymentBuilder.buildCharges(Action.CLOSE, designatorToAccountIdentifierMapper);

    final LocalDateTime today = today();

    accountingAdapter.bookCharges(charges,
        command.getCommand().getNote(),
        command.getCommand().getCreatedOn(),
        dataContextOfAction.getMessageForCharge(Action.CLOSE),
        Action.CLOSE.getTransactionType());

    final CaseEntity customerCase = dataContextOfAction.getCustomerCaseEntity();
    customerCase.setCurrentState(Case.State.CLOSED.name());
    caseRepository.save(customerCase);

    return new IndividualLoanCommandEvent(productIdentifier, caseIdentifier, DateConverter.toIsoString(today));
  }

  @Transactional
  @CommandHandler(logStart = CommandLogLevel.INFO, logFinish = CommandLogLevel.INFO)
  @EventEmitter(selectorName = EventConstants.SELECTOR_NAME, selectorValue = IndividualLoanEventConstants.RECOVER_INDIVIDUALLOAN_CASE)
  public IndividualLoanCommandEvent process(final RecoverCommand command) {
    final String productIdentifier = command.getProductIdentifier();
    final String caseIdentifier = command.getCaseIdentifier();
    final DataContextOfAction dataContextOfAction = dataContextService.checkedGetDataContext(
        productIdentifier, caseIdentifier, command.getCommand().getOneTimeAccountAssignments());
    IndividualLendingPatternFactory.checkActionCanBeExecuted(Case.State.valueOf(dataContextOfAction.getCustomerCaseEntity().getCurrentState()), Action.RECOVER);

    checkIfTasksAreOutstanding(dataContextOfAction, Action.RECOVER);

    final LocalDateTime today = today();

    final CaseEntity customerCase = dataContextOfAction.getCustomerCaseEntity();
    customerCase.setCurrentState(Case.State.CLOSED.name());
    caseRepository.save(customerCase);

    return new IndividualLoanCommandEvent(productIdentifier, caseIdentifier, DateConverter.toIsoString(today));
  }

  private Map<String, BigDecimal> getRequestedChargeAmounts(final @Nullable List<CostComponent> costComponents) {
    if (costComponents == null)
      return Collections.emptyMap();
    else
      return costComponents.stream()
          .collect(Collectors.groupingBy(
              CostComponent::getChargeIdentifier,
              Collectors.reducing(BigDecimal.ZERO,
                  CostComponent::getAmount,
                  BigDecimal::add)));
  }

  private void checkIfTasksAreOutstanding(final DataContextOfAction dataContextOfAction, final Action action) {
    final String productIdentifier = dataContextOfAction.getProductEntity().getIdentifier();
    final String caseIdentifier = dataContextOfAction.getCustomerCaseEntity().getIdentifier();
    final boolean tasksOutstanding = taskInstanceRepository.areTasksOutstanding(
        productIdentifier, caseIdentifier, action.name());
    if (tasksOutstanding)
      throw ServiceException.conflict("Cannot execute action ''{0}'' for case ''{1}.{2}'' because tasks are incomplete.",
          action.name(), productIdentifier, caseIdentifier);
  }

  private static LocalDateTime today() {
    return LocalDate.now(Clock.systemUTC()).atStartOfDay();
  }
}