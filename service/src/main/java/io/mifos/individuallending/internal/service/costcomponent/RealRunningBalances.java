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
import io.mifos.individuallending.internal.service.DesignatorToAccountIdentifierMapper;
import io.mifos.portfolio.service.internal.util.AccountingAdapter;
import net.jodah.expiringmap.ExpirationPolicy;
import net.jodah.expiringmap.ExpiringMap;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

/**
 * @author Myrle Krantz
 */
public class RealRunningBalances implements RunningBalances {
  private final ExpiringMap<String, BigDecimal> realAccountBalanceCache;

  RealRunningBalances(
      final AccountingAdapter accountingAdapter,
      final DesignatorToAccountIdentifierMapper designatorToAccountIdentifierMapper) {
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
          return accountIdentifier.map(accountingAdapter::getCurrentAccountBalance).orElse(BigDecimal.ZERO);
        })
        .build();
  }

  @Override
  public BigDecimal getAccountBalance(final String accountDesignator) {
    return realAccountBalanceCache.get(accountDesignator);
  }
}
