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
package org.apache.fineract.cn.individuallending.internal.service.costcomponent;

import org.apache.fineract.cn.individuallending.api.v1.domain.product.AccountDesignators;
import org.apache.fineract.cn.individuallending.internal.repository.CaseParametersEntity;
import org.apache.fineract.cn.individuallending.internal.service.DataContextOfAction;
import org.apache.fineract.cn.individuallending.internal.service.schedule.ScheduledAction;
import org.apache.fineract.cn.individuallending.internal.service.schedule.ScheduledActionHelpers;
import org.apache.fineract.cn.individuallending.internal.service.schedule.ScheduledCharge;
import org.apache.fineract.cn.individuallending.internal.service.schedule.ScheduledChargesService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

/**
 * @author Myrle Krantz
 */
@Service
public class AcceptPaymentBuilderService implements PaymentBuilderService {
  private final ScheduledChargesService scheduledChargesService;

  @Autowired
  public AcceptPaymentBuilderService(
      final ScheduledChargesService scheduledChargesService) {
    this.scheduledChargesService = scheduledChargesService;
  }

  @Override
  public PaymentBuilder getPaymentBuilder(
      final DataContextOfAction dataContextOfAction,
      final BigDecimal requestedLoanPaymentSize,
      final LocalDate forDate,
      final RunningBalances runningBalances) {
    final LocalDateTime startOfTerm = runningBalances.getStartOfTermOrThrow(dataContextOfAction);

    final CaseParametersEntity caseParameters = dataContextOfAction.getCaseParametersEntity();
    final String productIdentifier = dataContextOfAction.getProductEntity().getIdentifier();
    final int minorCurrencyUnitDigits = dataContextOfAction.getProductEntity().getMinorCurrencyUnitDigits();
    final ScheduledAction scheduledAction
        = ScheduledActionHelpers.getNextScheduledPayment(
        startOfTerm.toLocalDate(),
        forDate,
        dataContextOfAction.getCustomerCaseEntity().getEndOfTerm().toLocalDate(),
        dataContextOfAction.getCaseParameters()
    );

    final List<ScheduledCharge> scheduledChargesForThisAction = scheduledChargesService.getScheduledCharges(
        productIdentifier,
        Collections.singletonList(scheduledAction));

    final BigDecimal loanPaymentSize;

    if (requestedLoanPaymentSize != null) {
      loanPaymentSize = requestedLoanPaymentSize
          .min(runningBalances.getBalance(AccountDesignators.CUSTOMER_LOAN_GROUP).orElse(BigDecimal.ZERO));
    }
    else if (scheduledAction.getActionPeriod() != null && scheduledAction.getActionPeriod().isLastPeriod()) {
      loanPaymentSize = runningBalances.getBalance(AccountDesignators.CUSTOMER_LOAN_GROUP).orElse(BigDecimal.ZERO);
    }
    else {
      loanPaymentSize = dataContextOfAction.getCaseParametersEntity().getPaymentSize()
          .min(runningBalances.getBalance(AccountDesignators.CUSTOMER_LOAN_GROUP).orElse(BigDecimal.ZERO));
    }

    final AvailableRunningBalancesWithLimits availableRunningBalanceWithLimits =
        new AvailableRunningBalancesWithLimits(runningBalances);
    availableRunningBalanceWithLimits.setUpperLimit(AccountDesignators.ENTRY, loanPaymentSize);


    return CostComponentService.getCostComponentsForScheduledCharges(
        scheduledChargesForThisAction,
        caseParameters.getBalanceRangeMaximum(),
        availableRunningBalanceWithLimits,
        dataContextOfAction.getCaseParametersEntity().getPaymentSize(),
        BigDecimal.ZERO,
        loanPaymentSize,
        dataContextOfAction.getInterest(),
        minorCurrencyUnitDigits,
        true);
  }
}
