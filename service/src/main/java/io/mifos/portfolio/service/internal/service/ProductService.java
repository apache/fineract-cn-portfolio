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

import io.mifos.portfolio.api.v1.domain.AccountAssignment;
import io.mifos.portfolio.api.v1.domain.ChargeDefinition;
import io.mifos.portfolio.api.v1.domain.Product;
import io.mifos.portfolio.service.internal.mapper.ProductMapper;
import io.mifos.portfolio.service.internal.repository.ProductEntity;
import io.mifos.portfolio.service.internal.repository.ProductRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * @author Myrle Krantz
 */
@Service
public class ProductService {

  private final ProductRepository productRepository;
  private final ChargeDefinitionService chargeDefinitionService;

  @Autowired
  public ProductService(final ProductRepository productRepository,
                        final ChargeDefinitionService chargeDefinitionService) {
    super();
    this.productRepository = productRepository;
    this.chargeDefinitionService = chargeDefinitionService;
  }

  public List<Product> findAllEntities() {
    return ProductMapper.map(this.productRepository.findAll());
  }

  public Optional<Product> findByIdentifier(final String identifier)
  {
    return productRepository.findByIdentifier(identifier).map(ProductMapper::map);
  }

  public Optional<Boolean> findEnabledByIdentifier(final String identifier) {
    return productRepository.findByIdentifier(identifier).map(ProductEntity::getEnabled);
  }

  public Boolean isProductReadyToBeEnabled(final String identifier) {
    final Optional<Product> maybeProduct = findByIdentifier(identifier);
    if (!maybeProduct.isPresent())
      return false;
    final Product product = maybeProduct.get();
    final Set<AccountAssignment> accountAssignments = product.getAccountAssignments();
    final List<ChargeDefinition> chargeDefinitions = chargeDefinitionService.findAllEntities(identifier);
    return ProductMapper.accountAssignmentsCoverChargeDefinitions(accountAssignments, chargeDefinitions);
  }
}
