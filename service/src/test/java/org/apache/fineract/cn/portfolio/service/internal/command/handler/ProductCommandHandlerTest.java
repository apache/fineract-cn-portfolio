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
import org.apache.fineract.cn.portfolio.service.internal.repository.ProductAccountAssignmentEntity;
import org.apache.fineract.cn.portfolio.service.internal.repository.ProductEntity;
import org.apache.fineract.cn.portfolio.service.internal.util.AccountingAdapter;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import org.apache.fineract.cn.lang.ServiceException;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

/**
 * @author Myrle Krantz
 */
public class ProductCommandHandlerTest {
  @Test
  public void createAndAssignProductLossAllowanceAccountIfNecessary() {
    final ProductAccountAssignmentEntity customerPrincipalAccountAssignmentEntity = new ProductAccountAssignmentEntity();
    customerPrincipalAccountAssignmentEntity.setType(AccountingAdapter.IdentifierType.LEDGER);
    customerPrincipalAccountAssignmentEntity.setIdentifier("principalledgeridentifier");
    customerPrincipalAccountAssignmentEntity.setDesignator(AccountDesignators.CUSTOMER_LOAN_PRINCIPAL);
    final Set<ProductAccountAssignmentEntity> accountAssignments = new HashSet<>(Collections.singletonList(customerPrincipalAccountAssignmentEntity));
    final ProductEntity productEntity = new ProductEntity();
    productEntity.setId(20L);
    productEntity.setIdentifier("productxyz");
    productEntity.setAccountAssignments(accountAssignments);
    final AccountingAdapter accountingAdapter = Mockito.mock(AccountingAdapter.class);
    Mockito.doReturn("accountIdentifierBlah")
        .when(accountingAdapter)
        .createProductAccountForLedgerAssignment(
            productEntity.getIdentifier(),
            AccountDesignators.PRODUCT_LOSS_ALLOWANCE,
            customerPrincipalAccountAssignmentEntity.getIdentifier());

    ProductCommandHandler.createAndAssignProductLossAllowanceAccountIfNecessary(productEntity, accountingAdapter);

    Assert.assertTrue(productEntity.getAccountAssignments().stream()
        .anyMatch(x ->
            x.getDesignator().equals(AccountDesignators.PRODUCT_LOSS_ALLOWANCE) &&
            x.getType().equals(AccountingAdapter.IdentifierType.ACCOUNT) &&
            x.getIdentifier().equals("accountIdentifierBlah")
        ));
  }


  @Test(expected = ServiceException.class)
  public void shouldThrowWhenPrincipalAccountAssignmentMissingOnCreateAndAssignProductLossAllowanceAccountIfNecessary() {
    final Set<ProductAccountAssignmentEntity> accountAssignments = new HashSet<>();
    final ProductEntity productEntity = new ProductEntity();
    productEntity.setId(20L);
    productEntity.setIdentifier("productxyz");
    productEntity.setAccountAssignments(accountAssignments);

    ProductCommandHandler.createAndAssignProductLossAllowanceAccountIfNecessary(productEntity, null);
  }

  @Test(expected = ServiceException.class)
  public void shouldThrowWhenPrincipalAccountAssignedToAccountOnCreateAndAssignProductLossAllowanceAccountIfNecessary() {
    final ProductAccountAssignmentEntity customerPrincipalAccountAssignmentEntity = new ProductAccountAssignmentEntity();
    customerPrincipalAccountAssignmentEntity.setType(AccountingAdapter.IdentifierType.ACCOUNT);
    customerPrincipalAccountAssignmentEntity.setIdentifier("principalledgeridentifier");
    customerPrincipalAccountAssignmentEntity.setDesignator(AccountDesignators.CUSTOMER_LOAN_PRINCIPAL);
    final Set<ProductAccountAssignmentEntity> accountAssignments = new HashSet<>(Collections.singletonList(customerPrincipalAccountAssignmentEntity));
    final ProductEntity productEntity = new ProductEntity();
    productEntity.setId(20L);
    productEntity.setIdentifier("productxyz");
    productEntity.setAccountAssignments(accountAssignments);

    ProductCommandHandler.createAndAssignProductLossAllowanceAccountIfNecessary(productEntity, null);
  }

  @Test(expected = ServiceException.class)
  public void shouldThrowWhenLossAccountToLedgerOnCreateAndAssignProductLossAllowanceAccountIfNecessary() {
    final ProductAccountAssignmentEntity customerPrincipalAccountAssignmentEntity = new ProductAccountAssignmentEntity();
    customerPrincipalAccountAssignmentEntity.setType(AccountingAdapter.IdentifierType.LEDGER);
    customerPrincipalAccountAssignmentEntity.setIdentifier("principalledgeridentifier");
    customerPrincipalAccountAssignmentEntity.setDesignator(AccountDesignators.CUSTOMER_LOAN_PRINCIPAL);
    final ProductAccountAssignmentEntity productLossAccountAssignmentEntity = new ProductAccountAssignmentEntity();
    productLossAccountAssignmentEntity.setType(AccountingAdapter.IdentifierType.LEDGER);
    productLossAccountAssignmentEntity.setIdentifier("productLossLedgeridentifier");
    productLossAccountAssignmentEntity.setDesignator(AccountDesignators.PRODUCT_LOSS_ALLOWANCE);
    final Set<ProductAccountAssignmentEntity> accountAssignments = new HashSet<>(Arrays.asList(customerPrincipalAccountAssignmentEntity, productLossAccountAssignmentEntity));
    final ProductEntity productEntity = new ProductEntity();
    productEntity.setId(20L);
    productEntity.setIdentifier("productxyz");
    productEntity.setAccountAssignments(accountAssignments);

    ProductCommandHandler.createAndAssignProductLossAllowanceAccountIfNecessary(productEntity, null);
  }

  @Test
  public void shouldDoNothingWhenAccountIsAlreadyAssignedOnCreateAndAssignProductLossAllowanceAccountIfNecessary() {
    final ProductAccountAssignmentEntity customerPrincipalAccountAssignmentEntity = new ProductAccountAssignmentEntity();
    customerPrincipalAccountAssignmentEntity.setType(AccountingAdapter.IdentifierType.LEDGER);
    customerPrincipalAccountAssignmentEntity.setIdentifier("principalledgeridentifier");
    customerPrincipalAccountAssignmentEntity.setDesignator(AccountDesignators.CUSTOMER_LOAN_PRINCIPAL);
    final ProductAccountAssignmentEntity productLossAccountAssignmentEntity = new ProductAccountAssignmentEntity();
    productLossAccountAssignmentEntity.setType(AccountingAdapter.IdentifierType.ACCOUNT);
    productLossAccountAssignmentEntity.setIdentifier("productLossAccountIdentifier");
    productLossAccountAssignmentEntity.setDesignator(AccountDesignators.PRODUCT_LOSS_ALLOWANCE);
    final Set<ProductAccountAssignmentEntity> accountAssignments = new HashSet<>(Arrays.asList(customerPrincipalAccountAssignmentEntity, productLossAccountAssignmentEntity));
    final ProductEntity productEntity = new ProductEntity();
    productEntity.setId(20L);
    productEntity.setIdentifier("productxyz");
    productEntity.setAccountAssignments(accountAssignments);

    ProductCommandHandler.createAndAssignProductLossAllowanceAccountIfNecessary(productEntity, null);
  }
}