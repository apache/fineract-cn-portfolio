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
package org.apache.fineract.cn.portfolio.service.internal.checker;

import org.apache.fineract.cn.portfolio.api.v1.domain.Case;
import org.apache.fineract.cn.portfolio.api.v1.domain.InterestRange;
import org.apache.fineract.cn.portfolio.api.v1.domain.Product;
import org.apache.fineract.cn.portfolio.service.internal.pattern.PatternFactoryRegistry;
import org.apache.fineract.cn.portfolio.service.internal.repository.ProductEntity;
import org.apache.fineract.cn.portfolio.service.internal.repository.ProductRepository;
import org.apache.fineract.cn.portfolio.service.internal.service.CaseService;
import org.apache.fineract.cn.portfolio.service.internal.service.ProductService;
import org.apache.fineract.cn.products.spi.PatternFactory;
import java.math.BigDecimal;
import org.apache.fineract.cn.lang.ServiceException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * @author Myrle Krantz
 */
@Component
public class CaseChecker {
  private final CaseService caseService;
  private final ProductService productService;
  private final ProductRepository productRepository;
  private final PatternFactoryRegistry patternFactoryRegistry;

  @Autowired
  public CaseChecker(final CaseService caseService,
                     final ProductService productService,
                     final ProductRepository productRepository,
                     final PatternFactoryRegistry patternFactoryRegistry) {
    this.caseService = caseService;
    this.productService = productService;
    this.productRepository = productRepository;
    this.patternFactoryRegistry = patternFactoryRegistry;
  }

  public void checkForCreate(final String productIdentifier, final Case instance) {
    caseService.findByIdentifier(productIdentifier, instance.getIdentifier())
        .ifPresent(x -> {throw ServiceException
            .conflict("Duplicate identifier: " + productIdentifier + "." + x.getIdentifier());});

    final Product product = productService.findByIdentifier(productIdentifier)
        .orElseThrow(() -> ServiceException.badRequest("Product must exist ''{0}''.", productIdentifier));
    final Boolean productEnabled = product.isEnabled();
    if (!productEnabled) {
      throw ServiceException.badRequest("Product must be enabled before cases for it can be created: " + productIdentifier);}

    validateParameters(productIdentifier, instance, product);
  }

  public void checkForChange(final String productIdentifier, final Case instance) {
    final Product product = productService.findByIdentifier(productIdentifier)
        .orElseThrow(() -> ServiceException.badRequest("Product must exist ''{0}''.", productIdentifier));

    final Case.State currentState = Case.State.valueOf(instance.getCurrentState());
    if (currentState.equals(Case.State.ACTIVE) || currentState.equals(Case.State.CLOSED) || currentState.equals(Case.State.APPROVED))
      throw ServiceException.badRequest("You may not change a case after it has been approved or closed.");

    validateParameters(productIdentifier, instance, product);
  }

  private void validateParameters(
      final String productIdentifier,
      final Case instance,
      final Product product) {
    final InterestRange interestRange = product.getInterestRange();

    final BigDecimal interest = instance.getInterest();
    if (interest.compareTo(interestRange.getMinimum()) < 0 ||
        interest.compareTo(interestRange.getMaximum()) > 0)
      throw ServiceException.badRequest("Interest for the case ({0}) must be within the range defined by the product ({1}).", interest, interestRange.toString());

    getPatternFactory(productIdentifier).checkParameters(instance.getParameters());
  }

  private PatternFactory getPatternFactory(final String productIdentifier) {
    return productRepository.findByIdentifier(productIdentifier)
        .map(ProductEntity::getPatternPackage)
        .flatMap(patternFactoryRegistry::getPatternFactoryForPackage)
        .orElseThrow(() -> new IllegalArgumentException("Case references unsupported product type."));
  }
}