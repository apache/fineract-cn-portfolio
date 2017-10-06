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
import io.mifos.individuallending.api.v1.domain.product.ChargeProportionalDesignator;
import io.mifos.individuallending.api.v1.domain.workflow.Action;
import io.mifos.individuallending.internal.repository.CaseParametersEntity;
import io.mifos.individuallending.internal.service.DataContextOfAction;
import io.mifos.individuallending.internal.service.schedule.ScheduledAction;
import io.mifos.individuallending.internal.service.schedule.ScheduledCharge;
import io.mifos.portfolio.api.v1.domain.ChargeDefinition;
import org.springframework.stereotype.Service;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static io.mifos.individuallending.api.v1.domain.product.ChargeIdentifiers.WRITE_OFF_ID;
import static io.mifos.individuallending.api.v1.domain.product.ChargeIdentifiers.WRITE_OFF_NAME;

/**
 * @author Myrle Krantz
 */
@Service
public class WriteOffPaymentBuilderService implements PaymentBuilderService {
  @Override
  public PaymentBuilder getPaymentBuilder(
      final @Nonnull DataContextOfAction dataContextOfAction,
      final @Nullable BigDecimal ignored,
      final LocalDate forDate,
      final RunningBalances runningBalances)
  {
    final CaseParametersEntity caseParameters = dataContextOfAction.getCaseParametersEntity();
    final int minorCurrencyUnitDigits = dataContextOfAction.getProductEntity().getMinorCurrencyUnitDigits();

    final List<ScheduledCharge> scheduledCharges
        = Collections.singletonList(getScheduledChargeForWriteOff(forDate));

    final BigDecimal loanPaymentSize = dataContextOfAction.getCaseParametersEntity().getPaymentSize();

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
}
