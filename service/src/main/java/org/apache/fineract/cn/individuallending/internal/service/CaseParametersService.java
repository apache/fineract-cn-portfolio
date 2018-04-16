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
import org.apache.fineract.cn.portfolio.api.v1.domain.Case;
import org.apache.fineract.cn.portfolio.api.v1.domain.CasePage;
import org.apache.fineract.cn.portfolio.service.ServiceConstants;
import org.apache.fineract.cn.portfolio.service.internal.mapper.CaseMapper;
import org.apache.fineract.cn.portfolio.service.internal.repository.CaseEntity;
import org.apache.fineract.cn.portfolio.service.internal.repository.CaseRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * @author Myrle Krantz
 */
@Service
public class CaseParametersService {
  final private CaseRepository caseRepository;
  final private CaseParametersRepository caseParametersRepository;
  private final Gson gson;

  @Autowired
  public CaseParametersService(
          final CaseRepository caseRepository,
          final CaseParametersRepository caseParametersRepository,
          @Qualifier(ServiceConstants.GSON_NAME) final Gson gson) {
    this.caseRepository = caseRepository;
    this.caseParametersRepository = caseParametersRepository;
    this.gson = gson;
  }

  public Optional<CaseParameters> findByIdentifier(final String productIdentifier, final String caseIdentifier) {
    return caseRepository
            .findByProductIdentifierAndIdentifier(productIdentifier, caseIdentifier)
            .map(CaseEntity::getId)
            .flatMap(caseParametersRepository::findByCaseId)
            .map(x -> CaseParametersMapper.mapEntity(x, 4));
  }

  public CasePage findByCustomerIdentifier(
          final String customerIdentifier,
          final int pageIndex,
          final int size)
  {
    final Pageable pageRequest = new PageRequest(pageIndex, size, Sort.Direction.DESC, "id");

    final Page<CaseParametersEntity> ret = caseParametersRepository.findByCustomerIdentifier(customerIdentifier, pageRequest);

    return new CasePage(mapList(ret.getContent()), ret.getTotalPages(), ret.getTotalElements());
  }

  private List<Case> mapList(final List<CaseParametersEntity> in) {
    return in.stream()
            .map(x -> CaseMapper.map(caseRepository.findOne(x.getCaseId()), gson.toJson(CaseParametersMapper.mapEntity(x, 4))))
            .collect(Collectors.toList());
  }
}
