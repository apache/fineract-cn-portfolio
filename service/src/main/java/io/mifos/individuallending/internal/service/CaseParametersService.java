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

import com.google.gson.Gson;
import io.mifos.individuallending.api.v1.domain.caseinstance.CaseParameters;
import io.mifos.individuallending.internal.mapper.CaseParametersMapper;
import io.mifos.individuallending.internal.repository.CaseParametersEntity;
import io.mifos.individuallending.internal.repository.CaseParametersRepository;
import io.mifos.portfolio.api.v1.domain.Case;
import io.mifos.portfolio.api.v1.domain.CasePage;
import io.mifos.portfolio.service.ServiceConstants;
import io.mifos.portfolio.service.internal.mapper.CaseMapper;
import io.mifos.portfolio.service.internal.repository.CaseEntity;
import io.mifos.portfolio.service.internal.repository.CaseRepository;
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
            .map(CaseParametersMapper::mapEntity);
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
            .map(x -> CaseMapper.map(caseRepository.findOne(x.getCaseId()), gson.toJson(CaseParametersMapper.mapEntity(x))))
            .collect(Collectors.toList());
  }
}
