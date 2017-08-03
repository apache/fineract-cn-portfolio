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
package io.mifos.portfolio.api.v1.validation;

import io.mifos.core.lang.validation.CheckIdentifier;
import io.mifos.individuallending.api.v1.domain.product.ChargeProportionalDesignator;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

/**
 * @author Myrle Krantz
 */
public class CheckChargeReference implements ConstraintValidator<ValidChargeReference, String> {

  @Override
  public void initialize(ValidChargeReference constraintAnnotation) {

  }

  @Override
  public boolean isValid(String value, ConstraintValidatorContext context) {
    if (value == null)
      return true;

    if (ChargeProportionalDesignator.fromString(value).isPresent())
      return true;

    final CheckIdentifier identifierChecker = new CheckIdentifier();
    return identifierChecker.isValid(value, context);
  }
}
