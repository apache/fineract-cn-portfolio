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
package org.apache.fineract.cn.portfolio.api.v1.validation;

import org.apache.fineract.cn.portfolio.api.v1.domain.ChargeDefinition;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

/**
 * @author Myrle Krantz
 */
public class CheckValidChargeDefinition implements ConstraintValidator<ValidChargeDefinition, ChargeDefinition> {
  @Override
  public void initialize(ValidChargeDefinition constraintAnnotation) {
  }

  @SuppressWarnings("RedundantIfStatement")
  @Override
  public boolean isValid(ChargeDefinition value, ConstraintValidatorContext context) {
    if (value.getAmount() == null)
      return false;
    if (value.getAmount().scale() > 4)
      return false;
    if (value.getAccrueAction() != null && value.getAccrualAccountDesignator() == null)
      return false;
    if (value.getAccrueAction() == null && value.getAccrualAccountDesignator() != null)
      return false;
    if (value.getChargeMethod() == ChargeDefinition.ChargeMethod.PROPORTIONAL &&
        value.getProportionalTo() == null)
      return false;
    if (value.getChargeMethod() == ChargeDefinition.ChargeMethod.FIXED &&
        value.getProportionalTo() != null &&
        value.getForSegmentSet() == null) //Even if the charge is a fixed charge, we need a proportional to for segment sets.
       return false;
    if (value.getForSegmentSet() == null &&
        (value.getFromSegment() != null || value.getToSegment() != null))
      return false;
    if (value.getForSegmentSet() != null &&
        (value.getFromSegment() == null || value.getToSegment() == null))
      return false;

    return true;
  }
}
