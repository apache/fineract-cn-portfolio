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
package io.mifos.individuallending.internal.service;

import io.mifos.individuallending.api.v1.domain.caseinstance.CaseParameters;
import io.mifos.individuallending.api.v1.domain.workflow.Action;
import io.mifos.individuallending.internal.mapper.CaseParametersMapper;
import io.mifos.individuallending.internal.repository.CaseParametersEntity;
import io.mifos.portfolio.api.v1.domain.AccountAssignment;
import io.mifos.portfolio.service.internal.repository.CaseEntity;
import io.mifos.portfolio.service.internal.repository.ProductEntity;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;

/**
 * @author Myrle Krantz
 */
public class DataContextOfAction {
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
}