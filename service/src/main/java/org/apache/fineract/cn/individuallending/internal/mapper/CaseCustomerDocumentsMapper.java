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
package org.apache.fineract.cn.individuallending.internal.mapper;

import org.apache.fineract.cn.individuallending.api.v1.domain.caseinstance.CaseCustomerDocuments;
import org.apache.fineract.cn.individuallending.internal.repository.CaseCustomerDocumentEntity;
import org.apache.fineract.cn.individuallending.internal.repository.CaseParametersEntity;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @author Myrle Krantz
 */
public final class CaseCustomerDocumentsMapper {
  private CaseCustomerDocumentsMapper() {}

  public static CaseCustomerDocuments.Document map(
      final CaseCustomerDocumentEntity caseCustomerDocumentEntity) {
    final CaseCustomerDocuments.Document ret = new CaseCustomerDocuments.Document();
    ret.setCustomerId(caseCustomerDocumentEntity.getCustomerIdentifier());
    ret.setDocumentId(caseCustomerDocumentEntity.getDocumentIdentifier());
    return ret;
  }

  private static CaseCustomerDocumentEntity map(
      final CaseCustomerDocuments.Document caseCustomerDocument,
      final Long caseParametersId,
      final Integer order) {
    final CaseCustomerDocumentEntity ret = new CaseCustomerDocumentEntity();
    ret.setCustomerIdentifier(caseCustomerDocument.getCustomerId());
    ret.setDocumentIdentifier(caseCustomerDocument.getDocumentId());
    ret.setCaseParametersId(caseParametersId);
    ret.setOrder(order);
    return ret;
  }


  public static List<CaseCustomerDocumentEntity> map(
      final List<CaseCustomerDocuments.Document> documents,
      final CaseParametersEntity caseparametersEntity,
      final Map<CaseCustomerDocuments.Document, CaseCustomerDocumentEntity> existingCaseCustomerDocuments) {
    final List<CaseCustomerDocumentEntity> ret = new ArrayList<>();
    for (int i = 0; i < documents.size(); i++) {
      CaseCustomerDocumentEntity toAdd = map(documents.get(i), caseparametersEntity.getId(), i);
      final CaseCustomerDocumentEntity existing = existingCaseCustomerDocuments.get(documents.get(i));
      if (existing != null) {
        existing.setOrder(toAdd.getOrder());
        toAdd = existing;
      }
      ret.add(toAdd);
    }
    return ret;
  }
}
