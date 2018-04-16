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

import org.apache.fineract.cn.portfolio.api.v1.domain.Case;
import org.apache.fineract.cn.portfolio.api.v1.domain.CasePage;
import org.apache.fineract.cn.portfolio.api.v1.domain.CaseStatus;
import org.apache.fineract.cn.portfolio.api.v1.domain.Payment;
import org.apache.fineract.cn.portfolio.service.internal.mapper.CaseMapper;
import org.apache.fineract.cn.portfolio.service.internal.pattern.PatternFactoryRegistry;
import org.apache.fineract.cn.portfolio.service.internal.repository.CaseEntity;
import org.apache.fineract.cn.portfolio.service.internal.repository.CaseRepository;
import org.apache.fineract.cn.portfolio.service.internal.repository.ProductEntity;
import org.apache.fineract.cn.portfolio.service.internal.repository.ProductRepository;
import org.apache.fineract.cn.products.spi.PatternFactory;
import org.apache.fineract.cn.products.spi.ProductCommandDispatcher;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.fineract.cn.lang.ServiceException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

/**
 * @author Myrle Krantz
 */
@Service
public class CaseService {
  private final PatternFactoryRegistry patternFactoryRegistry;
  private final ProductRepository productRepository;
  private final CaseRepository caseRepository;

  @Autowired
  public CaseService(
          final PatternFactoryRegistry patternFactoryRegistry,
          final ProductRepository productRepository,
          final CaseRepository caseRepository) {
    this.patternFactoryRegistry = patternFactoryRegistry;
    this.productRepository = productRepository;
    this.caseRepository = caseRepository;
  }

  public CasePage findAllEntities(final String productIdentifier,
                                  final Boolean includeClosed,
                                  final int pageIndex,
                                  final int size) {
    final Pageable pageRequest = new PageRequest(pageIndex, size, Sort.Direction.DESC, "lastModifiedOn");

    Stream<Case.State> currentStatesStream = Arrays.stream(Case.State.values());
    if (!includeClosed)
      currentStatesStream = currentStatesStream.filter(x -> x != Case.State.CLOSED);
    final List<String> currentStates = currentStatesStream.map(Enum::name).collect(Collectors.toList());

    final Page<CaseEntity> ret = caseRepository.findByProductIdentifierAndCurrentStateIn(productIdentifier, currentStates, pageRequest);
    final int minorCurrencyUnitDigits = getMinorCurrencyUnitDigits(productIdentifier);

    return new CasePage(mapList(ret.getContent(), minorCurrencyUnitDigits), ret.getTotalPages(), ret.getTotalElements());
  }

  private List<Case> mapList(final List<CaseEntity> in,
                             final int minorCurrencyUnitDigits) {
    return in.stream()
            .map(caseEntity -> map(caseEntity, minorCurrencyUnitDigits))
            .filter(Optional::isPresent)
            .map(Optional::get)
            .collect(Collectors.toList());
  }

  public Optional<Case> findByIdentifier(final String productIdentifier, final String caseIdentifier)
  {
    final int minorCurrencyUnitDigits = getMinorCurrencyUnitDigits(productIdentifier);
    return caseRepository.findByProductIdentifierAndIdentifier(productIdentifier, caseIdentifier)
            .flatMap(caseEntity -> map(caseEntity, minorCurrencyUnitDigits));
  }

  public Optional<CaseStatus> findStatusByIdentifier(final String productIdentifier, final String caseIdentifier)
  {
    return caseRepository.findByProductIdentifierAndIdentifier(productIdentifier, caseIdentifier)
        .map(CaseMapper::mapToStatus);
  }

  public Set<String> getNextActionsForCase(final String productIdentifier, final String caseIdentifier) {
    final PatternFactory pattern = getPatternFactoryOrThrow(productIdentifier);

    return caseRepository.findByProductIdentifierAndIdentifier(productIdentifier, caseIdentifier)
            .map(x -> pattern.getNextActionsForState(Case.State.valueOf(x.getCurrentState())))
            .orElseThrow(() -> ServiceException
                .notFound("Case with identifier ''" + productIdentifier + "." + caseIdentifier + "'' doesn''t exist."));
  }

  public ProductCommandDispatcher getProductCommandDispatcher(final String productIdentifier) {
    return getPatternFactoryOrThrow(productIdentifier).getIndividualLendingCommandDispatcher();
  }

  private Optional<Case> map(final CaseEntity caseEntity, final int minorCurrencyUnitDigits) {
    return getPatternFactory(caseEntity.getProductIdentifier())
        .flatMap(x -> x.getParameters(caseEntity.getId(), minorCurrencyUnitDigits))
        .map(x -> CaseMapper.map(caseEntity, x));
  }

  private PatternFactory getPatternFactoryOrThrow(final String productIdentifier) {
    return getPatternFactory(productIdentifier)
            .orElseThrow(() -> ServiceException.notFound("Product with identifier ''" + productIdentifier + "'' doesn''t exist."));
  }

  private Optional<PatternFactory> getPatternFactory(final String productIdentifier) {
    return productRepository.findByIdentifier(productIdentifier)
              .map(ProductEntity::getPatternPackage)
              .flatMap(patternFactoryRegistry::getPatternFactoryForPackage);
  }

  public boolean existsByProductIdentifier(final String productIdentifier) {
    return caseRepository.existsByProductIdentifier(productIdentifier);
  }

  public boolean existsByIdentifier(final String productIdentifier,
                                    final String caseIdentifier) {
    return this.findByIdentifier(productIdentifier, caseIdentifier).isPresent();
  }

  public Payment getActionCostComponentsForCase(final String productIdentifier,
                                                final String caseIdentifier,
                                                final String actionIdentifier,
                                                final LocalDateTime localDateTime,
                                                final Set<String> forAccountDesignatorsList,
                                                final BigDecimal forPaymentSize) {
    return getPatternFactoryOrThrow(productIdentifier).getCostComponentsForAction(
        productIdentifier,
        caseIdentifier,
        actionIdentifier,
        localDateTime,
        forAccountDesignatorsList,
        forPaymentSize);
  }

  private int getMinorCurrencyUnitDigits(final String productIdentifier) {
    return productRepository.findByIdentifier(productIdentifier)
        .map(ProductEntity::getMinorCurrencyUnitDigits)
        .orElse(4);
  }
}