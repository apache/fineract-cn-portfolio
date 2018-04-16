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
package org.apache.fineract.cn.individuallending.api.v1.domain.caseinstance;

import java.util.List;
import java.util.Objects;

/**
 * @author Myrle Krantz
 */
public class CaseCustomerDocuments {
  private List<Document> documents;

  public static class Document {
    private String customerId;
    private String documentId;

    public Document() {
    }

    public Document(String customerId, String documentId) {
      this.customerId = customerId;
      this.documentId = documentId;
    }

    public String getCustomerId() {
      return customerId;
    }

    public void setCustomerId(String customerId) {
      this.customerId = customerId;
    }

    public String getDocumentId() {
      return documentId;
    }

    public void setDocumentId(String documentId) {
      this.documentId = documentId;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;
      Document document = (Document) o;
      return Objects.equals(customerId, document.customerId) &&
          Objects.equals(documentId, document.documentId);
    }

    @Override
    public int hashCode() {
      return Objects.hash(customerId, documentId);
    }

    @Override
    public String toString() {
      return "Document{" +
          "customerId='" + customerId + '\'' +
          ", documentId='" + documentId + '\'' +
          '}';
    }
  }

  @SuppressWarnings("unused")
  public CaseCustomerDocuments() {
  }

  public CaseCustomerDocuments(List<Document> documents) {
    this.documents = documents;
  }

  public List<Document> getDocuments() {
    return documents;
  }

  public void setDocuments(List<Document> documents) {
    this.documents = documents;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    CaseCustomerDocuments that = (CaseCustomerDocuments) o;
    return Objects.equals(documents, that.documents);
  }

  @Override
  public int hashCode() {
    return Objects.hash(documents);
  }

  @Override
  public String toString() {
    return "CaseCustomerDocuments{" +
        "documents=" + documents +
        '}';
  }
}
