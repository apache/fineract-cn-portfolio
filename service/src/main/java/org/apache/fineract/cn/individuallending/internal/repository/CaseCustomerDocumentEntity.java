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
package org.apache.fineract.cn.individuallending.internal.repository;

import javax.persistence.*;
import java.util.Objects;

/**
 * @author Myrle Krantz
 */
@SuppressWarnings("unused")
@Entity
@Table(name = "bastet_il_c_docs")
public class CaseCustomerDocumentEntity {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "id")
  private Long id;

  @Column(name = "case_id")
  private Long caseParametersId;

  @Column(name = "customer_identifier")
  private String customerIdentifier;

  @Column(name = "document_identifier")
  private String documentIdentifier;

  @Column(name = "list_order")
  private Integer order;

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public Long getCaseParametersId() {
    return caseParametersId;
  }

  public void setCaseParametersId(Long caseParametersId) {
    this.caseParametersId = caseParametersId;
  }

  public String getCustomerIdentifier() {
    return customerIdentifier;
  }

  public void setCustomerIdentifier(String customerIdentifier) {
    this.customerIdentifier = customerIdentifier;
  }

  public String getDocumentIdentifier() {
    return documentIdentifier;
  }

  public void setDocumentIdentifier(String documentIdentifier) {
    this.documentIdentifier = documentIdentifier;
  }

  public Integer getOrder() {
    return order;
  }

  public void setOrder(Integer order) {
    this.order = order;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    CaseCustomerDocumentEntity that = (CaseCustomerDocumentEntity) o;
    return Objects.equals(caseParametersId, that.caseParametersId) &&
        Objects.equals(customerIdentifier, that.customerIdentifier) &&
        Objects.equals(documentIdentifier, that.documentIdentifier) &&
        Objects.equals(order, that.order);
  }

  @Override
  public int hashCode() {
    return Objects.hash(caseParametersId, customerIdentifier, documentIdentifier, order);
  }
}