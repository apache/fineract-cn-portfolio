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

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;
import java.time.temporal.ChronoUnit;

/**
 * @author Myrle Krantz
 */
@SuppressWarnings("WeakerAccess")
public class CheckPaymentCycleUnit implements ConstraintValidator<ValidPaymentCycleUnit, ChronoUnit> {
  @Override
  public void initialize(final ValidPaymentCycleUnit constraintAnnotation) { }

  @Override
  public boolean isValid(final ChronoUnit value, final ConstraintValidatorContext context) {
    return (value == null) || (value == ChronoUnit.WEEKS) || (value == ChronoUnit.MONTHS) || (value == ChronoUnit.YEARS);
  }
}
