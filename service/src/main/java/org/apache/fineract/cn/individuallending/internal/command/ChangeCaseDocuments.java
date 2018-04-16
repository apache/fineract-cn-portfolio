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
package org.apache.fineract.cn.individuallending.internal.command;

import org.apache.fineract.cn.individuallending.api.v1.domain.caseinstance.CaseCustomerDocuments;

/**
 * @author Myrle Krantz
 */
public class ChangeCaseDocuments {
  private final String productIdentifier;
  private final String caseIdentifier;
  private final CaseCustomerDocuments instance;

  public ChangeCaseDocuments(String productIdentifier, String caseIdentifier, CaseCustomerDocuments instance) {
    this.productIdentifier = productIdentifier;
    this.caseIdentifier = caseIdentifier;
    this.instance = instance;
  }

  public String getProductIdentifier() {
    return productIdentifier;
  }

  public String getCaseIdentifier() {
    return caseIdentifier;
  }

  public CaseCustomerDocuments getInstance() {
    return instance;
  }

  @Override
  public String toString() {
    return "ChangeCaseDocuments{" +
        "productIdentifier='" + productIdentifier + '\'' +
        ", caseIdentifier='" + caseIdentifier + '\'' +
        '}';
  }
}
