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
package org.apache.fineract.cn.portfolio.service.internal.mapper;

import org.apache.fineract.cn.individuallending.api.v1.domain.product.ChargeProportionalDesignator;
import org.apache.fineract.cn.portfolio.api.v1.domain.ChargeDefinition;
import org.apache.fineract.cn.portfolio.service.internal.repository.BalanceSegmentEntity;
import org.apache.fineract.cn.portfolio.service.internal.repository.ChargeDefinitionEntity;
import org.apache.fineract.cn.portfolio.service.internal.repository.ProductEntity;

import javax.annotation.Nullable;
import java.util.Optional;

import static org.apache.fineract.cn.individuallending.api.v1.domain.product.ChargeIdentifiers.*;

/**
 * @author Myrle Krantz
 */
public class ChargeDefinitionMapper {
  public static ChargeDefinitionEntity map(
      final ProductEntity productEntity,
      final ChargeDefinition chargeDefinition,
      @Nullable final BalanceSegmentEntity fromSegment,
      @Nullable final BalanceSegmentEntity toSegment) {

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
    ret.setReadOnly(chargeDefinition.isReadOnly());
    if (fromSegment != null && toSegment != null) {
      ret.setSegmentSet(fromSegment.getSegmentSetIdentifier());
      ret.setFromSegment(fromSegment.getSegmentIdentifier());
      ret.setToSegment(toSegment.getSegmentIdentifier());
    }
    ret.setOnTop(chargeDefinition.getChargeOnTop());

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
    ret.setReadOnly(Optional.ofNullable(from.getReadOnly()).orElse(false));
    if (from.getSegmentSet() != null && from.getFromSegment() != null && from.getToSegment() != null) {
      ret.setForSegmentSet(from.getSegmentSet());
      ret.setFromSegment(from.getFromSegment());
      ret.setToSegment(from.getToSegment());
    }
    ret.setChargeOnTop(from.getOnTop());

    return ret;
  }

  private static String proportionalToLegacyMapper(final ChargeDefinitionEntity from,
                                                   final ChargeDefinition.ChargeMethod chargeMethod,
                                                   final String identifier) {
    if ((chargeMethod == ChargeDefinition.ChargeMethod.FIXED) || (from.getProportionalTo() != null))
      return from.getProportionalTo();

    switch (identifier) {
      case LOAN_ORIGINATION_FEE_ID:
        return ChargeProportionalDesignator.MAXIMUM_BALANCE_DESIGNATOR.getValue();
      case PROCESSING_FEE_ID:
        return ChargeProportionalDesignator.MAXIMUM_BALANCE_DESIGNATOR.getValue();
      case LATE_FEE_ID:
        return ChargeProportionalDesignator.CONTRACTUAL_REPAYMENT_DESIGNATOR.getValue();
      default:
        return ChargeProportionalDesignator.RUNNING_BALANCE_DESIGNATOR.getValue();
    }
  }
}
