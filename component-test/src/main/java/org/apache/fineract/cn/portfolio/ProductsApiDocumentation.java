package org.apache.fineract.cn.portfolio;

import com.google.gson.Gson;
import org.apache.fineract.cn.portfolio.api.v1.domain.Product;
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

import static org.apache.fineract.cn.lang.config.TenantHeaderFilter.TENANT_HEADER;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.documentationConfiguration;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.*;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.preprocessRequest;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.preprocessResponse;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.prettyPrint;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.requestFields;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class ProductsApiDocumentation extends AbstractPortfolioTest {

    @Rule
    public final JUnitRestDocumentation restDocumentation = new JUnitRestDocumentation("build/doc/generated-snippets/test-product");

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
    public void documentCreateProduct() throws Exception {
        final Product product = createAdjustedProduct(x -> {});
         product.setIdentifier("agro11");

        Gson gson = new Gson();
        this.mockMvc.perform(post("/products/")
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content(gson.toJson(product)))
                .andExpect(status().isAccepted())
                .andDo(document("document-create-product", preprocessRequest(prettyPrint()), preprocessResponse(prettyPrint()),
                        requestFields(
                                fieldWithPath("identifier").description("Product's identifier"),
                                fieldWithPath("name").description("Product's given name"),
                                fieldWithPath("termRange").type("List<TermRange>").description("The term range +\n" +
                                        " +\n" +
                                        "_TermRange_ { +\n" +
                                        "  *enum* _Type_ { +\n" +
                                        "     temporalUnit, +\n" +
                                        "     maximum, +\n" +
                                        "  } +"),
                                fieldWithPath("balanceRange").description("Product's balance range"),
                                fieldWithPath("interestRange").description("Products interest Range"),
                                fieldWithPath("interestBasis").description("Products's interest basis"),
                                fieldWithPath("patternPackage").description("Product's pattern package"),
                                fieldWithPath("description").description("product description"),
                                fieldWithPath("currencyCode").description("Country currency code"),
                                fieldWithPath("minorCurrencyUnitDigits").description("Country minor currency unit"),
                                fieldWithPath("accountAssignments").description("Account Assignments"),
                                fieldWithPath("parameters").description("Product's parameters"),
                                fieldWithPath("enabled").description("Readability"))));

    }


    @Test
    public void documentGetProducts() throws Exception {
        final Product product = createAdjustedProduct(x -> {});

       this.eventRecorder.wait(EventConstants.POST_PRODUCT, product.getIdentifier());

        try {
            this.mockMvc.perform(get("/products?pageIndex=0&size=200")
                    .accept(MediaType.APPLICATION_JSON_VALUE)
                    .contentType(MediaType.APPLICATION_JSON_VALUE)
                    .header(TENANT_HEADER, tenantDataStoreContext.getTenantName()))
                    .andExpect(status().isOk())
                    .andDo(document(
                            "document-get-products", preprocessRequest(prettyPrint()),
                            responseFields(
                                    fieldWithPath("identifier").description("Product's identifier"),
                                    fieldWithPath("name").description("Product's given name"),
                                    fieldWithPath("termRange").type("List<TermRange>").description("The term range +\n" +
                                            " +\n" +
                                            "_TermRange_ { +\n" +
                                            "  *enum* _Type_ { +\n" +
                                            "     temporalUnit, +\n" +
                                            "     maximum, +\n" +
                                            "  } +"),
                                    fieldWithPath("balanceRange").description("Product's balance range"),
                                    fieldWithPath("interestRange").description("Products interest Range"),
                                    fieldWithPath("interestBasis").description("Products's interest basis"),
                                    fieldWithPath("patternPackage").description("Product's pattern package"),
                                    fieldWithPath("description").description("product description"),
                                    fieldWithPath("currencyCode").description("Country currency code"),
                                    fieldWithPath("minorCurrencyUnitDigits").description("Country minor currency unit"),
                                    fieldWithPath("accountAssignments").description("Account Assignments"),
                                    fieldWithPath("parameters").description("Product's parameters"),
                                    fieldWithPath("enabled").description("Readability"))));
    } catch (Exception e) {
        e.printStackTrace();
    }

}

    @Test
    public void documentDeleteProducts() throws Exception {

        final Product product = createAdjustedProduct(x -> {
        });


        this.mockMvc.perform(delete("/products/" + product.getIdentifier())
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .accept(MediaType.ALL_VALUE))
                .andExpect(status().isAccepted())
                .andDo(document("document-delete-products", preprocessResponse(prettyPrint())));

    }

    @Test
    public void documentGetProduct() throws Exception {

        final Product product = createAdjustedProduct(x -> {
        });

        this.mockMvc.perform(get("/products/" + product.getIdentifier())
                .accept(MediaType.APPLICATION_JSON_VALUE)
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .header(TENANT_HEADER, tenantDataStoreContext.getTenantName()))
                .andExpect(status().isOk())
                .andDo(document(
                        "document-get-product", preprocessRequest(prettyPrint()),
                        responseFields(
                                fieldWithPath("identifier").description("Charge definition's identifier"),
                                fieldWithPath("name").description("Charge definitions given name"),
                                fieldWithPath("termRange").type("List<TermRange>").description("The term range +\n" +
                                        " +\n" +
                                        "_TermRange_ { +\n" +
                                        "  *enum* _Type_ { +\n" +
                                        "     temporalUnit, +\n" +
                                        "     maximum, +\n" +
                                        "  } +"),
                                fieldWithPath("balanceRange").description("From account designator"),
                                fieldWithPath("interestRange").description("To account designator"),
                                fieldWithPath("interestBasis").description("Charge definition's amount"),
                                fieldWithPath("patternPackage").description("Charge definitions charge method"),
                                fieldWithPath("description").description("Charge definitions charge method"),
                                fieldWithPath("currencyCode").description("Charge definition's charge action"),
                                fieldWithPath("minorCurrencyUnitDigits").description("Employee's middle name"),
                                fieldWithPath("accountAssignments").description("Readability"),
                                fieldWithPath("parameters").description("Readability"),
                                fieldWithPath("enabled").description("Readability"),
                                fieldWithPath("createdOn").description("Readability"),
                                fieldWithPath("createdBy").description("Readability"),
                                fieldWithPath("lastModifiedOn").description("Readability"),
                                fieldWithPath("lastModifiedBy").description("Readability"))));

}

    @Test
    public void documentChangeProduct() throws Exception {

        final Product product = createAdjustedProduct(x -> {});
        product.setName("akawo");

        Gson gson = new Gson();
        this.mockMvc.perform(put("/products/" + product.getIdentifier())
                .accept(MediaType.APPLICATION_JSON_VALUE)
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .header(TENANT_HEADER, tenantDataStoreContext.getTenantName())
                .content(gson.toJson(product)))
                .andExpect(status().isAccepted())
                .andDo(document(
                        "document-change-product", preprocessRequest(prettyPrint()), preprocessResponse(prettyPrint()),
                        requestFields(
                                fieldWithPath("identifier").description("Charge definition's identifier"),
                                fieldWithPath("name").description("Charge definitions given name"),
                                fieldWithPath("termRange").type("List<TermRange>").description("The term range +\n" +
                                        " +\n" +
                                        "_TermRange_ { +\n" +
                                        "  *enum* _Type_ { +\n" +
                                        "     temporalUnit, +\n" +
                                        "     maximum, +\n" +
                                        "  } +"),
                                fieldWithPath("balanceRange").description("From account designator"),
                                fieldWithPath("interestRange").description("To account designator"),
                                fieldWithPath("interestBasis").description("Charge definition's amount"),
                                fieldWithPath("patternPackage").description("Charge definitions charge method"),
                                fieldWithPath("description").description("Charge definitions charge method"),
                                fieldWithPath("currencyCode").description("Charge definition's charge action"),
                                fieldWithPath("minorCurrencyUnitDigits").description("Employee's middle name"),
                                fieldWithPath("accountAssignments").description("Readability"),
                                fieldWithPath("parameters").description("Readability"),
                                fieldWithPath("enabled").description("Readability"))));

    }

    @Test
    public void documentGetIncompleteAccountAssignments () throws Exception {

        final Product product = createAdjustedProduct(x -> {});

        this.mockMvc.perform(get("/products/" + product.getIdentifier()+"/incompleteaccountassignments")
                .accept(MediaType.APPLICATION_JSON_VALUE)
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .header(TENANT_HEADER, tenantDataStoreContext.getTenantName()))
                .andExpect(status().isOk())
                .andDo(document(
                        "document-get-incomplete-account-assignments", preprocessRequest(prettyPrint()),
                        responseFields(
                               )));

    }





}
