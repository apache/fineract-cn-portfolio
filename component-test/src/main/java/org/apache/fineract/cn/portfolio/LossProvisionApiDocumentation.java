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
import org.apache.fineract.cn.individuallending.api.v1.domain.product.LossProvisionConfiguration;
import org.apache.fineract.cn.individuallending.api.v1.domain.product.LossProvisionStep;
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

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

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


public class LossProvisionApiDocumentation extends AbstractPortfolioTest {

  @Rule
  public final JUnitRestDocumentation restDocumentation = new JUnitRestDocumentation("build/doc/generated-snippets/test-lossprovision");

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
  public void documentChangeLossProvisionConfiguration() throws Exception {
    final Product product = createAdjustedProduct(x -> {
    });


    final List<LossProvisionStep> lossProvisionSteps = new ArrayList<>();
    lossProvisionSteps.add(new LossProvisionStep(1, BigDecimal.valueOf(5_00, 2)));
    final LossProvisionConfiguration lossProvisionConfiguration = new LossProvisionConfiguration(lossProvisionSteps);

    individualLending.changeLossProvisionConfiguration(product.getIdentifier(), lossProvisionConfiguration);


    Gson gson = new Gson();
    this.mockMvc.perform(put("/individuallending/products/" + product.getIdentifier() + "/lossprovisionconfiguration")
            .accept(MediaType.APPLICATION_JSON_VALUE)
            .contentType(MediaType.APPLICATION_JSON_VALUE)
            .header(TENANT_HEADER, tenantDataStoreContext.getTenantName())
            .content(gson.toJson(lossProvisionConfiguration)))
            .andExpect(status().isAccepted())
            .andDo(document(
                    "document-change-loss-provision-configuration", preprocessRequest(prettyPrint()),
                    requestFields(
                            fieldWithPath("lossProvisionSteps").type("List<LossProvisionSteps>").description("The loss provision configurations +\n" +
                                    " +\n" +
                                    "_LossProvisionSteps_ { +\n" +
                                    "  *enum* _Type_ { +\n" +
                                    "     daysLate, +\n" +
                                    "     percentProvision, +\n" +
                                    "  } +")
                    )
            ));
  }

  @Test
  public void documentGetLossProvisionConfiguration() throws Exception {

    final Product product = createAdjustedProduct(x -> {
    });

    final List<LossProvisionStep> lossProvisionSteps1 = new ArrayList<>();
    lossProvisionSteps1.add(new LossProvisionStep(0, BigDecimal.valueOf(1_00, 2)));
    lossProvisionSteps1.add(new LossProvisionStep(1, BigDecimal.valueOf(9_00, 2)));
    lossProvisionSteps1.add(new LossProvisionStep(30, BigDecimal.valueOf(35_00, 2)));
    lossProvisionSteps1.add(new LossProvisionStep(60, BigDecimal.valueOf(55_00, 2)));
    final LossProvisionConfiguration lossProvisionConfiguration = new LossProvisionConfiguration(lossProvisionSteps1);

    individualLending.changeLossProvisionConfiguration(product.getIdentifier(), lossProvisionConfiguration);

    try {
      this.mockMvc.perform(get("/individuallending/products/" + product.getIdentifier() + "/lossprovisionconfiguration")
              .accept(MediaType.APPLICATION_JSON_VALUE)
              .contentType(MediaType.APPLICATION_JSON_VALUE)
              .header(TENANT_HEADER, tenantDataStoreContext.getTenantName()))
              .andExpect(status().isOk())
              .andDo(document(
                      "document-get-loss-provision-configuration", preprocessRequest(prettyPrint()),
                      responseFields(
                              fieldWithPath("lossProvisionSteps").type("List<LossProvisionSteps>").description("The loss provision configurations +\n" +
                                      " +\n" +
                                      "_LossProvisionSteps_ { +\n" +
                                      "  *enum* _Type_ { +\n" +
                                      "     daysLate, +\n" +
                                      "     percentProvision, +\n" +
                                      "  } +")
                      )
              ));
    } catch (Exception e) {
      e.printStackTrace();
    }

  }


}