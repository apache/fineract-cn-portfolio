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
import io.mifos.individuallending.internal.service.schedule.Period;
import io.mifos.individuallending.internal.service.schedule.ScheduledAction;
import io.mifos.individuallending.internal.service.schedule.ScheduledCharge;
import io.mifos.individuallending.internal.service.schedule.ScheduledChargesService;
import io.mifos.portfolio.api.v1.domain.ChargeDefinition;
import io.mifos.portfolio.api.v1.domain.CostComponent;
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
public class ClosePaymentBuilderService implements PaymentBuilderService {
  private final ScheduledChargesService scheduledChargesService;

  @Autowired
  public ClosePaymentBuilderService(
      final ScheduledChargesService scheduledChargesService) {
    this.scheduledChargesService = scheduledChargesService;
  }

  @Override
  public PaymentBuilder getPaymentBuilder(
      final DataContextOfAction dataContextOfAction,
      final BigDecimal ignored,
      final LocalDate forDate,
      final RunningBalances runningBalances)
  {
    if (runningBalances.getBalance(AccountDesignators.CUSTOMER_LOAN_GROUP).compareTo(BigDecimal.ZERO) != 0)
      throw ServiceException.conflict("Cannot close loan until the balance is zero.");

    final LocalDate startOfTerm = runningBalances.getStartOfTermOrThrow(dataContextOfAction);

    final CaseParametersEntity caseParameters = dataContextOfAction.getCaseParametersEntity();
    final String productIdentifier = dataContextOfAction.getProductEntity().getIdentifier();
    final int minorCurrencyUnitDigits = dataContextOfAction.getProductEntity().getMinorCurrencyUnitDigits();
    final ScheduledAction closeAction = new ScheduledAction(Action.CLOSE, forDate, new Period(1, forDate));

    final List<ScheduledCharge> scheduledCharges = scheduledChargesService.getScheduledCharges(
        productIdentifier,
        Collections.singletonList(closeAction));

    final Map<Boolean, List<ScheduledCharge>> chargesSplitIntoScheduledAndAccrued = scheduledCharges.stream()
        .collect(Collectors.partitioningBy(x -> CostComponentService.isAccruedChargeForAction(x.getChargeDefinition(), Action.CLOSE)));

    final Map<ChargeDefinition, CostComponent> accruedCostComponents = chargesSplitIntoScheduledAndAccrued.get(true)
        .stream()
        .map(ScheduledCharge::getChargeDefinition)
        .collect(Collectors.toMap(chargeDefinition -> chargeDefinition,
            chargeDefinition -> PaymentBuilderService.getAccruedCostComponentToApply(
                runningBalances,
                dataContextOfAction,
                startOfTerm,
                chargeDefinition)));

    return CostComponentService.getCostComponentsForScheduledCharges(
        Action.CLOSE,
        accruedCostComponents,
        chargesSplitIntoScheduledAndAccrued.get(false),
        caseParameters.getBalanceRangeMaximum(),
        runningBalances,
        dataContextOfAction.getCaseParametersEntity().getPaymentSize(),
        BigDecimal.ZERO,
        BigDecimal.ZERO,
        dataContextOfAction.getInterest(),
        minorCurrencyUnitDigits,
        true);
  }
}
