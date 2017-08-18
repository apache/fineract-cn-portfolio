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
package io.mifos.portfolio.service.internal.service;

import io.mifos.portfolio.api.v1.domain.BalanceSegmentSet;
import io.mifos.portfolio.service.internal.mapper.BalanceSegmentSetMapper;
import io.mifos.portfolio.service.internal.repository.BalanceSegmentSetRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * @author Myrle Krantz
 */
@Service
public class BalanceSegmentSetService {
  private final BalanceSegmentSetRepository balanceSegmentSetRepository;

  @Autowired
  public BalanceSegmentSetService(
      final BalanceSegmentSetRepository balanceSegmentSetRepository) {
    this.balanceSegmentSetRepository = balanceSegmentSetRepository;
  }

  public boolean existsByIdentifier(final String productIdentifier, final String balanceSegmentSetIdentifier)
  {
    //TODO: replace with existsBy once we've upgraded to spring data 1.11 or later.
    return balanceSegmentSetRepository
        .findByProductIdentifierAndSegmentSetIdentifier(productIdentifier, balanceSegmentSetIdentifier)
        .findAny().isPresent();
  }

  public Optional<BalanceSegmentSet> findByIdentifier(
      final String productIdentifier,
      final String balanceSegmentSetIdentifier) {
    return BalanceSegmentSetMapper.map(balanceSegmentSetRepository
        .findByProductIdentifierAndSegmentSetIdentifier(productIdentifier, balanceSegmentSetIdentifier));
  }
}
