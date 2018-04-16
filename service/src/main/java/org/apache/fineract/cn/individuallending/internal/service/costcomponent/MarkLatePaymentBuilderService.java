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

import org.apache.fineract.cn.individuallending.api.v1.domain.workflow.Action;
import org.apache.fineract.cn.individuallending.internal.repository.CaseParametersEntity;
import org.apache.fineract.cn.individuallending.internal.service.DataContextOfAction;
import org.apache.fineract.cn.individuallending.internal.service.schedule.LossProvisionChargesService;
import org.apache.fineract.cn.individuallending.internal.service.schedule.ScheduledAction;
import org.apache.fineract.cn.individuallending.internal.service.schedule.ScheduledCharge;
import org.apache.fineract.cn.individuallending.internal.service.schedule.ScheduledChargesService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * @author Myrle Krantz
 */
@Service
public class MarkLatePaymentBuilderService implements PaymentBuilderService {
  private final ScheduledChargesService scheduledChargesService;
  private final LossProvisionChargesService lossProvisionChargesService;

  @Autowired
  public MarkLatePaymentBuilderService(
      final ScheduledChargesService scheduledChargesService,
      final LossProvisionChargesService lossProvisionChargesService) {
    this.scheduledChargesService = scheduledChargesService;
    this.lossProvisionChargesService = lossProvisionChargesService;
  }

  @Override
  public PaymentBuilder getPaymentBuilder(
      final @Nonnull DataContextOfAction dataContextOfAction,
      final @Nullable BigDecimal ignored,
      final LocalDate forDate,
      final RunningBalances runningBalances)
  {
    final CaseParametersEntity caseParameters = dataContextOfAction.getCaseParametersEntity();
    final String productIdentifier = dataContextOfAction.getProductEntity().getIdentifier();
    final int minorCurrencyUnitDigits = dataContextOfAction.getProductEntity().getMinorCurrencyUnitDigits();
    final ScheduledAction scheduledAction = new ScheduledAction(Action.MARK_LATE, forDate);

    final BigDecimal loanPaymentSize = dataContextOfAction.getCaseParametersEntity().getPaymentSize();

    final List<ScheduledCharge> scheduledCharges = scheduledChargesService.getScheduledCharges(
        productIdentifier,
        Collections.singletonList(scheduledAction));
    final Optional<ScheduledCharge> initialLossProvisionCharge = lossProvisionChargesService.getScheduledChargeForMarkLate(
        dataContextOfAction, forDate);
    initialLossProvisionCharge.ifPresent(scheduledCharges::add);

    return CostComponentService.getCostComponentsForScheduledCharges(
        scheduledCharges,
        caseParameters.getBalanceRangeMaximum(),
        runningBalances,
        loanPaymentSize,
        BigDecimal.ZERO,
        BigDecimal.ZERO,
        dataContextOfAction.getInterest(),
        minorCurrencyUnitDigits,
        true);
  }
}
