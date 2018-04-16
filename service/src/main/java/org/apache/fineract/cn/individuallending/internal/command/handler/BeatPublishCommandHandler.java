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

import org.apache.fineract.cn.individuallending.api.v1.domain.product.AccountDesignators;
import org.apache.fineract.cn.individuallending.api.v1.domain.workflow.Action;
import org.apache.fineract.cn.individuallending.api.v1.events.IndividualLoanCommandEvent;
import org.apache.fineract.cn.individuallending.api.v1.events.IndividualLoanEventConstants;
import org.apache.fineract.cn.individuallending.internal.command.ApplyInterestCommand;
import org.apache.fineract.cn.individuallending.internal.command.CheckLateCommand;
import org.apache.fineract.cn.individuallending.internal.command.MarkInArrearsCommand;
import org.apache.fineract.cn.individuallending.internal.command.MarkLateCommand;
import org.apache.fineract.cn.individuallending.internal.repository.LateCaseEntity;
import org.apache.fineract.cn.individuallending.internal.repository.LateCaseRepository;
import org.apache.fineract.cn.individuallending.internal.repository.LossProvisionStepEntity;
import org.apache.fineract.cn.individuallending.internal.repository.LossProvisionStepRepository;
import org.apache.fineract.cn.individuallending.internal.service.DataContextOfAction;
import org.apache.fineract.cn.individuallending.internal.service.DataContextService;
import org.apache.fineract.cn.individuallending.internal.service.costcomponent.RealRunningBalances;
import org.apache.fineract.cn.individuallending.internal.service.schedule.Period;
import org.apache.fineract.cn.individuallending.internal.service.schedule.ScheduledActionHelpers;
import org.apache.fineract.cn.portfolio.api.v1.domain.Case;
import org.apache.fineract.cn.portfolio.service.config.PortfolioProperties;
import org.apache.fineract.cn.portfolio.service.internal.command.CreateBeatPublishCommand;
import org.apache.fineract.cn.portfolio.service.internal.repository.CaseCommandEntity;
import org.apache.fineract.cn.portfolio.service.internal.repository.CaseCommandRepository;
import org.apache.fineract.cn.portfolio.service.internal.repository.CaseEntity;
import org.apache.fineract.cn.portfolio.service.internal.repository.CaseRepository;
import org.apache.fineract.cn.portfolio.service.internal.util.AccountingAdapter;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.fineract.cn.command.annotation.Aggregate;
import org.apache.fineract.cn.command.annotation.CommandHandler;
import org.apache.fineract.cn.command.annotation.CommandLogLevel;
import org.apache.fineract.cn.command.annotation.EventEmitter;
import org.apache.fineract.cn.command.internal.CommandBus;
import org.apache.fineract.cn.lang.ApplicationName;
import org.apache.fineract.cn.lang.DateConverter;
import org.apache.fineract.cn.lang.ServiceException;
import org.apache.fineract.cn.rhythm.spi.v1.domain.BeatPublish;
import org.apache.fineract.cn.rhythm.spi.v1.events.BeatPublishEvent;
import org.apache.fineract.cn.rhythm.spi.v1.events.EventConstants;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.transaction.annotation.Transactional;

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
  private final LateCaseRepository lateCaseRepository;
  private final LossProvisionStepRepository lossProvisionStepRepository;

  @Autowired
  public BeatPublishCommandHandler(
      final CaseRepository caseRepository,
      final CaseCommandRepository caseCommandRepository,
      final PortfolioProperties portfolioProperties,
      final DataContextService dataContextService,
      final ApplicationName applicationName,
      final CommandBus commandBus,
      final AccountingAdapter accountingAdapter,
      final LateCaseRepository lateCaseRepository,
      final LossProvisionStepRepository lossProvisionStepRepository) {
    this.caseRepository = caseRepository;
    this.caseCommandRepository = caseCommandRepository;
    this.portfolioProperties = portfolioProperties;
    this.dataContextService = dataContextService;
    this.applicationName = applicationName;
    this.commandBus = commandBus;
    this.accountingAdapter = accountingAdapter;
    this.lateCaseRepository = lateCaseRepository;
    this.lossProvisionStepRepository = lossProvisionStepRepository;
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
      selectorName = org.apache.fineract.cn.portfolio.api.v1.events.EventConstants.SELECTOR_NAME,
      selectorValue = IndividualLoanEventConstants.CHECK_LATE_INDIVIDUALLOAN_CASE)
  public IndividualLoanCommandEvent process(final CheckLateCommand command) {
    final String productIdentifier = command.getProductIdentifier();
    final String caseIdentifier = command.getCaseIdentifier();
    final LocalDateTime forDateTime = DateConverter.fromIsoString(command.getForTime());
    final LocalDate forDate = forDateTime.toLocalDate();
    final DataContextOfAction dataContextOfAction = dataContextService.checkedGetDataContext(
        productIdentifier, caseIdentifier, Collections.emptyList());

    final RealRunningBalances balances = new RealRunningBalances(accountingAdapter, dataContextOfAction);

    final BigDecimal currentBalance = balances.getAccountBalance(AccountDesignators.CUSTOMER_LOAN_PRINCIPAL).orElse(BigDecimal.ZERO);
    if (currentBalance.compareTo(BigDecimal.ZERO) == 0) //No late fees if the current balance is zilch.
      return new IndividualLoanCommandEvent(productIdentifier, caseIdentifier, command.getForTime());


    final LocalDateTime dateOfMostRecentDisbursement = dateOfMostRecentDisburse(dataContextOfAction.getCustomerCaseEntity().getId())
            .orElseThrow(() ->
                ServiceException
                    .badRequest("No last disbursal date for ''{0}.{1}'' could be determined.  " +
                    "Therefore it cannot be checked for lateness.", productIdentifier, caseIdentifier));

    final List<Period> repaymentPeriods = ScheduledActionHelpers.generateRepaymentPeriods(
        dateOfMostRecentDisbursement.toLocalDate(),
        forDate,
        dataContextOfAction.getCaseParameters())
        .collect(Collectors.toList());

    final long repaymentPeriodsBetweenBeginningAndToday = repaymentPeriods.size() - 1;

    final BigDecimal expectedPaymentSum = dataContextOfAction
        .getCaseParametersEntity()
        .getPaymentSize()
        .multiply(BigDecimal.valueOf(repaymentPeriodsBetweenBeginningAndToday));

    final BigDecimal principalPaymentSum = balances.getSumOfChargesForActionSinceDate(
        AccountDesignators.CUSTOMER_LOAN_PRINCIPAL,
        Action.ACCEPT_PAYMENT,
        dateOfMostRecentDisbursement);
    final BigDecimal interestPaymentSum = balances.getSumOfChargesForActionSinceDate(
        AccountDesignators.CUSTOMER_LOAN_INTEREST,
        Action.ACCEPT_PAYMENT,
        dateOfMostRecentDisbursement);
    final BigDecimal feesPaymentSum = balances.getSumOfChargesForActionSinceDate(
        AccountDesignators.CUSTOMER_LOAN_FEES,
        Action.ACCEPT_PAYMENT,
        dateOfMostRecentDisbursement);
    final BigDecimal lateFeesSum = balances.getSumOfChargesForActionSinceDate(
        AccountDesignators.LATE_FEE_INCOME,
        Action.ACCEPT_PAYMENT,
        dateOfMostRecentDisbursement);
    final BigDecimal paymentsSum = principalPaymentSum.add(interestPaymentSum).add(feesPaymentSum.subtract(lateFeesSum));

    if (paymentsSum.compareTo(expectedPaymentSum) < 0) {
      final Optional<LocalDateTime> dateLateSince = dateLateSince(dataContextOfAction.getCustomerCaseEntity().getId());
      if (!dateLateSince.isPresent()) {
        commandBus.dispatch(new MarkLateCommand(productIdentifier, caseIdentifier, command.getForTime()));
      }

      if (dateLateSince.isPresent()) {
        int daysLate;
        try {
          daysLate = Math.toIntExact(dateLateSince.get().until(forDateTime, ChronoUnit.DAYS)) + 1;
        }
        catch (ArithmeticException e) {
          daysLate = -1;
        }
        if (daysLate > 1) {
          final Optional<LossProvisionStepEntity> lossStepEntity = lossProvisionStepRepository.findByProductIdAndDaysLate(dataContextOfAction.getProductEntity().getId(), daysLate);
          if (lossStepEntity.isPresent()) {
            commandBus.dispatch(new MarkInArrearsCommand(productIdentifier, caseIdentifier, command.getForTime(), daysLate));
          }
        }
      }
    }

    return new IndividualLoanCommandEvent(productIdentifier, caseIdentifier, command.getForTime());
  }

  private Optional<LocalDateTime> dateLateSince(final Long caseId) {
    return lateCaseRepository.findByCaseId(caseId).map(LateCaseEntity::getLateSince);
  }

  private Optional<LocalDateTime> dateOfMostRecentDisburse(final Long caseId) {
    final Pageable pageRequest = new PageRequest(0, 10, Sort.Direction.DESC, "createdOn");
    final Page<CaseCommandEntity> page = caseCommandRepository.findByCaseIdAndActionName(
        caseId,
        Action.DISBURSE.name(),
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