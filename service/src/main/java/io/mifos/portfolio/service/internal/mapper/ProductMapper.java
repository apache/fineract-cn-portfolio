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
package io.mifos.portfolio.service.internal.mapper;

import io.mifos.portfolio.api.v1.domain.*;
import io.mifos.portfolio.service.internal.repository.ProductAccountAssignmentEntity;
import io.mifos.portfolio.service.internal.repository.ProductEntity;
import io.mifos.portfolio.service.internal.service.AccountingAdapter;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @author Myrle Krantz
 */
public class ProductMapper {
  public static Product map(final ProductEntity productEntity) {
    final Product product = new Product();
    product.setIdentifier(productEntity.getIdentifier());
    product.setName(productEntity.getName());
    product.setTermRange(
            new TermRange(productEntity.getTermRangeTemporalUnit(),
                    productEntity.getTermRangeMaximum()));
    product.setBalanceRange(
            new BalanceRange(productEntity.getBalanceRangeMinimum(), productEntity.getBalanceRangeMaximum()));
    product.setInterestRange(
            new InterestRange(productEntity.getInterestRangeMinimum(), productEntity.getInterestRangeMaximum()));
    product.setInterestBasis(productEntity.getInterestBasis());
    product.setPatternPackage(productEntity.getPatternPackage());
    product.setDescription(productEntity.getDescription());
    product.setCurrencyCode(productEntity.getCurrencyCode());
    product.setMinorCurrencyUnitDigits(productEntity.getMinorCurrencyUnitDigits());
    product.setAccountAssignments(productEntity.getAccountAssignments()
            .stream().map(ProductMapper::map).collect(Collectors.toSet()));
    product.setParameters(productEntity.getParameters());

    return product;
  }

  public static ProductEntity map(final Product product, boolean enabled)
  {
    final ProductEntity productEntity = new ProductEntity();

    productEntity.setIdentifier(product.getIdentifier());
    productEntity.setName(product.getName());
    productEntity.setTermRangeTemporalUnit(product.getTermRange().getTemporalUnit());
    productEntity.setTermRangeMinimum(0);
    productEntity.setTermRangeMaximum(product.getTermRange().getMaximum());
    productEntity.setBalanceRangeMinimum(product.getBalanceRange().getMinimum());
    productEntity.setBalanceRangeMaximum(product.getBalanceRange().getMaximum());
    productEntity.setInterestRangeMinimum(product.getInterestRange().getMinimum());
    productEntity.setInterestRangeMaximum(product.getInterestRange().getMaximum());
    productEntity.setInterestBasis(product.getInterestBasis());
    productEntity.setPatternPackage(product.getPatternPackage());
    productEntity.setDescription(product.getDescription());
    productEntity.setCurrencyCode(product.getCurrencyCode());
    productEntity.setMinorCurrencyUnitDigits(product.getMinorCurrencyUnitDigits());
    productEntity.setAccountAssignments(product.getAccountAssignments().stream()
            .map(x -> ProductMapper.map(x, productEntity))
            .collect(Collectors.toSet()));
    productEntity.setParameters(product.getParameters());
    productEntity.setEnabled(enabled);

    return productEntity;
  }

  private static ProductAccountAssignmentEntity map (final AccountAssignment accountAssignment,
                                                     final ProductEntity productEntity)
  {
    final ProductAccountAssignmentEntity ret = new ProductAccountAssignmentEntity();
    ret.setProduct(productEntity);
    ret.setDesignator(accountAssignment.getDesignator());
    if (accountAssignment.getAccountIdentifier() != null) {
      ret.setIdentifier(accountAssignment.getAccountIdentifier());
      ret.setType(AccountingAdapter.IdentifierType.ACCOUNT);
    }
    else if (accountAssignment.getLedgerIdentifier() != null) {
      ret.setIdentifier(accountAssignment.getLedgerIdentifier());
      ret.setType(AccountingAdapter.IdentifierType.LEDGER);
    }
    return ret;
  }

  private static AccountAssignment map (final ProductAccountAssignmentEntity productAccountAssignmentEntity)
  {
    final AccountAssignment ret = new AccountAssignment();
    ret.setDesignator(productAccountAssignmentEntity.getDesignator());
    if (productAccountAssignmentEntity.getType() == AccountingAdapter.IdentifierType.ACCOUNT) {
      ret.setAccountIdentifier(productAccountAssignmentEntity.getIdentifier());
    }
    else if (productAccountAssignmentEntity.getType() == AccountingAdapter.IdentifierType.LEDGER) {
      ret.setLedgerIdentifier(productAccountAssignmentEntity.getIdentifier());
    }
    return ret;
  }

  public static List<Product> map(final List<ProductEntity> productEntities) {
    return productEntities.stream().map(ProductMapper::map).collect(Collectors.toList());
  }

  public static boolean accountAssignmentsCoverChargeDefinitions(
          final Set<AccountAssignment> accountAssignments,
          final List<ChargeDefinition> chargeDefinitionEntities) {
    final Set<String> allAccountDesignatorsRequired = chargeDefinitionEntities.stream()
            .flatMap(x -> Stream.of(x.getAccrualAccountDesignator(), x.getFromAccountDesignator(), x.getToAccountDesignator()))
            .filter(x -> x != null)
            .collect(Collectors.toSet());
    final Set<String> allAccountDesignatorsDefined = accountAssignments.stream().map(AccountAssignment::getDesignator)
            .collect(Collectors.toSet());
    return allAccountDesignatorsDefined.containsAll(allAccountDesignatorsRequired);

  }
}
