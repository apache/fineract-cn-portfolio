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

import io.mifos.core.lang.ServiceException;
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

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * @author Myrle Krantz
 */
@Service
public class DisbursePaymentBuilderService implements PaymentBuilderService {
  private final CostComponentService costComponentService;
  private final ScheduledChargesService scheduledChargesService;
  private final AccountingAdapter accountingAdapter;

  @Autowired
  public DisbursePaymentBuilderService(
      final CostComponentService costComponentService,
      final ScheduledChargesService scheduledChargesService,
      final AccountingAdapter accountingAdapter) {
    this.costComponentService = costComponentService;
    this.scheduledChargesService = scheduledChargesService;
    this.accountingAdapter = accountingAdapter;
  }

  public PaymentBuilder getPaymentBuilder(
      final @Nonnull DataContextOfAction dataContextOfAction,
      final @Nullable BigDecimal requestedDisbursalSize,
      final LocalDate forDate)
  {
    final DesignatorToAccountIdentifierMapper designatorToAccountIdentifierMapper
        = new DesignatorToAccountIdentifierMapper(dataContextOfAction);
    final String customerLoanPrincipalAccountIdentifier = designatorToAccountIdentifierMapper.mapOrThrow(AccountDesignators.CUSTOMER_LOAN_PRINCIPAL);
    final RealRunningBalances runningBalances = new RealRunningBalances(accountingAdapter, designatorToAccountIdentifierMapper);
    final BigDecimal currentBalance = runningBalances.getBalance(AccountDesignators.CUSTOMER_LOAN_PRINCIPAL);

    if (requestedDisbursalSize != null &&
        dataContextOfAction.getCaseParametersEntity().getBalanceRangeMaximum().compareTo(
            currentBalance.add(requestedDisbursalSize)) < 0)
      throw ServiceException.conflict("Cannot disburse over the maximum balance.");

    final Optional<LocalDateTime> optionalStartOfTerm = accountingAdapter.getDateOfOldestEntryContainingMessage(
        customerLoanPrincipalAccountIdentifier,
        dataContextOfAction.getMessageForCharge(Action.DISBURSE));
    final CaseParametersEntity caseParameters = dataContextOfAction.getCaseParametersEntity();
    final String productIdentifier = dataContextOfAction.getProductEntity().getIdentifier();
    final int minorCurrencyUnitDigits = dataContextOfAction.getProductEntity().getMinorCurrencyUnitDigits();
    final List<ScheduledAction> scheduledActions = Collections.singletonList(new ScheduledAction(Action.DISBURSE, forDate));

    final BigDecimal disbursalSize;
    if (requestedDisbursalSize == null)
      disbursalSize = dataContextOfAction.getCaseParametersEntity().getBalanceRangeMaximum();
    else
      disbursalSize = requestedDisbursalSize;

    final List<ScheduledCharge> scheduledCharges = scheduledChargesService.getScheduledCharges(
        productIdentifier, scheduledActions);


    final Map<Boolean, List<ScheduledCharge>> chargesSplitIntoScheduledAndAccrued = scheduledCharges.stream()
        .collect(Collectors.partitioningBy(x -> CostComponentService.isAccruedChargeForAction(x.getChargeDefinition(), Action.DISBURSE)));

    final Map<ChargeDefinition, CostComponent> accruedCostComponents =
        optionalStartOfTerm.map(startOfTerm ->
            chargesSplitIntoScheduledAndAccrued.get(true)
                .stream()
                .map(ScheduledCharge::getChargeDefinition)
                .collect(Collectors.toMap(chargeDefinition -> chargeDefinition,
                    chargeDefinition -> costComponentService.getAccruedCostComponentToApply(
                        dataContextOfAction,
                        designatorToAccountIdentifierMapper,
                        startOfTerm.toLocalDate(),
                        chargeDefinition)))).orElse(Collections.emptyMap());

    return CostComponentService.getCostComponentsForScheduledCharges(
        accruedCostComponents,
        chargesSplitIntoScheduledAndAccrued.get(false),
        caseParameters.getBalanceRangeMaximum(),
        runningBalances,
        dataContextOfAction.getCaseParametersEntity().getPaymentSize(),
        disbursalSize,
        BigDecimal.ZERO,
        dataContextOfAction.getInterest(),
        minorCurrencyUnitDigits,
        true);
  }

  public BigDecimal getLoanPaymentSizeForSingleDisbursement(
      final BigDecimal disbursementSize,
      final DataContextOfAction dataContextOfAction) {
    final List<ScheduledAction> hypotheticalScheduledActions = ScheduledActionHelpers.getHypotheticalScheduledActions(
        CostComponentService.today(),
        dataContextOfAction.getCaseParameters());
    final List<ScheduledCharge> hypotheticalScheduledCharges = scheduledChargesService.getScheduledCharges(
        dataContextOfAction.getProductEntity().getIdentifier(),
        hypotheticalScheduledActions);
    return CostComponentService.getLoanPaymentSize(
        disbursementSize,
        disbursementSize,
        dataContextOfAction.getInterest(),
        dataContextOfAction.getProductEntity().getMinorCurrencyUnitDigits(),
        hypotheticalScheduledCharges);
  }
}
