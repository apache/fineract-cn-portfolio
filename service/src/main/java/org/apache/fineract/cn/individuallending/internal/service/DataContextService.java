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

import com.google.gson.Gson;
import org.apache.fineract.cn.individuallending.api.v1.domain.caseinstance.CaseParameters;
import org.apache.fineract.cn.individuallending.internal.mapper.CaseParametersMapper;
import org.apache.fineract.cn.individuallending.internal.repository.CaseParametersEntity;
import org.apache.fineract.cn.individuallending.internal.repository.CaseParametersRepository;
import org.apache.fineract.cn.portfolio.api.v1.domain.AccountAssignment;
import org.apache.fineract.cn.portfolio.api.v1.domain.Case;
import org.apache.fineract.cn.portfolio.service.ServiceConstants;
import org.apache.fineract.cn.portfolio.service.internal.mapper.CaseMapper;
import org.apache.fineract.cn.portfolio.service.internal.repository.CaseEntity;
import org.apache.fineract.cn.portfolio.service.internal.repository.CaseRepository;
import org.apache.fineract.cn.portfolio.service.internal.repository.ProductEntity;
import org.apache.fineract.cn.portfolio.service.internal.repository.ProductRepository;
import java.util.List;
import javax.annotation.Nullable;
import org.apache.fineract.cn.lang.ServiceException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

/**
 * @author Myrle Krantz
 */
@Service
public class DataContextService {
  private final ProductRepository productRepository;
  private final CaseRepository caseRepository;
  private final CaseParametersRepository caseParametersRepository;
  private final Gson gson;

  @Autowired
  public DataContextService(
      final ProductRepository productRepository,
      final CaseRepository caseRepository,
      final CaseParametersRepository caseParametersRepository,
      @Qualifier(ServiceConstants.GSON_NAME) final Gson gson) {
    this.productRepository = productRepository;
    this.caseRepository = caseRepository;
    this.caseParametersRepository = caseParametersRepository;
    this.gson = gson;
  }

  public DataContextOfAction checkedGetDataContext(
      final String productIdentifier,
      final String caseIdentifier,
      final @Nullable List<AccountAssignment> oneTimeAccountAssignments) {

    final ProductEntity product =
        productRepository.findByIdentifier(productIdentifier)
            .orElseThrow(() -> ServiceException
                .notFound("Product not found ''{0}''.", productIdentifier));
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

  public DataContextOfAction checkedGetDataContext(
      final String productIdentifier,
      final Case caseInstance,
      final @Nullable List<AccountAssignment> oneTimeAccountAssignments) {

    final ProductEntity product =
        productRepository.findByIdentifier(productIdentifier)
            .orElseThrow(() -> ServiceException.notFound("Product not found ''{0}''.", productIdentifier));
    final CaseEntity customerCase = CaseMapper.map(caseInstance);

    final CaseParametersEntity caseParameters = CaseParametersMapper.map(
        0L,
        gson.fromJson(caseInstance.getParameters(), CaseParameters.class));

    return new DataContextOfAction(
        product,
        customerCase,
        caseParameters,
        oneTimeAccountAssignments);
  }
}