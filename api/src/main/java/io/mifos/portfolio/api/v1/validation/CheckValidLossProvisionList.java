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
package io.mifos.portfolio.api.v1.validation;

import io.mifos.individuallending.api.v1.domain.product.LossProvisionStep;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;
import java.math.BigDecimal;
import java.util.List;

/**
 * @author Myrle Krantz
 */
public class CheckValidLossProvisionList implements ConstraintValidator<ValidLossProvisionList, List<LossProvisionStep>> {
  @Override
  public void initialize(ValidLossProvisionList constraintAnnotation) {

  }

  @Override
  public boolean isValid(
      final List<LossProvisionStep> value,
      final ConstraintValidatorContext context) {
    final BigDecimal sum = value.stream()
        .map(LossProvisionStep::getPercentProvision)
        .map(x -> x.setScale(2, BigDecimal.ROUND_HALF_EVEN))
        .reduce(BigDecimal.ZERO, BigDecimal::add);
    return sum.compareTo(BigDecimal.valueOf(100_00, 2)) == 0;
  }
}