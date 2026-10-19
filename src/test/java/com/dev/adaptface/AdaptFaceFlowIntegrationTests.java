package com.dev.adaptface;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import java.util.Collections;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class AdaptFaceFlowIntegrationTests {

    private static final String BOOTSTRAP_KEY = "test-bootstrap-key-with-at-least-32-characters";
    private static final String MACHINE_API_KEY = "test-machine-key-with-at-least-32-characters";

    @Autowired
    private MockMvc mockMvc;

    @Test
    void shouldCompleteBootstrapAndRfidAccessFlow() throws Exception {
        String bootstrapResponse = mockMvc.perform(post("/auth/bootstrap")
                        .header("X-Bootstrap-Key", BOOTSTRAP_KEY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "institutionName": "SENAI Sorocaba",
                                  "name": "Administrador",
                                  "cpf": "11111111111",
                                  "email": "admin@adaptface.com",
                                  "password": "senha-segura",
                                  "gender": "UNDEFINED",
                                  "department": "TI"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andReturn().getResponse().getContentAsString();
        String token = JsonPath.read(bootstrapResponse, "$.token");

        mockMvc.perform(post("/auth/bootstrap")
                        .header("X-Bootstrap-Key", BOOTSTRAP_KEY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "institutionName": "Outra",
                                  "name": "Outro",
                                  "cpf": "22222222222",
                                  "email": "outro@adaptface.com",
                                  "password": "outra-senha",
                                  "department": "TI"
                                }
                                """))
                .andExpect(status().isBadRequest());

        String institutionsResponse = mockMvc.perform(get("/institutions")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("SENAI Sorocaba"))
                .andReturn().getResponse().getContentAsString();
        Number institutionId = JsonPath.read(institutionsResponse, "$[0].id");

        String userResponse = mockMvc.perform(post("/users")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "institutionId": %d,
                                  "name": "Maria",
                                  "cpf": "33333333333",
                                  "email": "maria@adaptface.com",
                                  "password": "senha-aluna",
                                  "gender": "FEMALE",
                                  "userType": "STUDENT",
                                  "registrationNumber": 12345,
                                  "period": "Noturno",
                                  "course": "ADS"
                                }
                                """.formatted(institutionId.longValue())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Maria"))
                .andReturn().getResponse().getContentAsString();
        Number userId = JsonPath.read(userResponse, "$.id");

        String enrollmentResponse = mockMvc.perform(post("/enrollments")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"userId\":%d}".formatted(userId.longValue())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.active").value(true))
                .andReturn().getResponse().getContentAsString();
        Number enrollmentId = JsonPath.read(enrollmentResponse, "$.id");

        mockMvc.perform(put("/enrollments/{id}/credentials/rfid", enrollmentId.longValue())
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"rfidTag\":\"a1:b2-c3 d4\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.hasRfid").value(true));

        String accessRequest = """
                {
                  "credentialType": "RFID",
                  "rfidTag": "A1B2C3D4",
                  "machineModel": "RFID-READER",
                  "entry": true
                }
                """;
        mockMvc.perform(post("/access/validate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(accessRequest))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(post("/access/validate")
                        .header("X-API-Key", MACHINE_API_KEY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(accessRequest))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result").value("GRANTED"))
                .andExpect(jsonPath("$.name").value("Maria"));

        String faceVector = "[1.0," + String.join(",", Collections.nCopies(511, "0.0")) + "]";
        mockMvc.perform(put("/enrollments/{id}/credentials/face", enrollmentId.longValue())
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"faceEmbedding\":" + faceVector + "}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.hasFace").value(true));

        mockMvc.perform(post("/access/validate")
                        .header("X-API-Key", MACHINE_API_KEY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"credentialType\":\"FACE\",\"faceEmbedding\":" + faceVector
                                + ",\"machineModel\":\"VISION-ROI\",\"entry\":true}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result").value("GRANTED"))
                .andExpect(jsonPath("$.name").value("Maria"));

        mockMvc.perform(get("/access/logs")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].result").value("GRANTED"))
                .andExpect(jsonPath("$.content[0].credentialType").value("FACE"));
    }
}
