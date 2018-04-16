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

import org.apache.fineract.cn.individuallending.api.v1.domain.product.AccountDesignators;
import org.apache.fineract.cn.individuallending.api.v1.domain.workflow.Action;
import org.apache.fineract.cn.individuallending.internal.service.DataContextOfAction;
import org.apache.fineract.cn.individuallending.internal.service.DesignatorToAccountIdentifierMapper;
import org.apache.fineract.cn.portfolio.api.v1.domain.ChargeDefinition;
import org.apache.fineract.cn.portfolio.service.internal.util.AccountingAdapter;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import net.jodah.expiringmap.ExpirationPolicy;
import net.jodah.expiringmap.ExpiringMap;
import org.apache.fineract.cn.accounting.api.v1.domain.Account;
import org.apache.fineract.cn.accounting.api.v1.domain.AccountType;

/**
 * @author Myrle Krantz
 */
public class RealRunningBalances implements RunningBalances {
  private final AccountingAdapter accountingAdapter;
  private final DesignatorToAccountIdentifierMapper designatorToAccountIdentifierMapper;
  private final DataContextOfAction dataContextOfAction;
  private final ExpiringMap<String, Optional<Account>> accountCache;
  @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
  private Optional<LocalDateTime> startOfTerm;

  public RealRunningBalances(
      final AccountingAdapter accountingAdapter,
      final DataContextOfAction dataContextOfAction) {
    this.accountingAdapter = accountingAdapter;
    this.designatorToAccountIdentifierMapper =
        new DesignatorToAccountIdentifierMapper(dataContextOfAction);
    this.dataContextOfAction = dataContextOfAction;
    this.accountCache = ExpiringMap.builder()
        .maxSize(40)
        .expirationPolicy(ExpirationPolicy.CREATED)
        .expiration(60,TimeUnit.SECONDS)
        .entryLoader((String accountDesignator) -> {
          final Optional<String> accountIdentifier;
          if (accountDesignator.equals(AccountDesignators.ENTRY) || accountDesignator.equals(AccountDesignators.EXPENSE)) {
            accountIdentifier = designatorToAccountIdentifierMapper.map(accountDesignator);
          }
          else {
            accountIdentifier = Optional.of(designatorToAccountIdentifierMapper.mapOrThrow(accountDesignator));
          }
          return accountIdentifier.map(accountingAdapter::getAccount);
        })
        .build();
    this.startOfTerm = Optional.empty();
  }

  @Override
  public BigDecimal getAccountSign(final String accountDesignator) {
    return accountCache.get(accountDesignator)
        .map(Account::getType)
        .map(AccountType::valueOf)
        .flatMap(x -> {
          switch (x)
          {
            case LIABILITY:
            case REVENUE:
            case EQUITY:
              return Optional.of(POSITIVE);

            default:
            case ASSET:
            case EXPENSE:
              return Optional.of(NEGATIVE);
          }
        })
        .orElseGet(() -> {
          switch (accountDesignator) {
            case AccountDesignators.EXPENSE:
              return NEGATIVE;
            case AccountDesignators.ENTRY:
              return POSITIVE;
            default:
              return NEGATIVE;
          }}
        );
  }

  @Override
  public Optional<BigDecimal> getAccountBalance(final String accountDesignator) {
    return accountCache.get(accountDesignator).map(Account::getBalance).map(BigDecimal::valueOf);
  }

  @Override
  public BigDecimal getAccruedBalanceForCharge(final ChargeDefinition chargeDefinition) {
    final String accrualAccountIdentifier = designatorToAccountIdentifierMapper.mapOrThrow(chargeDefinition.getAccrualAccountDesignator());

    final LocalDate startOfTermLocalDate = getStartOfTermOrThrow(dataContextOfAction).toLocalDate();

    final BigDecimal amountAccrued = accountingAdapter.sumMatchingEntriesSinceDate(
        accrualAccountIdentifier,
        startOfTermLocalDate,
        dataContextOfAction.getMessageForCharge(Action.valueOf(chargeDefinition.getAccrueAction())));
    final BigDecimal amountApplied = accountingAdapter.sumMatchingEntriesSinceDate(
        accrualAccountIdentifier,
        startOfTermLocalDate,
        dataContextOfAction.getMessageForCharge(Action.valueOf(chargeDefinition.getChargeAction())));
    return amountAccrued.subtract(amountApplied);
  }

  @Override
  public Optional<LocalDateTime> getStartOfTerm() {
    if (!startOfTerm.isPresent()) {
      final LocalDateTime persistedStartOfTerm = dataContextOfAction.getCustomerCaseEntity().getStartOfTerm();
      if (persistedStartOfTerm != null) {
        this.startOfTerm = Optional.of(persistedStartOfTerm);
        return this.startOfTerm;
      }
      final String customerLoanPrincipalAccountIdentifier = designatorToAccountIdentifierMapper.mapOrThrow(AccountDesignators.CUSTOMER_LOAN_PRINCIPAL);

      this.startOfTerm = accountingAdapter.getDateOfOldestEntryContainingMessage(
          customerLoanPrincipalAccountIdentifier,
          dataContextOfAction.getMessageForCharge(Action.DISBURSE));

      //Moving start of term persistence from accounting to the portfolio db.  Opportunistic migration only right now.
      startOfTerm.ifPresent(startOfTermPersistedInAccounting ->
          dataContextOfAction.getCustomerCaseEntity().setStartOfTerm(startOfTermPersistedInAccounting));
    }

    return this.startOfTerm;
  }

  public BigDecimal getSumOfChargesForActionSinceDate(
      final String accountDesignator,
      final Action action,
      final LocalDateTime since) {
    final String accountIdentifier = designatorToAccountIdentifierMapper.mapOrThrow(accountDesignator);
    return accountingAdapter.sumMatchingEntriesSinceDate(
        accountIdentifier,
        since.toLocalDate(),
        dataContextOfAction.getMessageForCharge(action));

  }
}
