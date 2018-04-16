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

import org.apache.fineract.cn.individuallending.api.v1.domain.product.ChargeProportionalDesignator;
import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;
import org.apache.fineract.cn.lang.validation.CheckIdentifier;

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
