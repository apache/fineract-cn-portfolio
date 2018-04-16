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

import org.apache.fineract.cn.portfolio.api.v1.domain.AccountAssignment;

import javax.validation.*;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author Myrle Krantz
 */
public class CheckAccountAssignments implements ConstraintValidator<ValidAccountAssignments, Set<AccountAssignment>> {
  public CheckAccountAssignments() {
  }

  public void initialize(final ValidAccountAssignments constraint) {
  }

  public boolean isValid(final Set<AccountAssignment> accountAssignments, final ConstraintValidatorContext context) {
    if (accountAssignments == null)
      return false;

    final List<String> accountDesignators = accountAssignments.stream()
            .map(AccountAssignment::getDesignator)
            .collect(Collectors.toList());

    final boolean allValidAccountAssignments = accountAssignments.stream()
        .allMatch(this::isValidAccountAssignment);
    if (!allValidAccountAssignments)
      return false;

    final Set<String> alreadySeenAccountDesignators = new HashSet<>();
    for (final String accountDesignator : accountDesignators)
    {
      if (alreadySeenAccountDesignators.contains(accountDesignator))
        return false;
      else
        alreadySeenAccountDesignators.add(accountDesignator);
    }
    return true;
  }

  private boolean isValidAccountAssignment(final AccountAssignment accountAssignment)
  {
    final ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
    final Validator validator = factory.getValidator();
    final Set<ConstraintViolation<AccountAssignment>> errors = validator.validate(accountAssignment);
    return (0 == errors.size());
  }
}