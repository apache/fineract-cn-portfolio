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
package org.apache.fineract.cn.portfolio.service.internal.command.handler;

import org.apache.fineract.cn.individuallending.api.v1.domain.product.AccountDesignators;
import org.apache.fineract.cn.portfolio.api.v1.domain.AccountAssignment;
import org.apache.fineract.cn.portfolio.api.v1.domain.ChargeDefinition;
import org.apache.fineract.cn.portfolio.api.v1.domain.Product;
import org.apache.fineract.cn.portfolio.api.v1.events.EventConstants;
import org.apache.fineract.cn.portfolio.service.internal.command.ChangeEnablingOfProductCommand;
import org.apache.fineract.cn.portfolio.service.internal.command.ChangeProductCommand;
import org.apache.fineract.cn.portfolio.service.internal.command.CreateProductCommand;
import org.apache.fineract.cn.portfolio.service.internal.command.DeleteProductCommand;
import org.apache.fineract.cn.portfolio.service.internal.mapper.ChargeDefinitionMapper;
import org.apache.fineract.cn.portfolio.service.internal.mapper.ProductMapper;
import org.apache.fineract.cn.portfolio.service.internal.pattern.PatternFactoryRegistry;
import org.apache.fineract.cn.portfolio.service.internal.repository.CaseRepository;
import org.apache.fineract.cn.portfolio.service.internal.repository.ChargeDefinitionEntity;
import org.apache.fineract.cn.portfolio.service.internal.repository.ChargeDefinitionRepository;
import org.apache.fineract.cn.portfolio.service.internal.repository.ProductAccountAssignmentEntity;
import org.apache.fineract.cn.portfolio.service.internal.repository.ProductEntity;
import org.apache.fineract.cn.portfolio.service.internal.repository.ProductRepository;
import org.apache.fineract.cn.portfolio.service.internal.util.AccountingAdapter;
import org.apache.fineract.cn.products.spi.PatternFactory;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.fineract.cn.command.annotation.Aggregate;
import org.apache.fineract.cn.command.annotation.CommandHandler;
import org.apache.fineract.cn.command.annotation.CommandLogLevel;
import org.apache.fineract.cn.command.annotation.EventEmitter;
import org.apache.fineract.cn.lang.ServiceException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

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

    patternFactory.defaultConfigurableCharges().forEach(charge -> createChargeDefinition(productEntity, charge));

    return createProductCommand.getInstance().getIdentifier();
  }

  private void createChargeDefinition(final ProductEntity productEntity, final ChargeDefinition chargeDefinition) {
    final ChargeDefinitionEntity chargeDefinitionEntity =
            ChargeDefinitionMapper.map(productEntity, chargeDefinition, null, null);
    chargeDefinitionRepository.save(chargeDefinitionEntity);
  }

  @Transactional
  @CommandHandler(logStart = CommandLogLevel.INFO, logFinish = CommandLogLevel.INFO)
  @EventEmitter(selectorName = EventConstants.SELECTOR_NAME, selectorValue = EventConstants.PUT_PRODUCT)
  public String process(final ChangeProductCommand changeProductCommand) {
    final Product instance = changeProductCommand.getInstance();

    if (caseRepository.existsByProductIdentifier(instance.getIdentifier()))
      throw ServiceException
          .conflict("Cases exist for product with the identifier '" + instance.getIdentifier() + "'. Product cannot be changed.");

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
      final Stream<ChargeDefinition> chargeDefinitions = chargeDefinitionRepository
              .findByProductId(productEntity.getIdentifier())
              .map(ChargeDefinitionMapper::map);

      createAndAssignProductLossAllowanceAccountIfNecessary(productEntity, accountingAdapter);

      final Set<String> accountAssignmentsRequiredButNotProvided
          = AccountingAdapter.accountAssignmentsRequiredButNotProvided(accountAssignments, chargeDefinitions);
      if (!accountAssignmentsRequiredButNotProvided.isEmpty())
        throw ServiceException.conflict("Not ready to enable product ''{0}''. One or more of the charge definitions " +
            "contains a designator for which no account assignment exists. Here are the unassigned designators ''{1}''",
            changeEnablingOfProductCommand.getProductIdentifier(), accountAssignmentsRequiredButNotProvided);

      final Set<String> accountAssignmentsMappedToNonexistentAccounts = accountingAdapter.accountAssignmentsMappedToNonexistentAccounts(accountAssignments);
      if (!accountAssignmentsMappedToNonexistentAccounts.isEmpty())
        throw ServiceException.conflict("Not ready to enable product ''{0}''. The following account assignments point " +
            "to an account or ledger which does not exist ''{1}''.", changeEnablingOfProductCommand.getProductIdentifier(),
            accountAssignmentsMappedToNonexistentAccounts);
    }

    productEntity.setEnabled(changeEnablingOfProductCommand.getEnabled());

    this.productRepository.save(productEntity);

    return changeEnablingOfProductCommand.getProductIdentifier();
  }

  static void createAndAssignProductLossAllowanceAccountIfNecessary(
      final ProductEntity productEntity,
      final AccountingAdapter accountingAdapter) {
    final Map<String, ProductAccountAssignmentEntity> accountAssignmentEntityMap
        = productEntity.getAccountAssignments().stream()
        .collect(Collectors.toMap(ProductAccountAssignmentEntity::getDesignator, Function.identity()));
    final Optional<ProductAccountAssignmentEntity> productLossAllowanceMapping
        = Optional.ofNullable(accountAssignmentEntityMap.get(AccountDesignators.PRODUCT_LOSS_ALLOWANCE));

    final boolean productLossAccountIsMappedToAccount = productLossAllowanceMapping
        .map(x -> x.getType() == AccountingAdapter.IdentifierType.ACCOUNT)
        .orElse(false);
    if (productLossAccountIsMappedToAccount)
      return; //Already done.

    final boolean productLossAccountIsMappedToLedger = productLossAllowanceMapping
        .map(x -> x.getType() == AccountingAdapter.IdentifierType.LEDGER)
        .orElse(false);
    if (productLossAccountIsMappedToLedger)
      throw ServiceException.conflict("Not ready to enable product ''{0}''.  The account assignment for product loss allowance is mapped to a ledger.",
          productEntity.getIdentifier());

    final String loanPrincipalLedgerMapping
        = Optional.ofNullable(accountAssignmentEntityMap.get(AccountDesignators.CUSTOMER_LOAN_PRINCIPAL))
        .flatMap(x -> {
            if (x.getType() != AccountingAdapter.IdentifierType.LEDGER)
              return Optional.empty();
            else
              return Optional.of(x.getIdentifier());
        })
        .orElseThrow(() ->
            ServiceException.conflict("Not ready to enable product ''{0}''.  The customer loan principal account is not mapped to a ledger.",
                productEntity.getIdentifier()));

    final String accountIdentifier = accountingAdapter.createProductAccountForLedgerAssignment(
        productEntity.getIdentifier(),
        AccountDesignators.PRODUCT_LOSS_ALLOWANCE,
        loanPrincipalLedgerMapping);

    final AccountAssignment productLossAccountAssignment = new AccountAssignment(AccountDesignators.PRODUCT_LOSS_ALLOWANCE, accountIdentifier);
    productEntity.getAccountAssignments().add(ProductMapper.map(productLossAccountAssignment, productEntity));
  }
}
