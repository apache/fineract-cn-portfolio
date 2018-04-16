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
package org.apache.fineract.cn.individuallending.internal.service;

import org.apache.fineract.cn.individuallending.api.v1.domain.caseinstance.CaseParameters;
import org.apache.fineract.cn.individuallending.api.v1.domain.workflow.Action;
import org.apache.fineract.cn.individuallending.internal.mapper.CaseParametersMapper;
import org.apache.fineract.cn.individuallending.internal.repository.CaseParametersEntity;
import org.apache.fineract.cn.portfolio.api.v1.domain.AccountAssignment;
import org.apache.fineract.cn.portfolio.service.internal.repository.CaseEntity;
import org.apache.fineract.cn.portfolio.service.internal.repository.ProductEntity;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * @author Myrle Krantz
 */
public class DataContextOfAction {
  private final static BigDecimal PAYMENT_SIZE_NOT_SET_SEMAPHORE = BigDecimal.ONE.negate();

  private final ProductEntity product;
  private final CaseEntity customerCase;
  private final CaseParametersEntity caseParameters;
  private final List<AccountAssignment> oneTimeAccountAssignments;

  public DataContextOfAction(
      final @Nonnull ProductEntity product,
      final @Nonnull CaseEntity customerCase,
      final @Nonnull CaseParametersEntity caseParameters,
      final @Nullable List<AccountAssignment> oneTimeAccountAssignments)
  {
    this.product = product;
    this.customerCase = customerCase;
    this.caseParameters = caseParameters;
    this.oneTimeAccountAssignments = oneTimeAccountAssignments == null ? Collections.emptyList() : oneTimeAccountAssignments;
  }

  public @Nonnull ProductEntity getProductEntity() {
    return product;
  }

  public @Nonnull CaseEntity getCustomerCaseEntity() {
    return customerCase;
  }

  public @Nonnull CaseParametersEntity getCaseParametersEntity() {
    return caseParameters;
  }

  public @Nonnull CaseParameters getCaseParameters() {
    return CaseParametersMapper.mapEntity(caseParameters, product.getMinorCurrencyUnitDigits());
  }

  @Nonnull List<AccountAssignment> getOneTimeAccountAssignments() {
    return oneTimeAccountAssignments;
  }

  public String getCompoundIdentifer() {
    return product.getIdentifier() + "." + customerCase.getIdentifier();
  }

  public String getMessageForCharge(final Action action) {
    return getCompoundIdentifer() + "." + action.name();
  }

  public BigDecimal getInterest() {
    return customerCase.getInterest();
  }

  public Optional<BigDecimal> getPaymentSize() {

    final BigDecimal persistedPaymentSize = caseParameters.getPaymentSize();

    if (persistedPaymentSize == null || persistedPaymentSize.compareTo(PAYMENT_SIZE_NOT_SET_SEMAPHORE) == 0)
      return Optional.empty();

    return Optional.of(persistedPaymentSize);
  }
}