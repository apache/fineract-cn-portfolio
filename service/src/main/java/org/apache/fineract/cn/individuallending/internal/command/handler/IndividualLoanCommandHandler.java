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
package org.apache.fineract.cn.individuallending.internal.command.handler;


import org.apache.fineract.cn.individuallending.IndividualLendingPatternFactory;
import org.apache.fineract.cn.individuallending.api.v1.domain.product.AccountDesignators;
import org.apache.fineract.cn.individuallending.api.v1.domain.workflow.Action;
import org.apache.fineract.cn.individuallending.api.v1.events.IndividualLoanCommandEvent;
import org.apache.fineract.cn.individuallending.api.v1.events.IndividualLoanEventConstants;
import org.apache.fineract.cn.individuallending.internal.command.AcceptPaymentCommand;
import org.apache.fineract.cn.individuallending.internal.command.ApplyInterestCommand;
import org.apache.fineract.cn.individuallending.internal.command.ApproveCommand;
import org.apache.fineract.cn.individuallending.internal.command.CloseCommand;
import org.apache.fineract.cn.individuallending.internal.command.DenyCommand;
import org.apache.fineract.cn.individuallending.internal.command.DisburseCommand;
import org.apache.fineract.cn.individuallending.internal.command.ImportCommand;
import org.apache.fineract.cn.individuallending.internal.command.MarkInArrearsCommand;
import org.apache.fineract.cn.individuallending.internal.command.MarkLateCommand;
import org.apache.fineract.cn.individuallending.internal.command.OpenCommand;
import org.apache.fineract.cn.individuallending.internal.command.RecoverCommand;
import org.apache.fineract.cn.individuallending.internal.command.WriteOffCommand;
import org.apache.fineract.cn.individuallending.internal.repository.CaseParametersEntity;
import org.apache.fineract.cn.individuallending.internal.repository.CaseParametersRepository;
import org.apache.fineract.cn.individuallending.internal.repository.LateCaseEntity;
import org.apache.fineract.cn.individuallending.internal.repository.LateCaseRepository;
import org.apache.fineract.cn.individuallending.internal.service.DataContextOfAction;
import org.apache.fineract.cn.individuallending.internal.service.DataContextService;
import org.apache.fineract.cn.individuallending.internal.service.DesignatorToAccountIdentifierMapper;
import org.apache.fineract.cn.individuallending.internal.service.costcomponent.AcceptPaymentBuilderService;
import org.apache.fineract.cn.individuallending.internal.service.costcomponent.ApplyInterestPaymentBuilderService;
import org.apache.fineract.cn.individuallending.internal.service.costcomponent.ApprovePaymentBuilderService;
import org.apache.fineract.cn.individuallending.internal.service.costcomponent.ClosePaymentBuilderService;
import org.apache.fineract.cn.individuallending.internal.service.costcomponent.CostComponentService;
import org.apache.fineract.cn.individuallending.internal.service.costcomponent.DenyPaymentBuilderService;
import org.apache.fineract.cn.individuallending.internal.service.costcomponent.DisbursePaymentBuilderService;
import org.apache.fineract.cn.individuallending.internal.service.costcomponent.MarkInArrearsPaymentBuilderService;
import org.apache.fineract.cn.individuallending.internal.service.costcomponent.MarkLatePaymentBuilderService;
import org.apache.fineract.cn.individuallending.internal.service.costcomponent.OpenPaymentBuilderService;
import org.apache.fineract.cn.individuallending.internal.service.costcomponent.PaymentBuilder;
import org.apache.fineract.cn.individuallending.internal.service.costcomponent.RealRunningBalances;
import org.apache.fineract.cn.individuallending.internal.service.costcomponent.RecoverPaymentBuilderService;
import org.apache.fineract.cn.individuallending.internal.service.costcomponent.WriteOffPaymentBuilderService;
import org.apache.fineract.cn.individuallending.internal.service.schedule.ScheduledActionHelpers;
import org.apache.fineract.cn.portfolio.api.v1.domain.AccountAssignment;
import org.apache.fineract.cn.portfolio.api.v1.domain.Case;
import org.apache.fineract.cn.portfolio.api.v1.domain.CostComponent;
import org.apache.fineract.cn.portfolio.service.internal.mapper.CaseMapper;
import org.apache.fineract.cn.portfolio.service.internal.repository.CaseCommandEntity;
import org.apache.fineract.cn.portfolio.service.internal.repository.CaseCommandRepository;
import org.apache.fineract.cn.portfolio.service.internal.repository.CaseEntity;
import org.apache.fineract.cn.portfolio.service.internal.repository.CaseRepository;
import org.apache.fineract.cn.portfolio.service.internal.repository.TaskInstanceRepository;
import org.apache.fineract.cn.portfolio.service.internal.util.AccountingAdapter;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import org.apache.fineract.cn.api.util.UserContextHolder;
import org.apache.fineract.cn.command.annotation.Aggregate;
import org.apache.fineract.cn.command.annotation.CommandHandler;
import org.apache.fineract.cn.command.annotation.CommandLogLevel;
import org.apache.fineract.cn.command.annotation.EventEmitter;
import org.apache.fineract.cn.lang.DateConverter;
import org.apache.fineract.cn.lang.ServiceException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

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
  private final MarkInArrearsPaymentBuilderService markInArrearsPaymentBuilderService;
  private final WriteOffPaymentBuilderService writeOffPaymentBuilderService;
  private final RecoverPaymentBuilderService recoverPaymentBuilderService;
  private final AccountingAdapter accountingAdapter;
  private final CaseCommandRepository caseCommandRepository;
  private final TaskInstanceRepository taskInstanceRepository;
  private final CaseParametersRepository caseParametersRepository;
  private final LateCaseRepository lateCaseRepository;

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
      final MarkInArrearsPaymentBuilderService markInArrearsPaymentBuilderService,
      final WriteOffPaymentBuilderService writeOffPaymentBuilderService,
      final RecoverPaymentBuilderService recoverPaymentBuilderService,
      final AccountingAdapter accountingAdapter,
      final CaseCommandRepository caseCommandRepository,
      final TaskInstanceRepository taskInstanceRepository,
      final CaseParametersRepository caseParametersRepository,
      final LateCaseRepository lateCaseRepository) {
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
    this.markInArrearsPaymentBuilderService = markInArrearsPaymentBuilderService;
    this.writeOffPaymentBuilderService = writeOffPaymentBuilderService;
    this.recoverPaymentBuilderService = recoverPaymentBuilderService;
    this.accountingAdapter = accountingAdapter;
    this.caseCommandRepository = caseCommandRepository;
    this.taskInstanceRepository = taskInstanceRepository;
    this.caseParametersRepository = caseParametersRepository;
    this.lateCaseRepository = lateCaseRepository;
  }

  @Transactional
  @CommandHandler(logStart = CommandLogLevel.INFO, logFinish = CommandLogLevel.INFO)
  @EventEmitter(
      selectorName = IndividualLoanEventConstants.SELECTOR_NAME,
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
        dataContextOfAction);

    final PaymentBuilder paymentBuilder
        = openPaymentBuilderService.getPaymentBuilder(dataContextOfAction, BigDecimal.ZERO, CostComponentService.today(), runningBalances);


    final Optional<String> transactionUniqueifier = accountingAdapter.bookCharges(paymentBuilder.getBalanceAdjustments(),
        designatorToAccountIdentifierMapper,
        command.getCommand().getNote(),
        command.getCommand().getCreatedOn(),
        dataContextOfAction.getMessageForCharge(Action.OPEN),
        Action.OPEN.getTransactionType());

    final CaseEntity customerCase = dataContextOfAction.getCustomerCaseEntity();

    recordCommand(
        command.getCommand().getCreatedOn(),
        customerCase.getId(),
        Action.OPEN,
        transactionUniqueifier);

    //Only move to new state if book charges command was accepted.
    customerCase.setCurrentState(Case.State.PENDING.name());
    caseRepository.save(customerCase);

    return new IndividualLoanCommandEvent(productIdentifier, caseIdentifier, command.getCommand().getCreatedOn());
  }


  @Transactional
  @CommandHandler(logStart = CommandLogLevel.INFO, logFinish = CommandLogLevel.INFO)
  @EventEmitter(
      selectorName = IndividualLoanEventConstants.SELECTOR_NAME,
      selectorValue = IndividualLoanEventConstants.IMPORT_INDIVIDUALLOAN_CASE)
  public IndividualLoanCommandEvent process(final ImportCommand command) throws InterruptedException {
    final String productIdentifier = command.getProductIdentifier();
    final String caseIdentifier = command.getCaseIdentifier();
    final DataContextOfAction dataContextOfAction = dataContextService.checkedGetDataContext(
        productIdentifier, caseIdentifier, command.getImportParameters().getCaseAccountAssignments());
    IndividualLendingPatternFactory.checkActionCanBeExecuted(Case.State.valueOf(dataContextOfAction.getCustomerCaseEntity().getCurrentState()), Action.IMPORT);

    checkIfTasksAreOutstanding(dataContextOfAction, Action.IMPORT);

    final DesignatorToAccountIdentifierMapper designatorToAccountIdentifierMapper
        = new DesignatorToAccountIdentifierMapper(dataContextOfAction);
    createAccounts(dataContextOfAction, designatorToAccountIdentifierMapper, command.getImportParameters().getCurrentBalances());

    final CaseEntity customerCase = dataContextOfAction.getCustomerCaseEntity();

    recordCommand(
        command.getImportParameters().getStartOfTerm(),
        customerCase.getId(),
        Action.DISBURSE,
        Optional.empty());

    recordCommand(
        command.getImportParameters().getCreatedOn(),
        customerCase.getId(),
        Action.IMPORT,
        Optional.empty());


    final LocalDate startOfTerm = DateConverter.fromIsoString(command.getImportParameters().getStartOfTerm()).toLocalDate();
    final LocalDateTime endOfTerm = ScheduledActionHelpers.getRoughEndDate(startOfTerm, dataContextOfAction.getCaseParameters())
        .atTime(LocalTime.MIDNIGHT);
    customerCase.setStartOfTerm(startOfTerm.atTime(LocalTime.MIDNIGHT));
    customerCase.setEndOfTerm(endOfTerm);
    customerCase.setCurrentState(Case.State.ACTIVE.name());
    caseRepository.save(customerCase);

    final CaseParametersEntity caseParameters = dataContextOfAction.getCaseParametersEntity();
    caseParameters.setPaymentSize(command.getImportParameters().getPaymentSize());
    caseParametersRepository.save(caseParameters);

    return new IndividualLoanCommandEvent(productIdentifier, caseIdentifier, command.getImportParameters().getCreatedOn());
  }

  @Transactional
  @CommandHandler(logStart = CommandLogLevel.INFO, logFinish = CommandLogLevel.INFO)
  @EventEmitter(
      selectorName = IndividualLoanEventConstants.SELECTOR_NAME,
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
        dataContextOfAction);

    final PaymentBuilder paymentBuilder
        = denyPaymentBuilderService.getPaymentBuilder(dataContextOfAction, BigDecimal.ZERO, CostComponentService.today(), runningBalances);


    final Optional<String> transactionUniqueifier = accountingAdapter.bookCharges(paymentBuilder.getBalanceAdjustments(),
        designatorToAccountIdentifierMapper,
        command.getCommand().getNote(),
        command.getCommand().getCreatedOn(),
        dataContextOfAction.getMessageForCharge(Action.DENY),
        Action.DENY.getTransactionType());

    final CaseEntity customerCase = dataContextOfAction.getCustomerCaseEntity();

    recordCommand(
        command.getCommand().getCreatedOn(),
        customerCase.getId(),
        Action.DENY,
        transactionUniqueifier);

    customerCase.setCurrentState(Case.State.CLOSED.name());
    caseRepository.save(customerCase);

    return new IndividualLoanCommandEvent(productIdentifier, caseIdentifier, command.getCommand().getCreatedOn());
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
      selectorName = IndividualLoanEventConstants.SELECTOR_NAME,
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
    createAccounts(dataContextOfAction, designatorToAccountIdentifierMapper, Collections.emptyMap());


    final RealRunningBalances runningBalances = new RealRunningBalances(
        accountingAdapter,
        dataContextOfAction);

    final PaymentBuilder paymentBuilder =
        approvePaymentBuilderService.getPaymentBuilder(dataContextOfAction, BigDecimal.ZERO, CostComponentService.today(), runningBalances);

    final Optional<String> transactionUniqueifier = accountingAdapter.bookCharges(paymentBuilder.getBalanceAdjustments(),
        designatorToAccountIdentifierMapper,
        command.getCommand().getNote(),
        command.getCommand().getCreatedOn(),
        dataContextOfAction.getMessageForCharge(Action.APPROVE),
        Action.APPROVE.getTransactionType());

    final CaseEntity customerCase = dataContextOfAction.getCustomerCaseEntity();

    recordCommand(
        command.getCommand().getCreatedOn(),
        customerCase.getId(),
        Action.APPROVE,
        transactionUniqueifier);

    //Only move to new state if book charges command was accepted.
    customerCase.setCurrentState(Case.State.APPROVED.name());
    caseRepository.save(customerCase);

    return new IndividualLoanCommandEvent(productIdentifier, caseIdentifier, command.getCommand().getCreatedOn());
  }

  @Transactional
  @CommandHandler(logStart = CommandLogLevel.INFO, logFinish = CommandLogLevel.INFO)
  @EventEmitter(selectorName = IndividualLoanEventConstants.SELECTOR_NAME, selectorValue = IndividualLoanEventConstants.DISBURSE_INDIVIDUALLOAN_CASE)
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
        dataContextOfAction);

    final PaymentBuilder paymentBuilder =
        disbursePaymentBuilderService.getPaymentBuilder(dataContextOfAction, disbursalAmount, CostComponentService.today(), runningBalances);

    final Optional<String> transactionUniqueifier = accountingAdapter.bookCharges(paymentBuilder.getBalanceAdjustments(),
        designatorToAccountIdentifierMapper,
        command.getCommand().getNote(),
        command.getCommand().getCreatedOn(),
        dataContextOfAction.getMessageForCharge(Action.DISBURSE),
        Action.DISBURSE.getTransactionType());

    final CaseEntity customerCase = dataContextOfAction.getCustomerCaseEntity();

    recordCommand(
        command.getCommand().getCreatedOn(),
        customerCase.getId(),
        Action.DISBURSE,
        transactionUniqueifier);

    //TODO: Only move to new state if book charges command was accepted.
    if (Case.State.valueOf(dataContextOfAction.getCustomerCaseEntity().getCurrentState()) != Case.State.ACTIVE) {
      final LocalDate startOfTerm = DateConverter.fromIsoString(command.getCommand().getCreatedOn()).toLocalDate();
      final LocalDateTime endOfTerm = ScheduledActionHelpers.getRoughEndDate(startOfTerm, dataContextOfAction.getCaseParameters())
          .atTime(LocalTime.MIDNIGHT);
      customerCase.setStartOfTerm(startOfTerm.atTime(LocalTime.MIDNIGHT));
      customerCase.setEndOfTerm(endOfTerm);
      customerCase.setCurrentState(Case.State.ACTIVE.name());
      caseRepository.save(customerCase);
    }
    final BigDecimal currentBalance = runningBalances.getBalance(AccountDesignators.CUSTOMER_LOAN_GROUP).orElse(BigDecimal.ZERO);

    final BigDecimal newLoanPaymentSize = disbursePaymentBuilderService.getLoanPaymentSizeForSingleDisbursement(
        currentBalance.add(paymentBuilder.getBalanceAdjustment(AccountDesignators.ENTRY)),
        dataContextOfAction);

    dataContextOfAction.getCaseParametersEntity().setPaymentSize(newLoanPaymentSize);
    caseParametersRepository.save(dataContextOfAction.getCaseParametersEntity());

    return new IndividualLoanCommandEvent(productIdentifier, caseIdentifier, command.getCommand().getCreatedOn());
  }

  @Transactional
  @CommandHandler(logStart = CommandLogLevel.INFO, logFinish = CommandLogLevel.INFO)
  @EventEmitter(
      selectorName = IndividualLoanEventConstants.SELECTOR_NAME,
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
        dataContextOfAction);

    final PaymentBuilder paymentBuilder =
        applyInterestPaymentBuilderService.getPaymentBuilder(dataContextOfAction, BigDecimal.ZERO, CostComponentService.today(), runningBalances);

    final Optional<String> transactionUniqueifier = accountingAdapter.bookCharges(paymentBuilder.getBalanceAdjustments(),
        designatorToAccountIdentifierMapper,
        "Applied interest on " + command.getForTime(),
        command.getForTime(),
        dataContextOfAction.getMessageForCharge(Action.APPLY_INTEREST),
        Action.APPLY_INTEREST.getTransactionType());

    final CaseEntity customerCase = dataContextOfAction.getCustomerCaseEntity();

    recordCommand(
        command.getForTime(),
        customerCase.getId(),
        Action.APPLY_INTEREST,
        transactionUniqueifier);

    return new IndividualLoanCommandEvent(productIdentifier, caseIdentifier, command.getForTime());
  }

  @Transactional
  @CommandHandler(logStart = CommandLogLevel.INFO, logFinish = CommandLogLevel.INFO)
  @EventEmitter(
      selectorName = IndividualLoanEventConstants.SELECTOR_NAME,
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
        dataContextOfAction);

    final PaymentBuilder paymentBuilder =
        acceptPaymentBuilderService.getPaymentBuilder(
            dataContextOfAction,
            command.getCommand().getPaymentSize(),
            DateConverter.fromIsoString(command.getCommand().getCreatedOn()).toLocalDate(), runningBalances);

    final Optional<String> transactionUniqueifier = accountingAdapter.bookCharges(paymentBuilder.getBalanceAdjustments(),
        designatorToAccountIdentifierMapper,
        command.getCommand().getNote(),
        command.getCommand().getCreatedOn(),
        dataContextOfAction.getMessageForCharge(Action.ACCEPT_PAYMENT),
        Action.ACCEPT_PAYMENT.getTransactionType());

    final CaseEntity customerCase = dataContextOfAction.getCustomerCaseEntity();

    recordCommand(
        command.getCommand().getCreatedOn(),
        customerCase.getId(),
        Action.ACCEPT_PAYMENT,
        transactionUniqueifier);

    //TODO: Should this be more sophisticated?  Take into account what the payment amount was?
    markCaseNotLate(dataContextOfAction);

    return new IndividualLoanCommandEvent(productIdentifier, caseIdentifier, command.getCommand().getCreatedOn());
  }

  @Transactional
  @CommandHandler(logStart = CommandLogLevel.INFO, logFinish = CommandLogLevel.INFO)
  @EventEmitter(
      selectorName = IndividualLoanEventConstants.SELECTOR_NAME,
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
        dataContextOfAction);

    final PaymentBuilder paymentBuilder =
        markLatePaymentBuilderService.getPaymentBuilder(dataContextOfAction, BigDecimal.ZERO, DateConverter.fromIsoString(command.getForTime()).toLocalDate(),
            runningBalances);

    final Optional<String> transactionUniqueifier = accountingAdapter.bookCharges(paymentBuilder.getBalanceAdjustments(),
        designatorToAccountIdentifierMapper,
        "Marked late on " + command.getForTime(),
        command.getForTime(),
        dataContextOfAction.getMessageForCharge(Action.MARK_LATE),
        Action.MARK_LATE.getTransactionType());

    final CaseEntity customerCase = dataContextOfAction.getCustomerCaseEntity();

    recordCommand(
        command.getForTime(),
        customerCase.getId(),
        Action.MARK_LATE,
        transactionUniqueifier);

    markCaseLate(dataContextOfAction, command.getForTime());

    return new IndividualLoanCommandEvent(productIdentifier, caseIdentifier, command.getForTime());
  }

  @Transactional
  @CommandHandler(logStart = CommandLogLevel.INFO, logFinish = CommandLogLevel.INFO)
  @EventEmitter(
      selectorName = IndividualLoanEventConstants.SELECTOR_NAME,
      selectorValue = IndividualLoanEventConstants.MARK_IN_ARREARS_INDIVIDUALLOAN_CASE)
  public IndividualLoanCommandEvent process(final MarkInArrearsCommand command) {
    final String productIdentifier = command.getProductIdentifier();
    final String caseIdentifier = command.getCaseIdentifier();
    final DataContextOfAction dataContextOfAction = dataContextService.checkedGetDataContext(
        productIdentifier, caseIdentifier, Collections.emptyList());
    IndividualLendingPatternFactory.checkActionCanBeExecuted(Case.State.valueOf(dataContextOfAction.getCustomerCaseEntity().getCurrentState()), Action.MARK_LATE);

    checkIfTasksAreOutstanding(dataContextOfAction, Action.MARK_IN_ARREARS);

    if (dataContextOfAction.getCustomerCaseEntity().getEndOfTerm() == null)
      throw ServiceException.internalError(
          "End of term not set for active case ''{0}.{1}.''", productIdentifier, caseIdentifier);

    final DesignatorToAccountIdentifierMapper designatorToAccountIdentifierMapper
        = new DesignatorToAccountIdentifierMapper(dataContextOfAction);
    final RealRunningBalances runningBalances = new RealRunningBalances(
        accountingAdapter,
        dataContextOfAction);

    final PaymentBuilder paymentBuilder =
        markInArrearsPaymentBuilderService.getPaymentBuilder(
            dataContextOfAction,
            BigDecimal.valueOf(command.getDaysLate()),
            DateConverter.fromIsoString(command.getForTime()).toLocalDate(),
            runningBalances);

    final Optional<String> transactionUniqueifier = accountingAdapter.bookCharges(paymentBuilder.getBalanceAdjustments(),
        designatorToAccountIdentifierMapper,
        "Marked in arrears on " + command.getForTime(),
        command.getForTime(),
        dataContextOfAction.getMessageForCharge(Action.MARK_IN_ARREARS),
        Action.MARK_IN_ARREARS.getTransactionType());

    final CaseEntity customerCase = dataContextOfAction.getCustomerCaseEntity();

    recordCommand(
        command.getForTime(),
        customerCase.getId(),
        Action.MARK_IN_ARREARS,
        transactionUniqueifier);

    return new IndividualLoanCommandEvent(productIdentifier, caseIdentifier, command.getForTime());
  }

  @Transactional
  @CommandHandler(logStart = CommandLogLevel.INFO, logFinish = CommandLogLevel.INFO)
  @EventEmitter(selectorName = IndividualLoanEventConstants.SELECTOR_NAME, selectorValue = IndividualLoanEventConstants.WRITE_OFF_INDIVIDUALLOAN_CASE)
  public IndividualLoanCommandEvent process(final WriteOffCommand command) {
    final String productIdentifier = command.getProductIdentifier();
    final String caseIdentifier = command.getCaseIdentifier();
    final DataContextOfAction dataContextOfAction = dataContextService.checkedGetDataContext(
        productIdentifier, caseIdentifier, command.getCommand().getOneTimeAccountAssignments());
    IndividualLendingPatternFactory.checkActionCanBeExecuted(Case.State.valueOf(dataContextOfAction.getCustomerCaseEntity().getCurrentState()), Action.WRITE_OFF);

    checkIfTasksAreOutstanding(dataContextOfAction, Action.WRITE_OFF);
    final DesignatorToAccountIdentifierMapper designatorToAccountIdentifierMapper
        = new DesignatorToAccountIdentifierMapper(dataContextOfAction);
    final RealRunningBalances runningBalances = new RealRunningBalances(
        accountingAdapter,
        dataContextOfAction);

    final PaymentBuilder paymentBuilder =
        writeOffPaymentBuilderService.getPaymentBuilder(
            dataContextOfAction,
            command.getCommand().getPaymentSize(),
            DateConverter.fromIsoString(command.getCommand().getCreatedOn()).toLocalDate(), runningBalances);

    final Optional<String> transactionUniqueifier = accountingAdapter.bookCharges(paymentBuilder.getBalanceAdjustments(),
        designatorToAccountIdentifierMapper,
        command.getCommand().getNote(),
        command.getCommand().getCreatedOn(),
        dataContextOfAction.getMessageForCharge(Action.WRITE_OFF),
        Action.WRITE_OFF.getTransactionType());

    final CaseEntity customerCase = dataContextOfAction.getCustomerCaseEntity();

    recordCommand(
        command.getCommand().getCreatedOn(),
        customerCase.getId(),
        Action.WRITE_OFF,
        transactionUniqueifier);

    customerCase.setCurrentState(Case.State.CLOSED.name());
    caseRepository.save(customerCase);

    return new IndividualLoanCommandEvent(productIdentifier, caseIdentifier, command.getCommand().getCreatedOn());
  }

  @Transactional
  @CommandHandler(logStart = CommandLogLevel.INFO, logFinish = CommandLogLevel.INFO)
  @EventEmitter(selectorName = IndividualLoanEventConstants.SELECTOR_NAME, selectorValue = IndividualLoanEventConstants.CLOSE_INDIVIDUALLOAN_CASE)
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
        dataContextOfAction);

    final PaymentBuilder paymentBuilder =
        closePaymentBuilderService.getPaymentBuilder(dataContextOfAction, BigDecimal.ZERO, CostComponentService.today(), runningBalances);

    final Optional<String> transactionIdentifier = accountingAdapter.bookCharges(paymentBuilder.getBalanceAdjustments(),
        designatorToAccountIdentifierMapper,
        command.getCommand().getNote(),
        command.getCommand().getCreatedOn(),
        dataContextOfAction.getMessageForCharge(Action.CLOSE),
        Action.CLOSE.getTransactionType());

    final CaseEntity customerCase = dataContextOfAction.getCustomerCaseEntity();
    customerCase.setCurrentState(Case.State.CLOSED.name());
    caseRepository.save(customerCase);

    return new IndividualLoanCommandEvent(productIdentifier, caseIdentifier, command.getCommand().getCreatedOn());
  }

  @Transactional
  @CommandHandler(logStart = CommandLogLevel.INFO, logFinish = CommandLogLevel.INFO)
  @EventEmitter(selectorName = IndividualLoanEventConstants.SELECTOR_NAME, selectorValue = IndividualLoanEventConstants.RECOVER_INDIVIDUALLOAN_CASE)
  public IndividualLoanCommandEvent process(final RecoverCommand command) {
    final String productIdentifier = command.getProductIdentifier();
    final String caseIdentifier = command.getCaseIdentifier();
    final DataContextOfAction dataContextOfAction = dataContextService.checkedGetDataContext(
        productIdentifier, caseIdentifier, command.getCommand().getOneTimeAccountAssignments());
    IndividualLendingPatternFactory.checkActionCanBeExecuted(Case.State.valueOf(dataContextOfAction.getCustomerCaseEntity().getCurrentState()), Action.RECOVER);

    checkIfTasksAreOutstanding(dataContextOfAction, Action.RECOVER);

    final DesignatorToAccountIdentifierMapper designatorToAccountIdentifierMapper
        = new DesignatorToAccountIdentifierMapper(dataContextOfAction);
    final RealRunningBalances runningBalances = new RealRunningBalances(
        accountingAdapter,
        dataContextOfAction);

    final PaymentBuilder paymentBuilder =
        recoverPaymentBuilderService.getPaymentBuilder(dataContextOfAction, BigDecimal.ZERO, CostComponentService.today(), runningBalances);

    final Optional<String> transactionUniqueifier = accountingAdapter.bookCharges(paymentBuilder.getBalanceAdjustments(),
        designatorToAccountIdentifierMapper,
        command.getCommand().getNote(),
        command.getCommand().getCreatedOn(),
        dataContextOfAction.getMessageForCharge(Action.RECOVER),
        Action.CLOSE.getTransactionType());

    final CaseEntity customerCase = dataContextOfAction.getCustomerCaseEntity();

    recordCommand(
        command.getCommand().getCreatedOn(),
        customerCase.getId(),
        Action.RECOVER,
        transactionUniqueifier);

    customerCase.setCurrentState(Case.State.CLOSED.name());
    caseRepository.save(customerCase);

    return new IndividualLoanCommandEvent(productIdentifier, caseIdentifier, command.getCommand().getCreatedOn());
  }

  private void createAccounts(
      final DataContextOfAction dataContextOfAction,
      final DesignatorToAccountIdentifierMapper designatorToAccountIdentifierMapper,
      final Map<String, BigDecimal> currentBalances) throws InterruptedException
  {
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
        .map(ledger -> {
          final BigDecimal currentBalance = currentBalances.getOrDefault(ledger.getDesignator(), BigDecimal.ZERO);
          return new AccountAssignment(ledger.getDesignator(),
              accountingAdapter.createOrFindCaseAccountForLedgerAssignment(
                  dataContextOfAction.getCaseParametersEntity().getCustomerIdentifier(),
                  ledger,
                  currentBalance));})
        .map(accountAssignment -> CaseMapper.map(accountAssignment, dataContextOfAction.getCustomerCaseEntity()))
        .forEach(caseAccountAssignmentEntity ->
            dataContextOfAction.getCustomerCaseEntity().getAccountAssignments().add(caseAccountAssignmentEntity)
        );
    caseRepository.save(dataContextOfAction.getCustomerCaseEntity());
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

  private void recordCommand(
      final String when,
      final Long caseId,
      final Action action,
      @SuppressWarnings("OptionalUsedAsFieldOrParameterType") final Optional<String> transactionUniqueifier) {
    final CaseCommandEntity caseCommandEntity = new CaseCommandEntity();
    caseCommandEntity.setCaseId(caseId);
    caseCommandEntity.setActionName(action.name());
    caseCommandEntity.setCreatedBy(UserContextHolder.checkedGetUser());
    caseCommandEntity.setCreatedOn(DateConverter.fromIsoString(when));
    caseCommandEntity.setTransactionUniqueifier(transactionUniqueifier.orElse(""));
    caseCommandRepository.save(caseCommandEntity);
  }

  private void markCaseLate(
      final DataContextOfAction dataContextOfAction,
      final String forTime) {
    final Optional<LateCaseEntity> lateCaseEntity = lateCaseRepository.findByCaseId(dataContextOfAction.getCustomerCaseEntity().getId());
    if (!lateCaseEntity.isPresent()) {
      final LateCaseEntity markCaseLate = new LateCaseEntity();
      markCaseLate.setCaseId(dataContextOfAction.getCustomerCaseEntity().getId());
      markCaseLate.setLateSince(DateConverter.fromIsoString(forTime));
      lateCaseRepository.save(markCaseLate);
    }
  }

  private void markCaseNotLate(
      final DataContextOfAction dataContextOfAction) {
    lateCaseRepository.deleteByCaseId(dataContextOfAction.getCustomerCaseEntity().getId());
  }
}