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
package org.apache.fineract.cn.individuallending.api.v1.client;

import org.apache.fineract.cn.individuallending.api.v1.domain.caseinstance.CaseCustomerDocuments;
import org.apache.fineract.cn.api.util.CustomFeignClientsConfiguration;
import org.springframework.cloud.netflix.feign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

/**
 * @author Myrle Krantz
 */
@SuppressWarnings("unused")
@FeignClient(path = "/portfolio/v1", url = "http://${kubernetes.portfolio.service.name}:${kubernetes.portfolio.server.port}", configuration = CustomFeignClientsConfiguration.class)
public interface CaseDocumentsManager {
  @RequestMapping(
      value = "/individuallending/products/{productidentifier}/cases/{caseidentifier}/documents",
      method = RequestMethod.GET,
      produces = MediaType.ALL_VALUE,
      consumes = MediaType.APPLICATION_JSON_VALUE)
  CaseCustomerDocuments getCaseDocuments(
      @PathVariable("productidentifier") final String productIdentifier,
      @PathVariable("caseidentifier") final String caseIdentifier);


  @RequestMapping(
      value = "/individuallending/products/{productidentifier}/cases/{caseidentifier}/documents",
      method = RequestMethod.PUT,
      produces = MediaType.APPLICATION_JSON_VALUE,
      consumes = MediaType.APPLICATION_JSON_VALUE
  )
  void changeCaseDocuments(
      @PathVariable("productidentifier") final String productIdentifier,
      @PathVariable("caseidentifier") final String caseIdentifier,
      final CaseCustomerDocuments caseInstance);
}