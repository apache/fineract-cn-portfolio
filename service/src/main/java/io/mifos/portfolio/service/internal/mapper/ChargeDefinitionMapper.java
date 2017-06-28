/*
 * Copyright 2017 The Mifos Initiative.
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
package io.mifos.portfolio.service.internal.mapper;

import io.mifos.portfolio.api.v1.domain.ChargeDefinition;
import io.mifos.portfolio.service.internal.repository.ChargeDefinitionEntity;
import io.mifos.portfolio.service.internal.repository.ProductEntity;

import static io.mifos.individuallending.api.v1.domain.product.ChargeIdentifiers.*;

/**
 * @author Myrle Krantz
 */
public class ChargeDefinitionMapper {
  public static ChargeDefinitionEntity map(final ProductEntity productEntity, final ChargeDefinition chargeDefinition) {

    final ChargeDefinitionEntity ret = new ChargeDefinitionEntity();

    ret.setIdentifier(chargeDefinition.getIdentifier());
    ret.setProduct(productEntity);
    ret.setName(chargeDefinition.getName());
    ret.setDescription(chargeDefinition.getDescription());
    ret.setAccrueAction(chargeDefinition.getAccrueAction());
    ret.setChargeAction(chargeDefinition.getChargeAction());
    ret.setAmount(chargeDefinition.getAmount());
    ret.setChargeMethod(chargeDefinition.getChargeMethod());
    ret.setProportionalTo(chargeDefinition.getProportionalTo());
    ret.setForCycleSizeUnit(chargeDefinition.getForCycleSizeUnit());
    ret.setFromAccountDesignator(chargeDefinition.getFromAccountDesignator());
    ret.setAccrualAccountDesignator(chargeDefinition.getAccrualAccountDesignator());
    ret.setToAccountDesignator(chargeDefinition.getToAccountDesignator());

    return ret;
  }

  public static ChargeDefinition map(final ChargeDefinitionEntity from) {
    final ChargeDefinition ret = new ChargeDefinition();

    ret.setIdentifier(from.getIdentifier());
    ret.setName(from.getName());
    ret.setDescription(from.getDescription());
    ret.setAccrueAction(from.getAccrueAction());
    ret.setChargeAction(from.getChargeAction());
    ret.setAmount(from.getAmount());
    ret.setChargeMethod(from.getChargeMethod());
    ret.setProportionalTo(proportionalToLegacyMapper(from, from.getChargeMethod(), from.getIdentifier()));
    ret.setForCycleSizeUnit(from.getForCycleSizeUnit());
    ret.setFromAccountDesignator(from.getFromAccountDesignator());
    ret.setAccrualAccountDesignator(from.getAccrualAccountDesignator());
    ret.setToAccountDesignator(from.getToAccountDesignator());

    return ret;
  }

  private static String proportionalToLegacyMapper(final ChargeDefinitionEntity from,
                                                   final ChargeDefinition.ChargeMethod chargeMethod,
                                                   final String identifier) {
    if ((chargeMethod == ChargeDefinition.ChargeMethod.FIXED) || (from.getProportionalTo() != null))
      return from.getProportionalTo();

    if (identifier.equals(LOAN_FUNDS_ALLOCATION_ID))
      return MAXIMUM_BALANCE_DESIGNATOR;
    else if (identifier.equals(LOAN_ORIGINATION_FEE_ID))
      return MAXIMUM_BALANCE_DESIGNATOR;
    else if (identifier.equals(PROCESSING_FEE_ID))
      return MAXIMUM_BALANCE_DESIGNATOR;
    else if (identifier.equals(LATE_FEE_ID))
      return PAYMENT_ID;
    else
      return RUNNING_BALANCE_DESIGNATOR;
  }
}
