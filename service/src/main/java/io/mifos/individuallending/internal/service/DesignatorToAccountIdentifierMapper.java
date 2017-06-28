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

import io.mifos.core.lang.ServiceException;
import io.mifos.individuallending.api.v1.domain.product.AccountDesignators;
import io.mifos.portfolio.api.v1.domain.AccountAssignment;
import io.mifos.portfolio.service.internal.mapper.CaseMapper;
import io.mifos.portfolio.service.internal.mapper.ProductMapper;
import io.mifos.portfolio.service.internal.repository.CaseAccountAssignmentEntity;
import io.mifos.portfolio.service.internal.repository.ProductAccountAssignmentEntity;

import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

/**
 * @author Myrle Krantz
 */
public class DesignatorToAccountIdentifierMapper {
  private final Set<ProductAccountAssignmentEntity> productAccountAssignments;
  private final Set<CaseAccountAssignmentEntity> caseAccountAssignments;
  private final List<AccountAssignment> oneTimeAccountAssignments;

  public DesignatorToAccountIdentifierMapper(final DataContextOfAction dataContextOfAction) {
    this.productAccountAssignments = dataContextOfAction.getProduct().getAccountAssignments();
    this.caseAccountAssignments = dataContextOfAction.getCustomerCase().getAccountAssignments();
    this.oneTimeAccountAssignments = dataContextOfAction.getOneTimeAccountAssignments();
  }

  private Stream<AccountAssignment> allAccountAssignmentsAsStream() {
    return Stream.concat(oneTimeAccountAssignments.stream(), fixedAccountAssignmentsAsStream());
  }

  private Stream<AccountAssignment> fixedAccountAssignmentsAsStream() {
    return Stream.concat(caseAccountAssignments.stream().map(CaseMapper::mapAccountAssignmentEntity),
            productAccountAssignments.stream().map(ProductMapper::mapAccountAssignmentEntity));
  }

  public String mapOrThrow(final String accountDesignator) {
    return allAccountAssignmentsAsStream()
            .filter(x -> x.getDesignator().equals(accountDesignator))
            .findFirst()
            .map(AccountAssignment::getAccountIdentifier)
            .orElseThrow(() -> ServiceException.badRequest("A required account designator was not set ''{0}''.", accountDesignator));
  }

  public Stream<AccountAssignment> getLedgersNeedingAccounts() {
    return fixedAccountAssignmentsAsStream()
            .filter(x -> !x.getDesignator().equals(AccountDesignators.ENTRY))
            .filter(x -> (x.getAccountIdentifier() == null) && (x.getLedgerIdentifier() != null));
  }
}
