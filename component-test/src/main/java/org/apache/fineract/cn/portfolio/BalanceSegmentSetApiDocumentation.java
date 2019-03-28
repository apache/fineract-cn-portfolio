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
import org.apache.fineract.cn.portfolio.api.v1.domain.BalanceSegmentSet;
import org.apache.fineract.cn.portfolio.api.v1.domain.Product;
import org.apache.fineract.cn.portfolio.api.v1.events.BalanceSegmentSetEvent;
import org.apache.fineract.cn.portfolio.api.v1.events.EventConstants;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.restdocs.JUnitRestDocumentation;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.math.BigDecimal;
import java.util.Arrays;

import static org.apache.fineract.cn.lang.config.TenantHeaderFilter.TENANT_HEADER;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.documentationConfiguration;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.*;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.*;
import static org.springframework.restdocs.payload.PayloadDocumentation.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class BalanceSegmentSetApiDocumentation extends AbstractPortfolioTest {

  @Rule
  public final JUnitRestDocumentation restDocumentation = new JUnitRestDocumentation("build/doc/generated-snippets/test-balancesegmentsets");

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
  public void documentCreateBalanceSegmentSet() throws Exception {

    final Product product = createProduct();

    final BalanceSegmentSet balanceSegmentSet = new BalanceSegmentSet();
    balanceSegmentSet.setIdentifier(testEnvironment.generateUniqueIdentifier("bss"));
    balanceSegmentSet.setSegments(Arrays.asList(
            BigDecimal.ZERO.setScale(5, BigDecimal.ROUND_HALF_EVEN),
            BigDecimal.TEN.setScale(5, BigDecimal.ROUND_HALF_EVEN),
            BigDecimal.valueOf(10_000_0000, 5)));
    balanceSegmentSet.setSegmentIdentifiers(Arrays.asList("abc", "def", "ghi"));

    Gson gson = new Gson();
    this.mockMvc.perform(post("/products/" + product.getIdentifier() + "/balancesegmentsets/")
            .contentType(MediaType.APPLICATION_JSON_VALUE)
            .content(gson.toJson(balanceSegmentSet)))
            .andExpect(status().isAccepted())
            .andDo(document("document-create-balance-segment-set", preprocessRequest(prettyPrint()), preprocessResponse(prettyPrint()),
                    requestFields(
                            fieldWithPath("identifier").description("Balance segment set's identifier"),
                            fieldWithPath("segments").description("Balance segment set's given name"),
                            fieldWithPath("segmentIdentifiers").description("Balance segment sets's segment identfiers"))));

  }

  @Test
  public void documentGetAllBalanceSegmentSets() throws Exception {
    final Product product = createProduct();

    final BalanceSegmentSet balanceSegmentSet = new BalanceSegmentSet();
    balanceSegmentSet.setIdentifier(testEnvironment.generateUniqueIdentifier("bss"));
    balanceSegmentSet.setSegments(Arrays.asList(
            BigDecimal.ZERO.setScale(3, BigDecimal.ROUND_HALF_EVEN),
            BigDecimal.TEN.setScale(3, BigDecimal.ROUND_HALF_EVEN),
            BigDecimal.valueOf(10_000_0000, 3)));
    balanceSegmentSet.setSegmentIdentifiers(Arrays.asList("how", "are", "you"));

    portfolioManager.createBalanceSegmentSet(product.getIdentifier(), balanceSegmentSet);

    this.mockMvc.perform(get("/products/" + product.getIdentifier() + "/balancesegmentsets/")
            .accept(MediaType.APPLICATION_JSON_VALUE)
            .contentType(MediaType.APPLICATION_JSON_VALUE)
            .header(TENANT_HEADER, tenantDataStoreContext.getTenantName()))
            .andExpect(status().isOk())
            .andDo(document(
                    "document-get-all-balance-segment-sets", preprocessRequest(prettyPrint()),
                    responseFields(
                            fieldWithPath("[]").description("An array of balance segment sets"),
                            fieldWithPath("[].identifier").description("Balance segment set's identifier "),
                            fieldWithPath("[].segments").description("Balance segment set's segments "),
                            fieldWithPath("[].segmentIdentifiers").description("Balance segment set's segment identifier"))));

  }

  @Test
  public void documentGetBalanceSegmentSet() throws Exception {
    final Product product = createProduct();

    final BalanceSegmentSet balanceSegmentSet = new BalanceSegmentSet();
    balanceSegmentSet.setIdentifier(testEnvironment.generateUniqueIdentifier("bss"));
    balanceSegmentSet.setSegments(Arrays.asList(
            BigDecimal.ZERO.setScale(3, BigDecimal.ROUND_HALF_EVEN),
            BigDecimal.TEN.setScale(3, BigDecimal.ROUND_HALF_EVEN),
            BigDecimal.valueOf(10_000_0000, 3)));
    balanceSegmentSet.setSegmentIdentifiers(Arrays.asList("how", "are", "you"));

    portfolioManager.createBalanceSegmentSet(product.getIdentifier(), balanceSegmentSet);
    this.eventRecorder.wait(EventConstants.POST_BALANCE_SEGMENT_SET, new BalanceSegmentSetEvent(product.getIdentifier(), balanceSegmentSet.getIdentifier()));


    this.mockMvc.perform(get("/products/" + product.getIdentifier() + "/balancesegmentsets/" + balanceSegmentSet.getIdentifier())
            .accept(MediaType.APPLICATION_JSON_VALUE)
            .contentType(MediaType.APPLICATION_JSON_VALUE)
            .header(TENANT_HEADER, tenantDataStoreContext.getTenantName()))
            .andExpect(status().isOk())
            .andDo(document(
                    "document-get-balance-segment-set", preprocessRequest(prettyPrint()),
                    responseFields(
                            fieldWithPath("identifier").description("Balance segment set's identifier"),
                            fieldWithPath("segments").description("Balance segment set's given name"),
                            fieldWithPath("segmentIdentifiers").description("Balance segment sets's segment identfiers"))));

  }

  @Test
  public void documentChangeBalanceSegmentSet() throws Exception {
    final Product product = createProduct();

    final BalanceSegmentSet balanceSegmentSet = new BalanceSegmentSet();
    balanceSegmentSet.setIdentifier(testEnvironment.generateUniqueIdentifier("bss"));
    balanceSegmentSet.setSegments(Arrays.asList(
            BigDecimal.ZERO.setScale(3, BigDecimal.ROUND_HALF_EVEN),
            BigDecimal.TEN.setScale(3, BigDecimal.ROUND_HALF_EVEN),
            BigDecimal.valueOf(10_000_0000, 3)));
    balanceSegmentSet.setSegmentIdentifiers(Arrays.asList("how", "are", "you"));

    portfolioManager.createBalanceSegmentSet(product.getIdentifier(), balanceSegmentSet);
    this.eventRecorder.wait(EventConstants.POST_BALANCE_SEGMENT_SET, new BalanceSegmentSetEvent(product.getIdentifier(), balanceSegmentSet.getIdentifier()));


    balanceSegmentSet.setSegments(Arrays.asList(
            BigDecimal.ZERO.setScale(6, BigDecimal.ROUND_HALF_EVEN),
            BigDecimal.valueOf(100_0000, 6),
            BigDecimal.valueOf(10_000_0000, 6)));
    balanceSegmentSet.setSegmentIdentifiers(Arrays.asList("abc", "def", "ghi"));

    Gson gson = new Gson();
    this.mockMvc.perform(put("/products/" + product.getIdentifier() + "/balancesegmentsets/" + balanceSegmentSet.getIdentifier())
            .accept(MediaType.APPLICATION_JSON_VALUE)
            .contentType(MediaType.APPLICATION_JSON_VALUE)
            .header(TENANT_HEADER, tenantDataStoreContext.getTenantName())
            .content(gson.toJson(balanceSegmentSet)))
            .andExpect(status().isAccepted())
            .andDo(document(
                    "document-change-balance-segment-set", preprocessRequest(prettyPrint()), preprocessResponse(prettyPrint()),
                    requestFields(
                            fieldWithPath("identifier").description("Balance segment set's identifier"),
                            fieldWithPath("segments").description("Balance segment set's given name"),
                            fieldWithPath("segmentIdentifiers").description("Balance segment sets's segment identfiers"))));

  }

  @Test
  public void documentDeleteBalanceSegmentSet() throws Exception {
    final Product product = createProduct();

    final BalanceSegmentSet balanceSegmentSet = new BalanceSegmentSet();
    balanceSegmentSet.setIdentifier(testEnvironment.generateUniqueIdentifier("bss"));
    balanceSegmentSet.setSegments(Arrays.asList(
            BigDecimal.ZERO.setScale(3, BigDecimal.ROUND_HALF_EVEN),
            BigDecimal.TEN.setScale(3, BigDecimal.ROUND_HALF_EVEN),
            BigDecimal.valueOf(10_000_0000, 3)));
    balanceSegmentSet.setSegmentIdentifiers(Arrays.asList("am", "fine", "thanks"));

    portfolioManager.createBalanceSegmentSet(product.getIdentifier(), balanceSegmentSet);
    this.mockMvc.perform(delete("/products/" + product.getIdentifier() + "/balancesegmentsets/" + balanceSegmentSet.getIdentifier())
            .contentType(MediaType.APPLICATION_JSON_VALUE)
            .accept(MediaType.ALL_VALUE))
            .andExpect(status().isAccepted())
            .andDo(document("document-delete-balance-segment-set", preprocessResponse(prettyPrint())));

  }


}
