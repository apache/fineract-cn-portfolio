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
package org.apache.fineract.cn.portfolio.service.internal.service;

import org.apache.fineract.cn.portfolio.api.v1.domain.BalanceSegmentSet;
import org.apache.fineract.cn.portfolio.service.internal.mapper.BalanceSegmentSetMapper;
import org.apache.fineract.cn.portfolio.service.internal.repository.BalanceSegmentEntity;
import org.apache.fineract.cn.portfolio.service.internal.repository.BalanceSegmentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * @author Myrle Krantz
 */
@Service
public class BalanceSegmentSetService {
  private final BalanceSegmentRepository balanceSegmentRepository;

  @Autowired
  public BalanceSegmentSetService(
      final BalanceSegmentRepository balanceSegmentRepository) {
    this.balanceSegmentRepository = balanceSegmentRepository;
  }

  public boolean existsByIdentifier(final String productIdentifier, final String balanceSegmentSetIdentifier)
  {
    //TODO: replace with existsBy once we've upgraded to spring data 1.11 or later.
    return balanceSegmentRepository
        .findByProductIdentifierAndSegmentSetIdentifier(productIdentifier, balanceSegmentSetIdentifier)
        .findAny().isPresent();
  }

  public Optional<BalanceSegmentSet> findByIdentifier(
      final String productIdentifier,
      final String balanceSegmentSetIdentifier) {
    return BalanceSegmentSetMapper.map(balanceSegmentRepository
        .findByProductIdentifierAndSegmentSetIdentifier(productIdentifier, balanceSegmentSetIdentifier));
  }

  public List<BalanceSegmentSet> findByIdentifier(final String productIdentifier) {
    final Map<String, List<BalanceSegmentEntity>> listsOfEntitiesDividedBySet = balanceSegmentRepository
        .findByProductIdentifier(productIdentifier)
        .collect(Collectors.groupingBy(BalanceSegmentEntity::getSegmentSetIdentifier, Collectors.toList()));
    return listsOfEntitiesDividedBySet.values().stream()
        .map(x -> BalanceSegmentSetMapper.map(x.stream()))
        .filter(Optional::isPresent)
        .map(Optional::get)
        .collect(Collectors.toList());
  }
}
