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
import org.apache.fineract.cn.individuallending.api.v1.domain.product.ChargeProportionalDesignator;
import org.apache.fineract.cn.individuallending.api.v1.domain.workflow.Action;
import org.apache.fineract.cn.individuallending.internal.repository.CaseParametersEntity;
import org.apache.fineract.cn.individuallending.internal.service.ChargeDefinitionService;
import org.apache.fineract.cn.individuallending.internal.service.DataContextOfAction;
import org.apache.fineract.cn.individuallending.internal.service.schedule.ScheduledAction;
import org.apache.fineract.cn.individuallending.internal.service.schedule.ScheduledCharge;
import org.apache.fineract.cn.portfolio.api.v1.domain.ChargeDefinition;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.apache.fineract.cn.individuallending.api.v1.domain.product.ChargeIdentifiers.*;

/**
 * @author Myrle Krantz
 */
@Service
public class WriteOffPaymentBuilderService implements PaymentBuilderService {
  final private ChargeDefinitionService chargeDefinitionService;

  @Autowired
  public WriteOffPaymentBuilderService(
      final ChargeDefinitionService chargeDefinitionService) {
    this.chargeDefinitionService = chargeDefinitionService;
  }

  @Override
  public PaymentBuilder getPaymentBuilder(
      final @Nonnull DataContextOfAction dataContextOfAction,
      final @Nullable BigDecimal ignored,
      final LocalDate forDate,
      final RunningBalances runningBalances)
  {
    final CaseParametersEntity caseParameters = dataContextOfAction.getCaseParametersEntity();
    final int minorCurrencyUnitDigits = dataContextOfAction.getProductEntity().getMinorCurrencyUnitDigits();

    final Stream<ScheduledCharge> scheduledChargesForAccruals
        = chargeDefinitionService.getChargeDefinitionsMappedByAccrueAction(dataContextOfAction.getProductEntity().getIdentifier())
        .values().stream().flatMap(Collection::stream)
        .map(x -> getReverseAccrualScheduledCharge(x, forDate));

    final List<ScheduledCharge> scheduledChargesForAccrualsAndWriteOff = Stream.concat(scheduledChargesForAccruals,
        Stream.of(getScheduledChargeForWriteOff(forDate)))
        .collect(Collectors.toList());

    final BigDecimal loanPaymentSize = dataContextOfAction.getCaseParametersEntity().getPaymentSize();

    return CostComponentService.getCostComponentsForScheduledCharges(
        scheduledChargesForAccrualsAndWriteOff,
        caseParameters.getBalanceRangeMaximum(),
        runningBalances,
        loanPaymentSize,
        BigDecimal.ZERO,
        BigDecimal.ZERO,
        dataContextOfAction.getInterest(),
        minorCurrencyUnitDigits,
        true);
  }


  private ScheduledCharge getScheduledChargeForWriteOff(final LocalDate forDate) {

    final ChargeDefinition chargeDefinition = new ChargeDefinition();
    chargeDefinition.setChargeAction(Action.WRITE_OFF.name());
    chargeDefinition.setIdentifier(WRITE_OFF_ID);
    chargeDefinition.setName(WRITE_OFF_NAME);
    chargeDefinition.setDescription(WRITE_OFF_NAME);
    chargeDefinition.setFromAccountDesignator(AccountDesignators.EXPENSE);
    chargeDefinition.setToAccountDesignator(AccountDesignators.GENERAL_LOSS_ALLOWANCE);
    chargeDefinition.setProportionalTo(ChargeProportionalDesignator.PRINCIPAL_DESIGNATOR.getValue());
    chargeDefinition.setChargeMethod(ChargeDefinition.ChargeMethod.PROPORTIONAL);
    chargeDefinition.setAmount(BigDecimal.valueOf(100));
    chargeDefinition.setReadOnly(true);
    final ScheduledAction scheduledAction = new ScheduledAction(Action.WRITE_OFF, forDate);
    return new ScheduledCharge(scheduledAction, chargeDefinition, Optional.empty());
  }

  private ScheduledCharge getReverseAccrualScheduledCharge(
      final ChargeDefinition accrualChargeDefinition,
      final LocalDate forDate) {

    final ChargeDefinition chargeDefinition = new ChargeDefinition();
    chargeDefinition.setChargeAction(Action.WRITE_OFF.name());
    chargeDefinition.setIdentifier(accrualChargeDefinition.getIdentifier());
    chargeDefinition.setName(accrualChargeDefinition.getName());
    chargeDefinition.setDescription(accrualChargeDefinition.getDescription());
    chargeDefinition.setFromAccountDesignator(accrualChargeDefinition.getFromAccountDesignator());
    chargeDefinition.setAccrualAccountDesignator(accrualChargeDefinition.getAccrualAccountDesignator());
    chargeDefinition.setAccrueAction(accrualChargeDefinition.getAccrueAction());
    chargeDefinition.setToAccountDesignator(AccountDesignators.PRODUCT_LOSS_ALLOWANCE);
    chargeDefinition.setChargeMethod(accrualChargeDefinition.getChargeMethod());
    chargeDefinition.setAmount(accrualChargeDefinition.getAmount());
    chargeDefinition.setReadOnly(true);
    final ScheduledAction scheduledAction = new ScheduledAction(Action.WRITE_OFF, forDate);
    return new ScheduledCharge(scheduledAction, chargeDefinition, Optional.empty());

  }
}
