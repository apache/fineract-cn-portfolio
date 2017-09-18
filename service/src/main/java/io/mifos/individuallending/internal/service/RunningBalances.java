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
package io.mifos.individuallending.internal.service;

import io.mifos.individuallending.api.v1.domain.product.AccountDesignators;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Myrle Krantz
 */
public interface RunningBalances {
  Map<String, BigDecimal> ACCOUNT_SIGNS = new HashMap<String, BigDecimal>() {{
    final BigDecimal negative = BigDecimal.valueOf(-1);
    final BigDecimal positive = BigDecimal.valueOf(1);

    this.put(AccountDesignators.CUSTOMER_LOAN, negative);
    this.put(AccountDesignators.LOAN_FUNDS_SOURCE, negative);
    this.put(AccountDesignators.PROCESSING_FEE_INCOME, positive);
    this.put(AccountDesignators.ORIGINATION_FEE_INCOME, positive);
    this.put(AccountDesignators.DISBURSEMENT_FEE_INCOME, positive);
    this.put(AccountDesignators.INTEREST_INCOME, positive);
    this.put(AccountDesignators.INTEREST_ACCRUAL, positive);
    this.put(AccountDesignators.LATE_FEE_INCOME, positive);
    this.put(AccountDesignators.LATE_FEE_ACCRUAL, positive);
    this.put(AccountDesignators.ARREARS_ALLOWANCE, positive);
    this.put(AccountDesignators.ENTRY, positive);
  }};

  BigDecimal getBalance(final String accountDesignator);

  default BigDecimal getMaxDebit(final String accountDesignator, final BigDecimal amount) {
    if (accountDesignator.equals(AccountDesignators.ENTRY))
      return amount;

    if (ACCOUNT_SIGNS.get(accountDesignator).signum() == -1)
      return amount;
    else
      return amount.min(getBalance(accountDesignator));
  }

  default BigDecimal getMaxCredit(final String accountDesignator, final BigDecimal amount) {
    if (accountDesignator.equals(AccountDesignators.ENTRY))
      return amount; //don't guard the entry account.

    if (ACCOUNT_SIGNS.get(accountDesignator).signum() != -1)
      return amount;
    else
      return amount.min(getBalance(accountDesignator));
  }
}
