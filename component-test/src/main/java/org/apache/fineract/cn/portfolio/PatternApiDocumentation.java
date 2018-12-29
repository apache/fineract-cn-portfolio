package org.apache.fineract.cn.portfolio;

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
import static org.springframework.restdocs.operation.preprocess.Preprocessors.preprocessRequest;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.preprocessResponse;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.prettyPrint;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class PatternApiDocumentation extends AbstractPortfolioTest {
    @Rule
    public final JUnitRestDocumentation restDocumentation = new JUnitRestDocumentation("build/doc/generated-snippets/test-pattern");

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
    public void documentGetPatterns ( ) throws Exception {

        try {
            this.mockMvc.perform(get("/patterns/")
                    .accept(MediaType.APPLICATION_JSON_VALUE)
                    .contentType(MediaType.APPLICATION_JSON_VALUE)
                    .header(TENANT_HEADER, tenantDataStoreContext.getTenantName()))
                    .andExpect(status().isOk())
                    .andDo(document(
                            "document-get-patterns", preprocessRequest(prettyPrint()),
                            responseFields(
                                    fieldWithPath("parameterPackage").description("Pattern's parameter package"),
                                    fieldWithPath("accountAssignmentGroups").description("Pattern's groups"),
                                    fieldWithPath("accountAssignmentsRequired").description("Pattern's account assignment status")
                                    )

                                    ));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
