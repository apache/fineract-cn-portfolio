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
package io.mifos.portfolio.service.internal.command.handler;

import io.mifos.core.command.annotation.Aggregate;
import io.mifos.core.command.annotation.CommandHandler;
import io.mifos.core.command.annotation.CommandLogLevel;
import io.mifos.core.command.annotation.EventEmitter;
import io.mifos.core.lang.ServiceException;
import io.mifos.portfolio.api.v1.domain.AccountAssignment;
import io.mifos.portfolio.api.v1.domain.ChargeDefinition;
import io.mifos.portfolio.api.v1.domain.Product;
import io.mifos.portfolio.api.v1.events.EventConstants;
import io.mifos.portfolio.service.internal.command.ChangeEnablingOfProductCommand;
import io.mifos.portfolio.service.internal.command.ChangeProductCommand;
import io.mifos.portfolio.service.internal.command.CreateProductCommand;
import io.mifos.portfolio.service.internal.command.DeleteProductCommand;
import io.mifos.portfolio.service.internal.mapper.ChargeDefinitionMapper;
import io.mifos.portfolio.service.internal.mapper.ProductMapper;
import io.mifos.portfolio.service.internal.pattern.PatternFactoryRegistry;
import io.mifos.portfolio.service.internal.repository.*;
import io.mifos.portfolio.service.internal.util.AccountingAdapter;
import io.mifos.products.spi.PatternFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author Myrle Krantz
 */
@SuppressWarnings("unused")
@Aggregate
public class ProductCommandHandler {
  private final PatternFactoryRegistry patternFactoryRegistry;
  private final CaseRepository caseRepository;
  private final ProductRepository productRepository;
  private final ChargeDefinitionRepository chargeDefinitionRepository;
  private final AccountingAdapter accountingAdapter;

  @Autowired
  public ProductCommandHandler(
          final PatternFactoryRegistry patternFactoryRegistry,
          final CaseRepository caseRepository,
          final ProductRepository productRepository,
          final ChargeDefinitionRepository chargeDefinitionRepository,
          final AccountingAdapter accountingAdapter) {
    super();
    this.patternFactoryRegistry = patternFactoryRegistry;
    this.caseRepository = caseRepository;
    this.productRepository = productRepository;
    this.chargeDefinitionRepository = chargeDefinitionRepository;
    this.accountingAdapter = accountingAdapter;
  }

  @Transactional
  @CommandHandler(logStart = CommandLogLevel.INFO, logFinish = CommandLogLevel.INFO)
  @EventEmitter(selectorName = EventConstants.SELECTOR_NAME, selectorValue = EventConstants.POST_PRODUCT)
  public String process(final CreateProductCommand createProductCommand) {
    final PatternFactory patternFactory = patternFactoryRegistry
            .getPatternFactoryForPackage(createProductCommand.getInstance().getPatternPackage())
            .orElseThrow(IllegalArgumentException::new);
    final ProductEntity productEntity = ProductMapper.map(createProductCommand.getInstance(), false);
    this.productRepository.save(productEntity);

    patternFactory.charges().forEach(charge -> createChargeDefinition(productEntity, charge));

    return createProductCommand.getInstance().getIdentifier();
  }

  private void createChargeDefinition(final ProductEntity productEntity, final ChargeDefinition chargeDefinition) {
    final ChargeDefinitionEntity chargeDefinitionEntity =
            ChargeDefinitionMapper.map(productEntity, chargeDefinition);
    chargeDefinitionRepository.save(chargeDefinitionEntity);
  }

  @Transactional
  @CommandHandler(logStart = CommandLogLevel.INFO, logFinish = CommandLogLevel.INFO)
  @EventEmitter(selectorName = EventConstants.SELECTOR_NAME, selectorValue = EventConstants.PUT_PRODUCT)
  public String process(final ChangeProductCommand changeProductCommand) {
    final Product instance = changeProductCommand.getInstance();

    if (caseRepository.existsByProductIdentifier(instance.getIdentifier()))
      throw ServiceException.conflict("Cases exist for product with the identifier '" + instance.getIdentifier() + "'. Product cannot be changed.");

    final ProductEntity oldEntity = productRepository
            .findByIdentifier(instance.getIdentifier())
            .orElseThrow(() -> ServiceException.notFound("Product not found '" + instance.getIdentifier() + "'."));

    final ProductEntity newEntity = ProductMapper.mapOverOldEntity(instance, oldEntity);

    productRepository.save(newEntity);

    return changeProductCommand.getInstance().getIdentifier();
  }

  @Transactional
  @CommandHandler(logStart = CommandLogLevel.INFO, logFinish = CommandLogLevel.INFO)
  @EventEmitter(selectorName = EventConstants.SELECTOR_NAME, selectorValue = EventConstants.DELETE_PRODUCT)
  public String process(final DeleteProductCommand deleteProductCommand) {
    final String productIdentifier = deleteProductCommand.getProductIdentifier();
    final ProductEntity product = productRepository.findByIdentifier(productIdentifier)
            .orElseThrow(() -> ServiceException.notFound("Instance with identifier ''{0}'' doesn''t exist.", productIdentifier));

    if (product.getEnabled())
      throw ServiceException.conflict("Cannot delete product with identifier ''{0}'', because it is enabled.", productIdentifier);

    if (caseRepository.existsByProductIdentifier(productIdentifier))
      throw ServiceException.conflict("Cannot delete product with identifier ''{0}'', because there are already cases defined on it.", productIdentifier);

    productRepository.delete(product);

    return deleteProductCommand.getProductIdentifier();
  }

  @Transactional
  @CommandHandler(logStart = CommandLogLevel.INFO, logFinish = CommandLogLevel.INFO)
  @EventEmitter(selectorName = EventConstants.SELECTOR_NAME, selectorValue =  EventConstants.PUT_PRODUCT_ENABLE)
  public String process(final ChangeEnablingOfProductCommand changeEnablingOfProductCommand)
  {
    final ProductEntity productEntity = this.productRepository.findByIdentifier(changeEnablingOfProductCommand.getProductIdentifier())
            .orElseThrow(() -> ServiceException.notFound("Product not found '" + changeEnablingOfProductCommand.getProductIdentifier() + "'."));

    //noinspection PointlessBooleanExpression
    if (changeEnablingOfProductCommand.getEnabled() == true) {
      final Set<AccountAssignment> accountAssignments = ProductMapper.map(productEntity).getAccountAssignments();
      final List<ChargeDefinition> chargeDefinitions = chargeDefinitionRepository
              .findByProductId(productEntity.getIdentifier())
              .stream()
              .map(ChargeDefinitionMapper::map)
              .collect(Collectors.toList());

      if (!AccountingAdapter.accountAssignmentsCoverChargeDefinitions(accountAssignments, chargeDefinitions))
        throw ServiceException.conflict("Not ready to enable product '" + changeEnablingOfProductCommand.getProductIdentifier() + "'. One or more of the charge definitions contains a designator for which no account assignment exists.");

      if (!accountingAdapter.accountAssignmentsRepresentRealAccounts(accountAssignments))
        throw ServiceException.conflict("Not ready to enable product '" + changeEnablingOfProductCommand.getProductIdentifier() + "'. One or more of the account assignments points to an account or ledger which does not exist.");
    }

    productEntity.setEnabled(changeEnablingOfProductCommand.getEnabled());

    this.productRepository.save(productEntity);

    return changeEnablingOfProductCommand.getProductIdentifier();
  }
}
