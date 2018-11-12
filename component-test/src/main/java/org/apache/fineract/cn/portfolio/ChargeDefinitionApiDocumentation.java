package org.apache.fineract.cn.portfolio;

import com.google.gson.Gson;
import org.apache.fineract.cn.individuallending.api.v1.domain.product.AccountDesignators;
import org.apache.fineract.cn.individuallending.api.v1.domain.workflow.Action;
import org.apache.fineract.cn.portfolio.api.v1.domain.ChargeDefinition;
import org.apache.fineract.cn.portfolio.api.v1.domain.Product;
import org.apache.fineract.cn.portfolio.api.v1.events.ChargeDefinitionEvent;
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

public class ChargeDefinitionApiDocumentation extends AbstractPortfolioTest {

    @Rule
    public final JUnitRestDocumentation restDocumentation = new JUnitRestDocumentation("build/doc/generated-snippets/test-chargedefinitions");

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
    public void documentGetAllChargeDefinitions ( ) throws Exception {
        final Product product = createProduct();

        try {
            this.mockMvc.perform(get("/products/" +product.getIdentifier()+"/charges/")
                    .accept(MediaType.APPLICATION_JSON_VALUE)
                    .contentType(MediaType.APPLICATION_JSON_VALUE)
                    .header(TENANT_HEADER, tenantDataStoreContext.getTenantName()))
                    .andExpect(status().isOk())
                    .andDo(document(
                            "document-get-all-charge-definitions", preprocessRequest(prettyPrint()),
                            responseFields(

                                    fieldWithPath("identifier").description("Charge definition's identifier"),
                                    fieldWithPath("name").description("Charge definitions given name"),
                                    fieldWithPath("description").description("Charge definitions description"),
                                    fieldWithPath("accrueAction").description("Charge definitions accrue action"),
                                    fieldWithPath("proportionalTo").description("Charge definitions proportional to"),
                                    fieldWithPath("accrualAccountDesignator").description("Charge definitions accrual acion generatort"),
                                    fieldWithPath("forCycleSizeUnit").description("Charge definitions cycle size unit"),
                                    fieldWithPath("forSegmentSet").description("Charge definitions segment set"),
                                    fieldWithPath("fromSegment").description("Charge definitions from segment"),
                                    fieldWithPath("toSegment").description("Charge definitions to segment"),
                                    fieldWithPath("chargeOnTop").description("Charge definitions charge on top"),
                                    fieldWithPath("fromAccountDesignator").description("From account designator"),
                                    fieldWithPath("toAccountDesignator").description("To account designator"),
                                    fieldWithPath("amount").description("Charge definition's amount"),
                                    fieldWithPath("chargeMethod").description("Charge definitions charge method"),
                                    fieldWithPath("chargeAction").description("Charge definition's charge action"),
                                    fieldWithPath("description").description("Employee's middle name"),
                                    fieldWithPath("readOnly").description("Readability"))));

    } catch (Exception e) {
            e.printStackTrace();
        }

    }


    @Test
    public void documentCreateChargeDefinition() throws Exception {

        final Product product = createProduct();

        final ChargeDefinition chargeDefinition = new ChargeDefinition();
        chargeDefinition.setIdentifier("charge123");
        chargeDefinition.setName("core123");
        chargeDefinition.setFromAccountDesignator("Pembe");
        chargeDefinition.setToAccountDesignator("Miriam");
        chargeDefinition.setAmount(BigDecimal.ONE.setScale(3, BigDecimal.ROUND_UNNECESSARY));
        chargeDefinition.setChargeMethod(ChargeDefinition.ChargeMethod.FIXED);
        chargeDefinition.setChargeAction(Action.OPEN.name());
        chargeDefinition.setDescription("describe charge");


        Gson gson = new Gson();
        this.mockMvc.perform(post("/products/"+product.getIdentifier()+"/charges/")
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content(gson.toJson(chargeDefinition)))
                .andExpect(status().isAccepted())
                .andDo(document("document-create-charge-definition", preprocessRequest(prettyPrint()), preprocessResponse(prettyPrint()),
                        requestFields(
                                fieldWithPath("identifier").description("Charge definition's identifier"),
                                fieldWithPath("name").description("Charge definitions given name"),
                                fieldWithPath("fromAccountDesignator").description("From account designator"),
                                fieldWithPath("toAccountDesignator").description("To account designator"),
                                fieldWithPath("amount").description("Charge definition's amount"),
                                fieldWithPath("chargeMethod").description("Charge definitions charge method"),
                                fieldWithPath("chargeAction").description("Charge definition's charge action"),
                                fieldWithPath("description").description("Employee's middle name"),
                                fieldWithPath("readOnly").description("Readability"))));

    }

    @Test
    public void documentChangeChargeDefinition() throws Exception {

        final Product product = createProduct();

        final ChargeDefinition chargeDefinition = new ChargeDefinition();
        chargeDefinition.setIdentifier("charge124");
        chargeDefinition.setName("core123");
        chargeDefinition.setFromAccountDesignator("Pembe");
        chargeDefinition.setToAccountDesignator("Miriam");
        chargeDefinition.setAmount(BigDecimal.ONE.setScale(3, BigDecimal.ROUND_UNNECESSARY));
        chargeDefinition.setChargeMethod(ChargeDefinition.ChargeMethod.FIXED);
        chargeDefinition.setChargeAction(Action.OPEN.name());
        chargeDefinition.setDescription("describe charge");

        portfolioManager.createChargeDefinition(product.getIdentifier(), chargeDefinition);

        chargeDefinition.setName("charge12345");
        chargeDefinition.setFromAccountDesignator("Paul");
        chargeDefinition.setToAccountDesignator("Motia");

        Gson gson = new Gson();
        this.mockMvc.perform(put("/products/" + product.getIdentifier() + "/charges/" + chargeDefinition.getIdentifier())
                .accept(MediaType.APPLICATION_JSON_VALUE)
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .header(TENANT_HEADER, tenantDataStoreContext.getTenantName())
                .content(gson.toJson(chargeDefinition)))
                .andExpect(status().isAccepted())
                .andDo(document(
                        "document-change-charge-definition", preprocessRequest(prettyPrint()), preprocessResponse(prettyPrint()),
                        requestFields(
                                fieldWithPath("identifier").description("Charge definition's identifier"),
                                fieldWithPath("name").description("Charge definitions given name"),
                                fieldWithPath("fromAccountDesignator").description("From account designator"),
                                fieldWithPath("toAccountDesignator").description("To account designator"),
                                fieldWithPath("amount").description("Charge definition's amount"),
                                fieldWithPath("chargeMethod").description("Charge definitions charge method"),
                                fieldWithPath("chargeAction").description("Charge definition's charge action"),
                                fieldWithPath("description").description("Employee's middle name"),
                                fieldWithPath("readOnly").description("Readability"))));
    }

    @Test
    public void documentGetChargeDefinition() throws Exception {

        final Product product = createProduct();

        final ChargeDefinition chargeDefinition = new ChargeDefinition();
        chargeDefinition.setIdentifier("charge10");
        chargeDefinition.setName("core123");
        chargeDefinition.setFromAccountDesignator("pembe");
        chargeDefinition.setToAccountDesignator("miriam");
        chargeDefinition.setAmount(BigDecimal.ONE.setScale(3, BigDecimal.ROUND_UNNECESSARY));
        chargeDefinition.setChargeMethod(ChargeDefinition.ChargeMethod.FIXED);
        chargeDefinition.setChargeAction(Action.OPEN.name());
        chargeDefinition.setDescription("describe charge");

        portfolioManager.createChargeDefinition(product.getIdentifier(), chargeDefinition);


        this.mockMvc.perform(get("/products/" + product.getIdentifier() + "/charges/" + chargeDefinition.getIdentifier())
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .accept(MediaType.ALL_VALUE))
                .andExpect(status().isOk())
                .andDo(document("document-get-case-document", preprocessRequest(prettyPrint()), preprocessResponse(prettyPrint()),
                        responseFields(
                                fieldWithPath("identifier").description("Charge definition's identifier"),
                                fieldWithPath("name").description("Charge definitions given name"),
                                fieldWithPath("description").description("Charge definitions description"),
                                fieldWithPath("accrueAction").description("Charge definitions accrue action"),
                                fieldWithPath("proportionalTo").description("Charge definitions proportional to"),
                                fieldWithPath("accrualAccountDesignator").description("Charge definitions accrual acion generatort"),
                                fieldWithPath("forCycleSizeUnit").description("Charge definitions cycle size unit"),
                                fieldWithPath("forSegmentSet").description("Charge definitions segment set"),
                                fieldWithPath("fromSegment").description("Charge definitions from segment"),
                                fieldWithPath("toSegment").description("Charge definitions to segment"),
                                fieldWithPath("chargeOnTop").description("Charge definitions charge on top"),
                                fieldWithPath("fromAccountDesignator").description("From account designator"),
                                fieldWithPath("toAccountDesignator").description("To account designator"),
                                fieldWithPath("amount").description("Charge definition's amount"),
                                fieldWithPath("chargeMethod").description("Charge definitions charge method"),
                                fieldWithPath("chargeAction").description("Charge definition's charge action"),
                                fieldWithPath("description").description("Employee's middle name"),
                                fieldWithPath("readOnly").description("Readability"))));
    }

    @Test
    public void documentDeleteChargeDefinition () throws Exception{

        final Product product = createProduct();

        final ChargeDefinition chargeDefinitionToDelete = new ChargeDefinition();
        chargeDefinitionToDelete.setAmount(BigDecimal.TEN);
        chargeDefinitionToDelete.setIdentifier("random123");
        chargeDefinitionToDelete.setName("account");
        chargeDefinitionToDelete.setDescription("account charge definition");
        chargeDefinitionToDelete.setChargeAction(Action.APPROVE.name());
        chargeDefinitionToDelete.setChargeMethod(ChargeDefinition.ChargeMethod.FIXED);
        chargeDefinitionToDelete.setToAccountDesignator(AccountDesignators.GENERAL_LOSS_ALLOWANCE);
        chargeDefinitionToDelete.setFromAccountDesignator(AccountDesignators.INTEREST_ACCRUAL);
        portfolioManager.createChargeDefinition(product.getIdentifier(), chargeDefinitionToDelete);
        this.eventRecorder.wait(EventConstants.POST_CHARGE_DEFINITION,
                new ChargeDefinitionEvent(product.getIdentifier(), chargeDefinitionToDelete.getIdentifier()));

        this.mockMvc.perform(delete("/products/"+product.getIdentifier()+"/charges/" + chargeDefinitionToDelete.getIdentifier())
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .accept(MediaType.ALL_VALUE))
                .andExpect(status().isAccepted())
                .andDo(document("document-delete-charge-definition", preprocessResponse(prettyPrint())));
    }

}
