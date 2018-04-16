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
package org.apache.fineract.cn.individuallending.internal.service.schedule;

import org.apache.fineract.cn.individuallending.api.v1.domain.product.AccountDesignators;
import org.apache.fineract.cn.individuallending.api.v1.domain.product.ChargeProportionalDesignator;
import org.apache.fineract.cn.individuallending.api.v1.domain.product.LossProvisionStep;
import org.apache.fineract.cn.individuallending.api.v1.domain.workflow.Action;
import org.apache.fineract.cn.individuallending.internal.service.DataContextOfAction;
import org.apache.fineract.cn.individuallending.internal.service.LossProvisionStepService;
import org.apache.fineract.cn.portfolio.api.v1.domain.ChargeDefinition;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import static org.apache.fineract.cn.individuallending.api.v1.domain.product.ChargeIdentifiers.*;

/**
 * @author Myrle Krantz
 */
@Service
public class LossProvisionChargesService {
  private final LossProvisionStepService lossProvisionStepService;

  @Autowired
  public LossProvisionChargesService(
      final LossProvisionStepService lossProvisionStepService) {
    this.lossProvisionStepService = lossProvisionStepService;
  }

  public Optional<ScheduledCharge> getScheduledChargeForMarkInArrears(
      final DataContextOfAction dataContextOfAction,
      final LocalDate forDate,
      final int daysLate)
  {
    return getScheduledLossProvisioningCharge(dataContextOfAction, forDate, daysLate, Action.MARK_IN_ARREARS);
  }

  public Optional<ScheduledCharge> getScheduledChargeForMarkLate(
      final DataContextOfAction dataContextOfAction,
      final LocalDate forDate)
  {
    return getScheduledLossProvisioningCharge(dataContextOfAction, forDate, 1, Action.MARK_LATE);
  }


  public Optional<ScheduledCharge> getScheduledChargeForDisbursement(
      final DataContextOfAction dataContextOfAction,
      final LocalDate forDate)
  {
    final Optional<ScheduledCharge> ret = getScheduledLossProvisioningCharge(dataContextOfAction, forDate, 0, Action.DISBURSE);
    ret.ifPresent(x -> x.getChargeDefinition().setProportionalTo(ChargeProportionalDesignator.REQUESTED_REPAYMENT_DESIGNATOR.getValue()));
    return ret;
  }

  private Optional<ScheduledCharge> getScheduledLossProvisioningCharge(
      final DataContextOfAction dataContextOfAction,
      final LocalDate forDate,
      final int daysLate, Action action) {
    final Optional<ChargeDefinition> optionalChargeDefinition = lossProvisionStepService.findByProductIdAndDaysLate(dataContextOfAction.getProductEntity().getId(), daysLate)
        .map(LossProvisionStep::getPercentProvision)
        .map(percentProvision -> getLossProvisionCharge(percentProvision, action));

    return optionalChargeDefinition.map(chargeDefinition -> {
      final ScheduledAction scheduledAction = new ScheduledAction(action, forDate);
      return new ScheduledCharge(scheduledAction, chargeDefinition, Optional.empty());
    });
  }

  private ChargeDefinition getLossProvisionCharge(
      final BigDecimal percentProvision,
      final Action action) {
    final ChargeDefinition ret = new ChargeDefinition();
    ret.setChargeAction(action.name());
    ret.setIdentifier(PROVISION_FOR_LOSSES_ID);
    ret.setName(PROVISION_FOR_LOSSES_NAME);
    ret.setDescription(PROVISION_FOR_LOSSES_NAME);
    ret.setFromAccountDesignator(AccountDesignators.PRODUCT_LOSS_ALLOWANCE);
    ret.setToAccountDesignator(AccountDesignators.GENERAL_LOSS_ALLOWANCE);
    ret.setProportionalTo(ChargeProportionalDesignator.PRINCIPAL_DESIGNATOR.getValue());
    ret.setChargeMethod(ChargeDefinition.ChargeMethod.PROPORTIONAL);
    ret.setAmount(percentProvision.negate());
    ret.setReadOnly(true);
    return ret;
  }
}