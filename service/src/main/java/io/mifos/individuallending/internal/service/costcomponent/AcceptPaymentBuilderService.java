/*
 * Copyright 2017 Kuelap, Inc.
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
package io.mifos.individuallending.internal.service.costcomponent;

import io.mifos.individuallending.api.v1.domain.product.AccountDesignators;
import io.mifos.individuallending.api.v1.domain.workflow.Action;
import io.mifos.individuallending.internal.repository.CaseParametersEntity;
import io.mifos.individuallending.internal.service.DataContextOfAction;
import io.mifos.individuallending.internal.service.DesignatorToAccountIdentifierMapper;
import io.mifos.individuallending.internal.service.schedule.ScheduledAction;
import io.mifos.individuallending.internal.service.schedule.ScheduledActionHelpers;
import io.mifos.individuallending.internal.service.schedule.ScheduledCharge;
import io.mifos.individuallending.internal.service.schedule.ScheduledChargesService;
import io.mifos.portfolio.api.v1.domain.ChargeDefinition;
import io.mifos.portfolio.api.v1.domain.CostComponent;
import io.mifos.portfolio.service.internal.util.AccountingAdapter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author Myrle Krantz
 */
@Service
public class AcceptPaymentBuilderService implements PaymentBuilderService {
  private final CostComponentService costComponentService;
  private final ScheduledChargesService scheduledChargesService;
  private final AccountingAdapter accountingAdapter;

  @Autowired
  public AcceptPaymentBuilderService(
      final CostComponentService costComponentService,
      final ScheduledChargesService scheduledChargesService,
      final AccountingAdapter accountingAdapter) {
    this.costComponentService = costComponentService;
    this.scheduledChargesService = scheduledChargesService;
    this.accountingAdapter = accountingAdapter;
  }

  public PaymentBuilder getPaymentBuilder(
      final DataContextOfAction dataContextOfAction,
      final BigDecimal requestedLoanPaymentSize,
      final LocalDate forDate)
  {
    final DesignatorToAccountIdentifierMapper designatorToAccountIdentifierMapper
        = new DesignatorToAccountIdentifierMapper(dataContextOfAction);
    final RealRunningBalances runningBalances = new RealRunningBalances(accountingAdapter, designatorToAccountIdentifierMapper);

    final LocalDate startOfTerm = costComponentService.getStartOfTermOrThrow(dataContextOfAction, designatorToAccountIdentifierMapper);

    final CaseParametersEntity caseParameters = dataContextOfAction.getCaseParametersEntity();
    final String productIdentifier = dataContextOfAction.getProductEntity().getIdentifier();
    final int minorCurrencyUnitDigits = dataContextOfAction.getProductEntity().getMinorCurrencyUnitDigits();
    final ScheduledAction scheduledAction
        = ScheduledActionHelpers.getNextScheduledPayment(
        startOfTerm,
        forDate,
        dataContextOfAction.getCustomerCaseEntity().getEndOfTerm().toLocalDate(),
        dataContextOfAction.getCaseParameters()
    );

    final List<ScheduledCharge> scheduledChargesForThisAction = scheduledChargesService.getScheduledCharges(
        productIdentifier,
        Collections.singletonList(scheduledAction));

    final Map<Boolean, List<ScheduledCharge>> chargesSplitIntoScheduledAndAccrued = scheduledChargesForThisAction.stream()
        .collect(Collectors.partitioningBy(x -> CostComponentService.isAccruedChargeForAction(x.getChargeDefinition(), Action.ACCEPT_PAYMENT)));

    final Map<ChargeDefinition, CostComponent> accruedCostComponents = chargesSplitIntoScheduledAndAccrued.get(true)
        .stream()
        .map(ScheduledCharge::getChargeDefinition)
        .collect(Collectors.toMap(chargeDefinition -> chargeDefinition,
            chargeDefinition -> costComponentService.getAccruedCostComponentToApply(dataContextOfAction, designatorToAccountIdentifierMapper, startOfTerm, chargeDefinition)));


    final BigDecimal loanPaymentSize;

    if (requestedLoanPaymentSize != null) {
      loanPaymentSize = requestedLoanPaymentSize;
    }
    else {
      if (scheduledAction.getActionPeriod() != null && scheduledAction.getActionPeriod().isLastPeriod()) {
        loanPaymentSize = runningBalances.getBalance(AccountDesignators.CUSTOMER_LOAN_GROUP);
      }
      else {
        final BigDecimal paymentSizeBeforeOnTopCharges = runningBalances.getBalance(AccountDesignators.CUSTOMER_LOAN_GROUP)
            .min(dataContextOfAction.getCaseParametersEntity().getPaymentSize());

        @SuppressWarnings("UnnecessaryLocalVariable")
        final BigDecimal paymentSizeIncludingOnTopCharges = accruedCostComponents.entrySet().stream()
            .filter(entry -> entry.getKey().getChargeOnTop() != null && entry.getKey().getChargeOnTop())
            .map(entry -> entry.getValue().getAmount())
            .reduce(paymentSizeBeforeOnTopCharges, BigDecimal::add);

        loanPaymentSize = paymentSizeIncludingOnTopCharges;
      }
    }


    return CostComponentService.getCostComponentsForScheduledCharges(
        accruedCostComponents,
        chargesSplitIntoScheduledAndAccrued.get(false),
        caseParameters.getBalanceRangeMaximum(),
        runningBalances,
        dataContextOfAction.getCaseParametersEntity().getPaymentSize(),
        BigDecimal.ZERO,
        loanPaymentSize,
        dataContextOfAction.getInterest(),
        minorCurrencyUnitDigits,
        true);
  }
}
