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

import io.mifos.core.api.util.UserContextHolder;
import io.mifos.core.lang.DateConverter;
import io.mifos.portfolio.api.v1.domain.*;
import io.mifos.portfolio.service.internal.repository.ProductAccountAssignmentEntity;
import io.mifos.portfolio.service.internal.repository.ProductEntity;
import io.mifos.portfolio.service.internal.util.AccountingAdapter;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

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
            .stream().map(ProductMapper::mapAccountAssignmentEntity).collect(Collectors.toSet()));
    product.setParameters(productEntity.getParameters());
    product.setCreatedBy(productEntity.getCreatedBy());
    product.setCreatedOn(DateConverter.toIsoString(productEntity.getCreatedOn()));
    product.setLastModifiedBy(productEntity.getLastModifiedBy());
    product.setLastModifiedOn(DateConverter.toIsoString(productEntity.getLastModifiedOn()));

    return product;
  }

  public static ProductEntity map(final Product product, boolean enabled)
  {
    final ProductEntity ret = new ProductEntity();

    ret.setIdentifier(product.getIdentifier());
    ret.setName(product.getName());
    ret.setTermRangeTemporalUnit(product.getTermRange().getTemporalUnit());
    ret.setTermRangeMinimum(0);
    ret.setTermRangeMaximum(product.getTermRange().getMaximum());
    ret.setBalanceRangeMinimum(product.getBalanceRange().getMinimum());
    ret.setBalanceRangeMaximum(product.getBalanceRange().getMaximum());
    ret.setInterestRangeMinimum(product.getInterestRange().getMinimum());
    ret.setInterestRangeMaximum(product.getInterestRange().getMaximum());
    ret.setInterestBasis(product.getInterestBasis());
    ret.setPatternPackage(product.getPatternPackage());
    ret.setDescription(product.getDescription());
    ret.setCurrencyCode(product.getCurrencyCode());
    ret.setMinorCurrencyUnitDigits(product.getMinorCurrencyUnitDigits());
    ret.setAccountAssignments(product.getAccountAssignments().stream()
            .map(x -> ProductMapper.map(x, ret))
            .collect(Collectors.toSet()));
    ret.setParameters(product.getParameters());
    ret.setEnabled(enabled);

    final LocalDateTime time = LocalDateTime.now(Clock.systemUTC());
    final String user = UserContextHolder.checkedGetUser();
    ret.setCreatedOn(time);
    ret.setCreatedBy(user);
    ret.setLastModifiedOn(time);
    ret.setLastModifiedBy(user);

    return ret;
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

  public static AccountAssignment mapAccountAssignmentEntity (final ProductAccountAssignmentEntity productAccountAssignmentEntity)
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

  public static ProductEntity mapOverOldEntity(final Product instance, final ProductEntity oldEntity) {
    final ProductEntity newEntity = map(instance, false);

    newEntity.setId(oldEntity.getId());
    newEntity.setCreatedBy(oldEntity.getCreatedBy());
    newEntity.setCreatedOn(oldEntity.getCreatedOn());

    final Set<ProductAccountAssignmentEntity> oldAccountAssignmentEntities = oldEntity.getAccountAssignments();
    final Map<String, ProductAccountAssignmentEntity> accountAssignmentsMap
            = oldAccountAssignmentEntities.stream()
            .collect(Collectors.toMap(ProductAccountAssignmentEntity::getDesignator, x -> x));

    final Set<AccountAssignment> newAccountAssignments = instance.getAccountAssignments();
    final Set<ProductAccountAssignmentEntity> newAccountAssignmentEntities =
    newAccountAssignments.stream().map(x -> {
      final ProductAccountAssignmentEntity newAccountAssignmentEntity = ProductMapper.map(x, newEntity);
      final ProductAccountAssignmentEntity oldAccountAssignmentEntity = accountAssignmentsMap.get(x.getDesignator());
      if (oldAccountAssignmentEntity != null) newAccountAssignmentEntity.setId(oldAccountAssignmentEntity.getId());

      return newAccountAssignmentEntity;
    }).collect(Collectors.toSet());
    newEntity.setAccountAssignments(newAccountAssignmentEntities);

    newEntity.setChargeDefinitions(oldEntity.getChargeDefinitions());
    newEntity.setTaskDefinitions(oldEntity.getTaskDefinitions());


    return newEntity;
  }
}
