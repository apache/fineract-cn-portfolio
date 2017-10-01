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
import io.mifos.core.command.internal.CommandBus;
import io.mifos.core.lang.ApplicationName;
import io.mifos.core.lang.DateConverter;
import io.mifos.core.lang.ServiceException;
import io.mifos.individuallending.api.v1.domain.product.AccountDesignators;
import io.mifos.individuallending.api.v1.domain.workflow.Action;
import io.mifos.individuallending.api.v1.events.IndividualLoanCommandEvent;
import io.mifos.individuallending.api.v1.events.IndividualLoanEventConstants;
import io.mifos.individuallending.internal.command.ApplyInterestCommand;
import io.mifos.individuallending.internal.command.CheckLateCommand;
import io.mifos.individuallending.internal.command.MarkLateCommand;
import io.mifos.individuallending.internal.service.*;
import io.mifos.individuallending.internal.service.DataContextOfAction;
import io.mifos.individuallending.internal.service.schedule.Period;
import io.mifos.individuallending.internal.service.schedule.ScheduledActionHelpers;
import io.mifos.portfolio.api.v1.domain.Case;
import io.mifos.portfolio.service.config.PortfolioProperties;
import io.mifos.portfolio.service.internal.command.CreateBeatPublishCommand;
import io.mifos.portfolio.service.internal.repository.CaseCommandEntity;
import io.mifos.portfolio.service.internal.repository.CaseCommandRepository;
import io.mifos.portfolio.service.internal.repository.CaseEntity;
import io.mifos.portfolio.service.internal.repository.CaseRepository;
import io.mifos.portfolio.service.internal.util.AccountingAdapter;
import io.mifos.rhythm.spi.v1.domain.BeatPublish;
import io.mifos.rhythm.spi.v1.events.BeatPublishEvent;
import io.mifos.rhythm.spi.v1.events.EventConstants;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @author Myrle Krantz
 */
@SuppressWarnings("unused")
@Aggregate
public class BeatPublishCommandHandler {
  private final CaseRepository caseRepository;
  private final CaseCommandRepository caseCommandRepository;
  private final PortfolioProperties portfolioProperties;
  private final DataContextService dataContextService;
  private final ApplicationName applicationName;
  private final CommandBus commandBus;
  private final AccountingAdapter accountingAdapter;

  @Autowired
  public BeatPublishCommandHandler(
      final CaseRepository caseRepository,
      final CaseCommandRepository caseCommandRepository,
      final PortfolioProperties portfolioProperties,
      final DataContextService dataContextService,
      final ApplicationName applicationName,
      final CommandBus commandBus,
      final AccountingAdapter accountingAdapter) {
    this.caseRepository = caseRepository;
    this.caseCommandRepository = caseCommandRepository;
    this.portfolioProperties = portfolioProperties;
    this.dataContextService = dataContextService;
    this.applicationName = applicationName;
    this.commandBus = commandBus;
    this.accountingAdapter = accountingAdapter;
  }

  @Transactional
  @CommandHandler(logStart = CommandLogLevel.INFO, logFinish = CommandLogLevel.INFO)
  @EventEmitter(selectorName = EventConstants.SELECTOR_NAME, selectorValue = EventConstants.POST_PUBLISHEDBEAT)
  public BeatPublishEvent process(final CreateBeatPublishCommand createBeatPublishCommand) {
    final BeatPublish instance = createBeatPublishCommand.getInstance();
    final LocalDateTime forTime = DateConverter.fromIsoString(instance.getForTime());
    if (portfolioProperties.getBookInterestInTimeSlot() == forTime.getHour())
    {
      final Stream<CaseEntity> activeCases = caseRepository.findByCurrentStateIn(Collections.singleton(Case.State.ACTIVE.name()));
      activeCases.forEach(activeCase -> {
        final ApplyInterestCommand applyInterestCommand = new ApplyInterestCommand(
            activeCase.getProductIdentifier(),
            activeCase.getIdentifier(),
            instance.getForTime());
        commandBus.dispatch(applyInterestCommand);
      });
    }

    if (portfolioProperties.getCheckForLatenessInTimeSlot() == forTime.getHour())
    {
      final Stream<CaseEntity> activeCases = caseRepository.findByCurrentStateIn(Collections.singleton(Case.State.ACTIVE.name()));
      activeCases.forEach(activeCase -> {
        final CheckLateCommand checkLateCommand = new CheckLateCommand(
            activeCase.getProductIdentifier(),
            activeCase.getIdentifier(),
            instance.getForTime());
        commandBus.dispatch(checkLateCommand);
      });
    }

    return new BeatPublishEvent(applicationName.toString(), instance.getIdentifier(), instance.getForTime());
  }

  @Transactional
  @CommandHandler(logStart = CommandLogLevel.INFO, logFinish = CommandLogLevel.INFO)
  @EventEmitter(
      selectorName = io.mifos.portfolio.api.v1.events.EventConstants.SELECTOR_NAME,
      selectorValue = IndividualLoanEventConstants.CHECK_LATE_INDIVIDUALLOAN_CASE)
  public IndividualLoanCommandEvent process(final CheckLateCommand command) {
    final String productIdentifier = command.getProductIdentifier();
    final String caseIdentifier = command.getCaseIdentifier();
    final LocalDateTime forTime = DateConverter.fromIsoString(command.getForTime());
    final DataContextOfAction dataContextOfAction = dataContextService.checkedGetDataContext(
        productIdentifier, caseIdentifier, Collections.emptyList());

    final DesignatorToAccountIdentifierMapper designatorToAccountIdentifierMapper
        = new DesignatorToAccountIdentifierMapper(dataContextOfAction);
    final String customerLoanPrincipalAccountIdentifier = designatorToAccountIdentifierMapper.mapOrThrow(AccountDesignators.CUSTOMER_LOAN_PRINCIPAL);
    final String customerLoanInterestAccountIdentifier = designatorToAccountIdentifierMapper.mapOrThrow(AccountDesignators.CUSTOMER_LOAN_INTEREST);
    final String lateFeeAccrualAccountIdentifier = designatorToAccountIdentifierMapper.mapOrThrow(AccountDesignators.LATE_FEE_ACCRUAL);

    final BigDecimal currentBalance = accountingAdapter.getCurrentAccountBalance(customerLoanPrincipalAccountIdentifier);
    if (currentBalance.compareTo(BigDecimal.ZERO) == 0) //No late fees if the current balance is zilch.
      return new IndividualLoanCommandEvent(productIdentifier, caseIdentifier, command.getForTime());


    final LocalDateTime dateOfMostRecentDisbursement =
        accountingAdapter.getDateOfMostRecentEntryContainingMessage(customerLoanPrincipalAccountIdentifier, dataContextOfAction.getMessageForCharge(Action.DISBURSE))
            .orElseThrow(() ->
                ServiceException.badRequest("No last disbursal date for ''{0}.{1}'' could be determined.  " +
                    "Therefore it cannot be checked for lateness.", productIdentifier, caseIdentifier));

    final List<Period> repaymentPeriods = ScheduledActionHelpers.generateRepaymentPeriods(
        dateOfMostRecentDisbursement.toLocalDate(),
        forTime.toLocalDate(),
        dataContextOfAction.getCaseParameters())
        .collect(Collectors.toList());

    final long repaymentPeriodsBetweenBeginningAndToday = repaymentPeriods.size() - 1;

    final BigDecimal expectedPaymentSum = dataContextOfAction
        .getCaseParametersEntity()
        .getPaymentSize()
        .multiply(BigDecimal.valueOf(repaymentPeriodsBetweenBeginningAndToday));

    final BigDecimal principalSum = accountingAdapter.sumMatchingEntriesSinceDate(
        customerLoanPrincipalAccountIdentifier,
        dateOfMostRecentDisbursement.toLocalDate(),
        dataContextOfAction.getMessageForCharge(Action.ACCEPT_PAYMENT));
    final BigDecimal interestSum = accountingAdapter.sumMatchingEntriesSinceDate(
        customerLoanInterestAccountIdentifier,
        dateOfMostRecentDisbursement.toLocalDate(),
        dataContextOfAction.getMessageForCharge(Action.ACCEPT_PAYMENT));
    final BigDecimal paymentsSum = principalSum.add(interestSum);

    final BigDecimal lateFeesAccrued = accountingAdapter.sumMatchingEntriesSinceDate(
        lateFeeAccrualAccountIdentifier,
        dateOfMostRecentDisbursement.toLocalDate(),
        dataContextOfAction.getMessageForCharge(Action.MARK_LATE));

    if (paymentsSum.compareTo(expectedPaymentSum) < 0) {
      final Optional<LocalDateTime> dateOfMostRecentLateFee = dateOfMostRecentMarkLate(dataContextOfAction.getCustomerCaseEntity().getId());
      if (!dateOfMostRecentLateFee.isPresent() ||
          mostRecentLateFeeIsBeforeMostRecentRepaymentPeriod(repaymentPeriods, dateOfMostRecentLateFee.get())) {
        commandBus.dispatch(new MarkLateCommand(productIdentifier, caseIdentifier, command.getForTime()));
      }
    }

    return new IndividualLoanCommandEvent(productIdentifier, caseIdentifier, command.getForTime());
  }

  private Optional<LocalDateTime> dateOfMostRecentMarkLate(final Long caseId) {
    final Pageable pageRequest = new PageRequest(0, 10, Sort.Direction.DESC, "createdOn");
    final Page<CaseCommandEntity> page = caseCommandRepository.findByCaseIdAndActionName(
        caseId,
        Action.MARK_LATE.name(),
        pageRequest);

    return page.getContent().stream().findFirst().map(CaseCommandEntity::getCreatedOn);
  }

  private boolean mostRecentLateFeeIsBeforeMostRecentRepaymentPeriod(
      final List<Period> repaymentPeriods,
      final LocalDateTime dateOfMostRecentLateFee) {
    return repaymentPeriods.stream()
        .anyMatch(x -> x.getBeginDate().isAfter(dateOfMostRecentLateFee.toLocalDate()));
  }
}