/*
 * Copyright 2017 Kuelap, Inc.
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
import io.mifos.individuallending.internal.repository.CaseParametersEntity;
import io.mifos.individuallending.internal.repository.CaseParametersRepository;
import io.mifos.portfolio.api.v1.domain.AccountAssignment;
import io.mifos.portfolio.service.internal.repository.CaseEntity;
import io.mifos.portfolio.service.internal.repository.CaseRepository;
import io.mifos.portfolio.service.internal.repository.ProductEntity;
import io.mifos.portfolio.service.internal.repository.ProductRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.Nullable;
import java.util.List;

/**
 * @author Myrle Krantz
 */
@Service
public class DataContextService {
  private final ProductRepository productRepository;
  private final CaseRepository caseRepository;
  private final CaseParametersRepository caseParametersRepository;

  @Autowired
  public DataContextService(
      final ProductRepository productRepository,
      final CaseRepository caseRepository,
      final CaseParametersRepository caseParametersRepository) {
    this.productRepository = productRepository;
    this.caseRepository = caseRepository;
    this.caseParametersRepository = caseParametersRepository;
  }

  public DataContextOfAction checkedGetDataContext(
      final String productIdentifier,
      final String caseIdentifier,
      final @Nullable List<AccountAssignment> oneTimeAccountAssignments) {

    final ProductEntity product =
        productRepository.findByIdentifier(productIdentifier)
            .orElseThrow(() -> ServiceException.notFound("Product not found ''{0}''.", productIdentifier));
    final CaseEntity customerCase =
        caseRepository.findByProductIdentifierAndIdentifier(productIdentifier, caseIdentifier)
            .orElseThrow(() -> ServiceException.notFound("Case not found ''{0}.{1}''.", productIdentifier, caseIdentifier));

    final CaseParametersEntity caseParameters =
        caseParametersRepository.findByCaseId(customerCase.getId())
            .orElseThrow(() -> ServiceException.notFound(
                "Individual loan not found ''{0}.{1}''.",
                productIdentifier, caseIdentifier));

    return new DataContextOfAction(
        product,
        customerCase,
        caseParameters,
        oneTimeAccountAssignments);
  }
}