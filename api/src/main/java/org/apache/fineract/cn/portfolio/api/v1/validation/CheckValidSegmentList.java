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

import org.apache.fineract.cn.portfolio.api.v1.domain.BalanceSegmentSet;

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

    if (value.getSegments().size() == 0)
      return false;

    if (value.getSegments().size() != value.getSegmentIdentifiers().size())
      return false;

    if (value.getSegments().get(0).compareTo(BigDecimal.ZERO) != 0)
      return false;

    for (int i = 0; i < value.getSegments().size() -1; i++) {
      final BigDecimal segment1 = value.getSegments().get(i);
      final BigDecimal segment2 = value.getSegments().get(i+1);

      if (segment1.compareTo(segment2) > 0)
        return false;
    }

    return true;
  }
}