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
package org.apache.fineract.cn.portfolio;

import com.google.gson.Gson;
import org.apache.fineract.cn.portfolio.api.v1.domain.Case;
import org.apache.fineract.cn.portfolio.api.v1.domain.Product;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.restdocs.JUnitRestDocumentation;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.apache.fineract.cn.lang.config.TenantHeaderFilter.TENANT_HEADER;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.documentationConfiguration;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.get;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.post;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.preprocessRequest;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.preprocessResponse;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.prettyPrint;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.requestFields;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class PlannedPaymentsApiDocumentation extends AbstractPortfolioTest {

  @Rule
  public final JUnitRestDocumentation restDocumentation = new JUnitRestDocumentation("build/doc/generated-snippets/test-plannedpayments");

  @Autowired
  private WebApplicationContext context;

  private MockMvc mockMvc;

  @Before
  public void setUp() {

    this.mockMvc = MockMvcBuilders.webAppContextSetup(this.context)
            .apply(documentationConfiguration(this.restDocumentation))
            .alwaysDo(document("{method-name}", preprocessRequest(prettyPrint()), preprocessResponse(prettyPrint())))
            .build();
  }

  @Test
  public void documentGetPaymentScheduledForCase() throws Exception {
    final Product product = createAndEnableProduct();
    final Case caseInstance = createCase(product.getIdentifier());

    this.mockMvc.perform(get("/individuallending/products/" + product.getIdentifier() + "/cases/" + caseInstance.getIdentifier() + "/plannedpayments")
            .accept(MediaType.APPLICATION_JSON_VALUE)
            .contentType(MediaType.APPLICATION_JSON_VALUE)
            .header(TENANT_HEADER, tenantDataStoreContext.getTenantName()))
            .andExpect(status().isOk())
            .andDo(document(
                    "document-get-payment-scheduled-for-case", preprocessRequest(prettyPrint()),
                    responseFields(
                            fieldWithPath("chargeNames").description("Charge names"),
                            fieldWithPath("elements").description("Payments"),
                            fieldWithPath("totalPages").description("Total number of pages "),
                            fieldWithPath("totalElements").description("Total elements found"))));

  }

  @Test
  public void documentGetPaymentScheduledForParameters() throws Exception {
    final Product product = createAndEnableProduct();
    final Case caseInstance = createCase(product.getIdentifier());

    Gson gson = new Gson();
    this.mockMvc.perform(post("/individuallending/products/" + product.getIdentifier() + "/plannedpayments")
            .contentType(MediaType.APPLICATION_JSON_VALUE)
            .content(gson.toJson(caseInstance)))
            .andExpect(status().isOk())
            .andDo(document("document-get-payment-scheduled-for-parameters", preprocessRequest(prettyPrint()), preprocessResponse(prettyPrint()),
                    requestFields(
                            fieldWithPath("identifier").description("Cases's identifier"),
                            fieldWithPath("productIdentifier").description("Products's identifier"),
                            fieldWithPath("interest").description("Cases's interest"),
                            fieldWithPath("parameters").description("cases's parameters"),
                            fieldWithPath("accountAssignments").description("Cases's account assignment"),
                            fieldWithPath("currentState").description("Cases's current state"))));

  }

}
