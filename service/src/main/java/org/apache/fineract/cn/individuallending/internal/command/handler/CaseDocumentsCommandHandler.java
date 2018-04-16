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
package org.apache.fineract.cn.individuallending.internal.command.handler;

import org.apache.fineract.cn.individuallending.api.v1.domain.caseinstance.CaseCustomerDocuments;
import org.apache.fineract.cn.individuallending.api.v1.events.IndividualLoanEventConstants;
import org.apache.fineract.cn.individuallending.internal.command.ChangeCaseDocuments;
import org.apache.fineract.cn.individuallending.internal.mapper.CaseCustomerDocumentsMapper;
import org.apache.fineract.cn.individuallending.internal.repository.CaseCustomerDocumentEntity;
import org.apache.fineract.cn.individuallending.internal.repository.CaseCustomerDocumentsRepository;
import org.apache.fineract.cn.individuallending.internal.repository.CaseParametersEntity;
import org.apache.fineract.cn.individuallending.internal.repository.CaseParametersRepository;
import org.apache.fineract.cn.portfolio.api.v1.events.CaseEvent;
import org.apache.fineract.cn.portfolio.service.internal.repository.CaseRepository;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.apache.fineract.cn.command.annotation.Aggregate;
import org.apache.fineract.cn.command.annotation.CommandHandler;
import org.apache.fineract.cn.command.annotation.CommandLogLevel;
import org.apache.fineract.cn.command.annotation.EventEmitter;
import org.apache.fineract.cn.lang.ServiceException;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author Myrle Krantz
 */
@Aggregate
public class CaseDocumentsCommandHandler {
  final private CaseRepository caseRepository;
  final private CaseParametersRepository caseParametersRepository;
  final private CaseCustomerDocumentsRepository caseCustomerDocumentsRepository;

  public CaseDocumentsCommandHandler(
      final CaseRepository caseRepository,
      final CaseParametersRepository caseParametersRepository,
      final CaseCustomerDocumentsRepository caseCustomerDocumentsRepository) {
    this.caseRepository = caseRepository;
    this.caseParametersRepository = caseParametersRepository;
    this.caseCustomerDocumentsRepository = caseCustomerDocumentsRepository;
  }

  @Transactional
  @CommandHandler(logStart = CommandLogLevel.INFO, logFinish = CommandLogLevel.INFO)
  @EventEmitter(selectorName = IndividualLoanEventConstants.SELECTOR_NAME, selectorValue = IndividualLoanEventConstants.PUT_DOCUMENT)
  public CaseEvent process(final ChangeCaseDocuments command) {
    final CaseParametersEntity caseparametersEntity =
        caseRepository.findByProductIdentifierAndIdentifier(command.getProductIdentifier(), command.getCaseIdentifier())
            .flatMap(x -> caseParametersRepository.findByCaseId(x.getId()))
            .orElseThrow(() -> ServiceException
                .notFound("Case ''{0}.{1}'' not found", command.getProductIdentifier(), command.getCaseIdentifier()));

    final Map<CaseCustomerDocuments.Document, CaseCustomerDocumentEntity> existingCaseCustomerDocuments
        = caseCustomerDocumentsRepository.findByCaseParametersId(caseparametersEntity.getId())
        .collect(Collectors.toMap(CaseCustomerDocumentsMapper::map, x -> x));

    final List<CaseCustomerDocumentEntity> newCaseCustomerDocuments = CaseCustomerDocumentsMapper.map(
        command.getInstance().getDocuments(),
        caseparametersEntity,
        existingCaseCustomerDocuments);

    final Set<CaseCustomerDocumentEntity> toDelete = caseCustomerDocumentsRepository.findByCaseParametersId(caseparametersEntity.getId())
        .filter(x -> !command.getInstance().getDocuments().contains(CaseCustomerDocumentsMapper.map(x)))
        .collect(Collectors.toSet());

    caseCustomerDocumentsRepository.delete(toDelete);
    caseCustomerDocumentsRepository.save(newCaseCustomerDocuments);

    return new CaseEvent(command.getProductIdentifier(), command.getCaseIdentifier());
  }
}
