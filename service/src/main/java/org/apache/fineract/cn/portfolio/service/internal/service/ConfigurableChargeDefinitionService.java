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

import org.apache.fineract.cn.portfolio.api.v1.domain.ChargeDefinition;
import org.apache.fineract.cn.portfolio.service.internal.mapper.ChargeDefinitionMapper;
import org.apache.fineract.cn.portfolio.service.internal.repository.ChargeDefinitionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.stream.Stream;

/**
 * @author Myrle Krantz
 */
@Service
public class ConfigurableChargeDefinitionService {
  private final ChargeDefinitionRepository chargeDefinitionRepository;

  @Autowired
  public ConfigurableChargeDefinitionService(final ChargeDefinitionRepository chargeDefinitionRepository) {
    this.chargeDefinitionRepository = chargeDefinitionRepository;
  }

  public Stream<ChargeDefinition> findAllEntities(final String productIdentifier) {
    return chargeDefinitionRepository.findByProductId(productIdentifier)
            .map(ChargeDefinitionMapper::map);
  }

  public Optional<ChargeDefinition> findByIdentifier(final String productIdentifier, final String identifier) {
    return chargeDefinitionRepository
            .findByProductIdAndChargeDefinitionIdentifier(productIdentifier, identifier)
            .map(ChargeDefinitionMapper::map);
  }
}
