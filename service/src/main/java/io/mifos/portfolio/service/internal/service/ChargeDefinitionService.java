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
package io.mifos.portfolio.service.internal.service;

import io.mifos.portfolio.api.v1.domain.ChargeDefinition;
import io.mifos.portfolio.service.internal.mapper.ChargeDefinitionMapper;
import io.mifos.portfolio.service.internal.repository.ChargeDefinitionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * @author Myrle Krantz
 */
@Service
public class ChargeDefinitionService {
  private final ChargeDefinitionRepository chargeDefinitionRepository;

  @Autowired
  public ChargeDefinitionService(final ChargeDefinitionRepository chargeDefinitionRepository) {
    this.chargeDefinitionRepository = chargeDefinitionRepository;
  }

  public List<ChargeDefinition> findAllEntities(final String productIdentifier) {
    return chargeDefinitionRepository.findByProductId(productIdentifier).stream()
            .map(ChargeDefinitionMapper::map)
            .collect(Collectors.toList());
  }

  @Nonnull
  public Map<String, List<ChargeDefinition>> getChargeDefinitionsMappedByChargeAction(
          final String productIdentifier)
  {
    final List<ChargeDefinition> chargeDefinitions = findAllEntities(productIdentifier);

    return chargeDefinitions.stream()
            .collect(Collectors.groupingBy(ChargeDefinition::getChargeAction,
                    Collectors.mapping(x -> x, Collectors.toList())));
  }

  @Nonnull
  public Map<String, List<ChargeDefinition>> getChargeDefinitionsMappedByAccrueAction(
          final String productIdentifier)
  {
    final List<ChargeDefinition> chargeDefinitions = findAllEntities(productIdentifier);

    return chargeDefinitions.stream()
            .filter(x -> x.getAccrueAction() != null)
            .collect(Collectors.groupingBy(ChargeDefinition::getAccrueAction,
                    Collectors.mapping(x -> x, Collectors.toList())));
  }

  public Optional<ChargeDefinition> findByIdentifier(final String productIdentifier, final String identifier) {
    return chargeDefinitionRepository
            .findByProductIdAndChargeDefinitionIdentifier(productIdentifier, identifier)
            .map(ChargeDefinitionMapper::map);
  }
}
