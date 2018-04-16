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
package org.apache.fineract.cn.individuallending.internal.service.costcomponent;

import org.apache.fineract.cn.individuallending.IndividualLendingPatternFactory;
import org.apache.fineract.cn.individuallending.api.v1.domain.product.AccountDesignators;
import org.apache.fineract.cn.individuallending.internal.service.DataContextOfAction;
import org.apache.fineract.cn.portfolio.api.v1.domain.ChargeDefinition;
import org.apache.fineract.cn.portfolio.api.v1.domain.Pattern;
import org.apache.fineract.cn.portfolio.api.v1.domain.RequiredAccountAssignment;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;
import org.apache.fineract.cn.lang.ServiceException;

/**
 * @author Myrle Krantz
 */
public interface RunningBalances {
  BigDecimal NEGATIVE = BigDecimal.valueOf(-1);
  BigDecimal POSITIVE = BigDecimal.valueOf(1);

  /**
   * Most accounts assignments have a required type, but some (entry for example) can change from request to request.
   *
   * @return NEGATIVE or POSITIVE constant as defined above depending on the type of the underlying account.
   */
  BigDecimal getAccountSign(final String accountDesignator);

  Optional<BigDecimal> getAccountBalance(final String accountDesignator);

  BigDecimal getAccruedBalanceForCharge(
      final ChargeDefinition chargeDefinition);

  Optional<LocalDateTime> getStartOfTerm();

  default boolean isAccountNegative(final String accountDesignator) {
    return getAccountSign(accountDesignator).signum() == -1;
  }


  default LocalDateTime getStartOfTermOrThrow(final DataContextOfAction dataContextOfAction) {
    return this.getStartOfTerm()
        .orElseThrow(() -> ServiceException.internalError(
            "Start of term for loan ''{0}'' could not be acquired from accounting.",
            dataContextOfAction.getCompoundIdentifer()));
  }

  default Optional<BigDecimal> getLedgerBalance(final String ledgerDesignator) {
    final Pattern individualLendingPattern = IndividualLendingPatternFactory.individualLendingPattern();
    return individualLendingPattern.getAccountAssignmentsRequired().stream()
        .filter(requiredAccountAssignment -> ledgerDesignator.equals(requiredAccountAssignment.getGroup()))
        .map(RequiredAccountAssignment::getAccountDesignator)
        .map(this::getAccountBalance)
        .reduce(Optional.empty(), (x, y) -> {
          if (x.isPresent() && y.isPresent())
            return Optional.of(x.get().add(y.get()));
          else if (x.isPresent())
            return x;
          else //noinspection OptionalIsPresent
            if (y.isPresent())
            return y;
          else
            return Optional.empty();
        });
  }

  default Optional<BigDecimal> getBalance(final String designator) {
    final Pattern individualLendingPattern = IndividualLendingPatternFactory.individualLendingPattern();
    if (individualLendingPattern.getAccountAssignmentGroups().contains(designator))
      return getLedgerBalance(designator);
    else
      return getAccountBalance(designator);
  }


  /**
   *
   * @param requestedAmount The requested amount is necessary as a parameter, because infinity is
   *                        not available as a return value for BigDecimal.  There is no way to express that there is
   *                        no limit, so when there is no limit, the requestedAmount is what is returned.
   */
  default BigDecimal getAvailableBalance(final String designator, final BigDecimal requestedAmount) {
    return getBalance(designator).orElse(requestedAmount);
  }

  default BigDecimal getMaxDebit(final String accountDesignator, final BigDecimal amount) {
    if (isAccountNegative(accountDesignator))
      return amount;
    else
      return amount.min(getAvailableBalance(accountDesignator, amount));
  }

  default BigDecimal getMaxCredit(final String accountDesignator, final BigDecimal amount) {
    if (accountDesignator.equals(AccountDesignators.EXPENSE) ||
        accountDesignator.equals(AccountDesignators.PRODUCT_LOSS_ALLOWANCE) ||
        accountDesignator.equals(AccountDesignators.GENERAL_LOSS_ALLOWANCE))
      return amount;
    //expense account can achieve a "relative" negative balance, and
    // both loss allowance accounts can achieve an "absolute" negative balance.

    if (!isAccountNegative(accountDesignator))
      return amount;
    else
      return amount.min(getAvailableBalance(accountDesignator, amount));
  }
}
