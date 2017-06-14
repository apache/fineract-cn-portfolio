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
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * @author Myrle Krantz
 */
@SuppressWarnings("WeakerAccess")
public class CheckValidSortColumn implements ConstraintValidator<ValidSortColumn, String> {
  private Set<String> allowableColumns;

  @Override
  public void initialize(final ValidSortColumn constraintAnnotation) {
    allowableColumns = new HashSet<>(Arrays.asList(constraintAnnotation.value()));
  }

  @Override
  public boolean isValid(final String value, final ConstraintValidatorContext context) {
    return validate(value, allowableColumns);
  }

  public static boolean validate(String value, Set<String> allowableColumns) {
    return value == null || allowableColumns.contains(value);
  }
}