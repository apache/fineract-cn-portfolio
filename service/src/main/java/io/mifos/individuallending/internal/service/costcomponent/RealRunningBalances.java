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
package io.mifos.individuallending.internal.service.costcomponent;

import io.mifos.individuallending.api.v1.domain.product.AccountDesignators;
import io.mifos.individuallending.api.v1.domain.workflow.Action;
import io.mifos.individuallending.internal.service.DataContextOfAction;
import io.mifos.individuallending.internal.service.DesignatorToAccountIdentifierMapper;
import io.mifos.portfolio.api.v1.domain.ChargeDefinition;
import io.mifos.portfolio.service.internal.util.AccountingAdapter;
import net.jodah.expiringmap.ExpirationPolicy;
import net.jodah.expiringmap.ExpiringMap;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

/**
 * @author Myrle Krantz
 */
public class RealRunningBalances implements RunningBalances {
  private final AccountingAdapter accountingAdapter;
  private final DesignatorToAccountIdentifierMapper designatorToAccountIdentifierMapper;
  private final DataContextOfAction dataContextOfAction;
  private final ExpiringMap<String, Optional<BigDecimal>> realAccountBalanceCache;
  @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
  private Optional<LocalDateTime> startOfTerm;

  public RealRunningBalances(
      final AccountingAdapter accountingAdapter,
      final DataContextOfAction dataContextOfAction) {
    this.accountingAdapter = accountingAdapter;
    this.designatorToAccountIdentifierMapper =
        new DesignatorToAccountIdentifierMapper(dataContextOfAction);
    this.dataContextOfAction = dataContextOfAction;
    this.realAccountBalanceCache = ExpiringMap.builder()
        .maxSize(20)
        .expirationPolicy(ExpirationPolicy.CREATED)
        .expiration(30,TimeUnit.SECONDS)
        .entryLoader((String accountDesignator) -> {
          final Optional<String> accountIdentifier;
          if (accountDesignator.equals(AccountDesignators.ENTRY)) {
            accountIdentifier = designatorToAccountIdentifierMapper.map(accountDesignator);
          }
          else {
            accountIdentifier = Optional.of(designatorToAccountIdentifierMapper.mapOrThrow(accountDesignator));
          }
          return accountIdentifier.map(accountingAdapter::getCurrentAccountBalance);
        })
        .build();
    this.startOfTerm = Optional.empty();
  }

  @Override
  public Optional<BigDecimal> getAccountBalance(final String accountDesignator) {
    return realAccountBalanceCache.get(accountDesignator);
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
  public Optional<LocalDateTime> getStartOfTerm(final DataContextOfAction dataContextOfAction) {
    if (!startOfTerm.isPresent()) {
      final String customerLoanPrincipalAccountIdentifier = designatorToAccountIdentifierMapper.mapOrThrow(AccountDesignators.CUSTOMER_LOAN_PRINCIPAL);

      this.startOfTerm = accountingAdapter.getDateOfOldestEntryContainingMessage(
          customerLoanPrincipalAccountIdentifier,
          dataContextOfAction.getMessageForCharge(Action.DISBURSE));
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
