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
import org.apache.fineract.cn.individuallending.api.v1.domain.caseinstance.CaseCustomerDocuments;
import org.apache.fineract.cn.portfolio.api.v1.domain.*;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.restdocs.JUnitRestDocumentation;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.util.Arrays;

import static org.apache.fineract.cn.lang.config.TenantHeaderFilter.TENANT_HEADER;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.documentationConfiguration;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.get;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.put;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.preprocessRequest;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.preprocessResponse;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.prettyPrint;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.requestFields;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class CaseDocumentsApiDocumentation extends AbstractPortfolioTest {

  @Rule
  public final JUnitRestDocumentation restDocumentation = new JUnitRestDocumentation("build/doc/generated-snippets/test-casedocuments");

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
  public void documentGetCaseDocuments() throws Exception {

    final Product product = createAndEnableProduct();

    final Case customerCase = createCase(product.getIdentifier());


    final CaseCustomerDocuments caseDocuments = caseDocumentsManager.getCaseDocuments(
            product.getIdentifier(), customerCase.getIdentifier());

    final CaseCustomerDocuments.Document studentLoanDocument
            = new CaseCustomerDocuments.Document(Fixture.CUSTOMER_IDENTIFIER, "student_loan_documents");
    final CaseCustomerDocuments.Document houseTitle
            = new CaseCustomerDocuments.Document(Fixture.CUSTOMER_IDENTIFIER, "house_title");
    final CaseCustomerDocuments.Document workContract
            = new CaseCustomerDocuments.Document(Fixture.CUSTOMER_IDENTIFIER, "work_contract");

    caseDocuments.setDocuments(Arrays.asList(studentLoanDocument, houseTitle, workContract));

    this.mockMvc.perform(get("/individuallending/products/" + product.getIdentifier() + "/cases/" + customerCase.getIdentifier() + "/documents")
            .contentType(MediaType.APPLICATION_JSON_VALUE)
            .accept(MediaType.ALL_VALUE))
            .andExpect(status().isOk())
            .andDo(document("document-get-case-documents", preprocessRequest(prettyPrint()), preprocessResponse(prettyPrint()),
                    responseFields(
                            fieldWithPath("documents").type("List<Document>").description("The case document +\n" +
                                    " +\n" +
                                    "_Document_ { +\n" +
                                    "  *enum* _Type_ { +\n" +
                                    "     customerId, +\n" +
                                    "     documentId, +\n" +
                                    "  } +"))));

  }


  @Test
  public void documentChangeCaseDocuments() throws Exception {

    final Product product = createAndEnableProduct();

    final Case customerCase = createCase(product.getIdentifier());

    final CaseCustomerDocuments caseDocuments = caseDocumentsManager.getCaseDocuments(
            product.getIdentifier(), customerCase.getIdentifier());

    final CaseCustomerDocuments.Document houseLoanDocument
            = new CaseCustomerDocuments.Document(Fixture.CUSTOMER_IDENTIFIER, "house_loan_documents");
    final CaseCustomerDocuments.Document houseTitle
            = new CaseCustomerDocuments.Document(Fixture.CUSTOMER_IDENTIFIER, "house_title");
    final CaseCustomerDocuments.Document workContract
            = new CaseCustomerDocuments.Document(Fixture.CUSTOMER_IDENTIFIER, "work_contract");

    caseDocuments.setDocuments(Arrays.asList(houseLoanDocument, houseTitle, workContract));

    caseDocumentsManager.changeCaseDocuments(product.getIdentifier(), customerCase.getIdentifier(), caseDocuments);

    Gson gson = new Gson();
    this.mockMvc.perform(put("/individuallending/products/" + product.getIdentifier() + "/cases/" + customerCase.getIdentifier() + "/documents")
            .accept(MediaType.APPLICATION_JSON_VALUE)
            .contentType(MediaType.APPLICATION_JSON_VALUE)
            .header(TENANT_HEADER, tenantDataStoreContext.getTenantName())
            .content(gson.toJson(caseDocuments)))
            .andExpect(status().isAccepted())
            .andDo(document(
                    "document-change-case-documents", preprocessRequest(prettyPrint()), preprocessResponse(prettyPrint()),
                    requestFields(
                            fieldWithPath("documents").type("List<Document>").description("The case document +\n" +
                                    " +\n" +
                                    "_Document_ { +\n" +
                                    "  *enum* _Type_ { +\n" +
                                    "     customerId, +\n" +
                                    "     documentId, +\n" +
                                    "  } +"))));
  }

}