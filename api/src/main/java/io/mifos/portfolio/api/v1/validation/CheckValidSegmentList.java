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

import io.mifos.portfolio.api.v1.domain.BalanceSegmentSet;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;
import java.math.BigDecimal;

/**
 * @author Myrle Krantz
 */
public class CheckValidSegmentList implements ConstraintValidator<ValidSegmentList, BalanceSegmentSet> {
  @Override
  public void initialize(ValidSegmentList constraintAnnotation) {

  }

  @Override
  public boolean isValid(BalanceSegmentSet value, ConstraintValidatorContext context) {
    if (value.getSegments() == null)
      return false;
    if (value.getSegmentIdentifiers() == null)
      return false;

    if (value.getSegments().size() + 1 != value.getSegmentIdentifiers().size())
      return false;

    for (final BigDecimal segment : value.getSegments()) {
      if (segment.compareTo(BigDecimal.ZERO) <= 0)
        return false;
    }

    return true;
  }
}