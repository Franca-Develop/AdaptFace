package com.dev.adaptface;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class OpenApiDocumentationTests {

    @Autowired
    private MockMvc mvc;

    @Test
    void publishesSwaggerUiWithoutAuthentication() throws Exception {
        mvc.perform(get("/swagger-ui.html"))
                .andExpect(status().is3xxRedirection());
    }

    @Test
    void documentsAllApplicationEndpointsAndSecuritySchemes() throws Exception {
        mvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.info.title").value("AdaptFace API"))
                .andExpect(jsonPath("$.servers[0].url").value("http://localhost:8080"))
                .andExpect(jsonPath("$.components.securitySchemes.bearerAuth").exists())
                .andExpect(jsonPath("$.components.securitySchemes.machineApiKey").exists())
                .andExpect(jsonPath("$.paths['/auth/login'].post.summary").exists())
                .andExpect(jsonPath("$.paths['/auth/bootstrap'].post.summary").exists())
                .andExpect(jsonPath("$.paths['/registrations'].post.summary").exists())
                .andExpect(jsonPath("$.paths['/institutions'].get.summary").exists())
                .andExpect(jsonPath("$.paths['/users'].get.summary").exists())
                .andExpect(jsonPath("$.paths['/users/{id}'].put.summary").exists())
                .andExpect(jsonPath("$.paths['/enrollments/{id}/status'].patch.summary").exists())
                .andExpect(jsonPath("$.paths['/enrollments/{enrollmentId}/credentials/face'].put.summary").exists())
                .andExpect(jsonPath("$.paths['/access/validate'].post.summary").exists())
                .andExpect(jsonPath("$.paths['/access/logs'].get.summary").exists());
    }

    @Test
    void documentsValidationConstraintsAndResponseCodes() throws Exception {
        mvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.components.schemas.RegistrationRequestDTO.properties.faceEmbedding.minItems").value(512))
                .andExpect(jsonPath("$.components.schemas.RegistrationRequestDTO.properties.faceEmbedding.maxItems").value(512))
                .andExpect(jsonPath("$.paths['/registrations'].post.responses['201']").exists())
                .andExpect(jsonPath("$.paths['/registrations'].post.responses['409']").exists())
                .andExpect(jsonPath("$.paths['/access/validate'].post.responses['413']").exists())
                .andExpect(jsonPath("$.paths['/access/validate'].post.responses['503']").exists());
    }
}
