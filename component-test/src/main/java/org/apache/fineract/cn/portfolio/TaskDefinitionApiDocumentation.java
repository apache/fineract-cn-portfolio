package org.apache.fineract.cn.portfolio;

import com.google.gson.Gson;
import org.apache.fineract.cn.portfolio.api.v1.domain.Product;
import org.apache.fineract.cn.portfolio.api.v1.domain.TaskDefinition;
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

public class TaskDefinitionApiDocumentation extends AbstractPortfolioTest {

    @Rule
    public final JUnitRestDocumentation restDocumentation = new JUnitRestDocumentation("build/doc/generated-snippets/test-taskdefinition");

    @Autowired
    private WebApplicationContext context;

    private MockMvc mockMvc;

    @Before
    public void setUp ( ) {

        this.mockMvc = MockMvcBuilders.webAppContextSetup(this.context)
                .apply(documentationConfiguration(this.restDocumentation))
                .alwaysDo(document("{method-name}", preprocessRequest(prettyPrint()), preprocessResponse(prettyPrint())))
                .build();
    }

    @Test
    public void documentListTaskDefinitions ( ) throws Exception {
        final Product product = createProduct();

        try {
            this.mockMvc.perform(get("/products/" +product.getIdentifier()+"/tasks/")
                    .accept(MediaType.APPLICATION_JSON_VALUE)
                    .contentType(MediaType.APPLICATION_JSON_VALUE)
                    .header(TENANT_HEADER, tenantDataStoreContext.getTenantName()))
                    .andExpect(status().isOk())
                    .andDo(document(
                            "document-list-task-definitions", preprocessRequest(prettyPrint()),
                            responseFields(
                                    fieldWithPath("identifier").type("String").description("task identifier's identifier"),
                                    fieldWithPath("name").type("String").description("task identifier's name"),
                                    fieldWithPath("description").type("String").description("task identifier's description"),
                                    fieldWithPath("actions").description("The task definition action"),
                                    fieldWithPath("fourEyes").type("String").description("task identifier's identifier"),
                                    fieldWithPath("mandatory").type("String").description("task identifier's identifier")
                            )
                    ));
        } catch (Exception e) {
            e.printStackTrace();
        }

    }


    @Test
    public void documentGetTaskDefinition ( ) throws Exception {
        final Product product = createProduct();
        final TaskDefinition taskDefinition = createTaskDefinition(product);

        try {
            this.mockMvc.perform(get("/products/" +product.getIdentifier()+"/tasks/" +taskDefinition.getIdentifier())
                    .accept(MediaType.APPLICATION_JSON_VALUE)
                    .contentType(MediaType.APPLICATION_JSON_VALUE)
                    .header(TENANT_HEADER, tenantDataStoreContext.getTenantName()))
                    .andExpect(status().isOk())
                    .andDo(document(
                            "document-get-task-definition", preprocessRequest(prettyPrint()),
                            responseFields(
                                    fieldWithPath("identifier").type("String").description("task identifier's identifier"),
                                    fieldWithPath("name").type("String").description("task identifier's name"),
                                    fieldWithPath("description").type("String").description("task identifier's description"),
                                    fieldWithPath("actions").description("The task definition action"),
                                    fieldWithPath("fourEyes").type("String").description("task identifier's identifier"),
                                    fieldWithPath("mandatory").type("String").description("task identifier's identifier")
                            )
                    ));
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    @Test
    public void documentChangeTaskDefinition () throws Exception {

        final Product product = createProduct();
        final TaskDefinition taskDefinition = createTaskDefinition(product);
        taskDefinition.setDescription("Describe task definition");
        taskDefinition.setFourEyes(false);

        Gson gson = new Gson();
        this.mockMvc.perform(put("/products/" + product.getIdentifier() + "/tasks/" +taskDefinition.getIdentifier() )
                .accept(MediaType.APPLICATION_JSON_VALUE)
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .header(TENANT_HEADER, tenantDataStoreContext.getTenantName())
                .content(gson.toJson(taskDefinition)))
                .andExpect(status().isAccepted())
                .andDo(document(
                        "document-change-task-definition", preprocessRequest(prettyPrint()),
                        requestFields(
                                fieldWithPath("identifier").type("String").description("task identifier's identifier"),
                                fieldWithPath("name").type("String").description("task identifier's name"),
                                fieldWithPath("description").type("String").description("task identifier's description"),
                                fieldWithPath("actions").description("The task definition action"),
                                fieldWithPath("fourEyes").type("String").description("task identifier's identifier"),
                                fieldWithPath("mandatory").type("String").description("task identifier's identifier")
                        )

                        ));

    }

    @Test
    public void documentAddTaskDefinition () throws Exception {

        final Product product = createProduct();
        final TaskDefinition taskDefinition = createTaskDefinition(product);
        taskDefinition.setIdentifier("ask");

        Gson gson = new Gson();
        this.mockMvc.perform(post("/products/"+product.getIdentifier()+"/tasks/")
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content(gson.toJson(taskDefinition)))
                .andExpect(status().isAccepted())
                .andDo(document("document-add-task-definition", preprocessRequest(prettyPrint()), preprocessResponse(prettyPrint()),
                        requestFields(
                                fieldWithPath("identifier").type("String").description("task identifier's identifier"),
                                fieldWithPath("name").type("String").description("task identifier's name"),
                                fieldWithPath("description").type("String").description("task identifier's description"),
                                fieldWithPath("actions").description("The task definition action"),
                                fieldWithPath("fourEyes").type("String").description("task identifier's identifier"),
                                fieldWithPath("mandatory").type("String").description("task identifier's identifier"))));
    }

    @Test
    public void documentDeleteTaskDefinition () throws Exception {
        final Product product = createProduct();
        final TaskDefinition taskDefinition = createTaskDefinition(product);

        this.mockMvc.perform(delete("/products/"+product.getIdentifier()+"/tasks/" + taskDefinition.getIdentifier())
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .accept(MediaType.ALL_VALUE))
                .andExpect(status().isAccepted())
                .andDo(document("document-delete-task-definition", preprocessResponse(prettyPrint())));
    }
}
