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
package io.mifos.individuallending.internal.service.schedule;

import io.mifos.core.lang.ServiceException;
import io.mifos.individuallending.api.v1.domain.product.AccountDesignators;
import io.mifos.individuallending.api.v1.domain.product.ChargeProportionalDesignator;
import io.mifos.individuallending.api.v1.domain.workflow.Action;
import io.mifos.individuallending.internal.service.DataContextOfAction;
import io.mifos.portfolio.api.v1.domain.ChargeDefinition;
import io.mifos.portfolio.service.internal.repository.ProductArrearsConfigurationEntity;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static io.mifos.individuallending.api.v1.domain.product.ChargeIdentifiers.PROVISION_FOR_LOSSES_ID;
import static io.mifos.individuallending.api.v1.domain.product.ChargeIdentifiers.PROVISION_FOR_LOSSES_NAME;

/**
 * @author Myrle Krantz
 */
@Service
public class LossProvisioningService {
  public Optional<ScheduledCharge> getScheduledChargeForMarkLate(
      final DataContextOfAction dataContextOfAction,
      final LocalDate forDate,
      final int daysLate)
  {
    return getScheduledLossProvisioningCharge(dataContextOfAction, forDate, daysLate, Action.MARK_LATE);
  }


  public Optional<ScheduledCharge> getScheduledChargeForDisbursement(
      final DataContextOfAction dataContextOfAction,
      final LocalDate forDate)
  {
    return getScheduledLossProvisioningCharge(dataContextOfAction, forDate, 0, Action.DISBURSE);
  }

  private Optional<ScheduledCharge> getScheduledLossProvisioningCharge(
      final DataContextOfAction dataContextOfAction,
      final LocalDate forDate,
      final int daysLate, Action action) {
    final Optional<ChargeDefinition> optionalChargeDefinition = percentProvision(dataContextOfAction, daysLate)
        .map(this::getLossProvisionCharge);

    return optionalChargeDefinition.map(chargeDefinition -> {
      final ScheduledAction scheduledAction = new ScheduledAction(action, forDate);
      return new ScheduledCharge(scheduledAction, chargeDefinition, Optional.empty());
    });
  }

  private Optional<BigDecimal> percentProvision(
      final DataContextOfAction dataContextOfAction,
      final int daysLate)
  {
    final List<ProductArrearsConfigurationEntity> arrearsConfigurationForGivenDaysLate =
        dataContextOfAction.getProductArrearsConfigurationEntities().stream()
            .filter(x -> x.getDaysLate() == daysLate)
            .collect(Collectors.toList());

    if (arrearsConfigurationForGivenDaysLate.size() > 1)
      throw ServiceException.internalError("There should not be more than one arrears allocation for given # of days late.");
    if (arrearsConfigurationForGivenDaysLate.size() == 0)
      return Optional.empty(); //None

    return Optional.of(arrearsConfigurationForGivenDaysLate.get(0).getPercentProvision());
  }

  private ChargeDefinition getLossProvisionCharge(final BigDecimal percentProvision) {
    final ChargeDefinition ret = new ChargeDefinition();
    ret.setChargeAction(Action.MARK_LATE.name());
    ret.setIdentifier(PROVISION_FOR_LOSSES_ID);
    ret.setName(PROVISION_FOR_LOSSES_NAME);
    ret.setDescription(PROVISION_FOR_LOSSES_NAME);
    ret.setFromAccountDesignator(AccountDesignators.PRODUCT_LOSS_ALLOWANCE);
    ret.setAccrualAccountDesignator(AccountDesignators.GENERAL_LOSS_ALLOWANCE);
    ret.setToAccountDesignator(AccountDesignators.GENERAL_EXPENSE);
    ret.setProportionalTo(ChargeProportionalDesignator.PRINCIPAL_DESIGNATOR.getValue());
    ret.setChargeMethod(ChargeDefinition.ChargeMethod.PROPORTIONAL);
    ret.setAmount(percentProvision);
    ret.setReadOnly(true);
    return ret;
  }
}