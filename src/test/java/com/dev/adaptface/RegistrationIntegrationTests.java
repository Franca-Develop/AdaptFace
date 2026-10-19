package com.dev.adaptface;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.dev.adaptface.repository.CredentialRepository;
import com.dev.adaptface.repository.UserRepository;
import com.dev.adaptface.dto.request.InstitutionRequestDTO;
import com.dev.adaptface.dto.request.UserRequestDTO;
import com.dev.adaptface.entity.enums.UserType;
import com.dev.adaptface.service.InstitutionService;
import com.dev.adaptface.service.UserService;
import com.jayway.jsonpath.JsonPath;
import java.util.Collections;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class RegistrationIntegrationTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CredentialRepository credentialRepository;

    @Autowired
    private InstitutionService institutionService;

    @Autowired
    private UserService userService;

    @Test
    void shouldRegisterAtomicallyAndEnforceAdministrativeAccess() throws Exception {
        InstitutionRequestDTO institution = new InstitutionRequestDTO();
        institution.setName("Instituição de teste");
        Long existingInstitutionId = institutionService.create(institution).getId();
        UserRequestDTO imported = new UserRequestDTO();
        imported.setInstitutionId(existingInstitutionId);
        imported.setName("Aluno já existente de teste");
        imported.setCpf("90000000000");
        imported.setEmail("existing@example.test");
        imported.setPassword("senha-existente");
        imported.setUserType(UserType.STUDENT);
        imported.setRegistrationNumber(999);
        imported.setPeriod("Não informado");
        imported.setCourse("ADS");
        userService.create(imported);
        mockMvc.perform(get("/auth/setup-status"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.requiresSetup").value(true));
        mockMvc.perform(post("/registrations").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isUnauthorized());

        String response = mockMvc.perform(post("/auth/bootstrap")
                        .header("X-Bootstrap-Key", "test-bootstrap-key-with-at-least-32-characters")
                        .contentType(MediaType.APPLICATION_JSON).content("""
                                {"institutionName":"Instituição de teste", "name":"Administrador de teste",
                                 "cpf":"90000000001", "email":"admin@example.test", "password":"teste-seguro",
                                 "department":"Testes"}
                                """))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        String token = JsonPath.read(response, "$.token");
        assertThat(userRepository.findByEmail("existing@example.test").orElseThrow().getUserType())
                .isEqualTo(UserType.STUDENT);
        mockMvc.perform(get("/auth/setup-status"))
                .andExpect(jsonPath("$.requiresSetup").value(false));
        String institutions = mockMvc.perform(get("/institutions").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(1))
                .andReturn().getResponse().getContentAsString();
        Number institutionId = JsonPath.read(institutions, "$[0].id");
        String embedding = "[1.0," + String.join(",", Collections.nCopies(511, "0.0")) + "]";
        String user = """
                {"institutionId":%d,"name":"Aluno de teste","cpf":"90000000002",
                 "email":"student@example.test","password":"senha-aluno","userType":"STUDENT",
                 "registrationNumber":321,"period":"Não informado","course":"ADS"}
                """.formatted(institutionId.longValue());
        String registration = "{\"user\":" + user + ",\"faceEmbedding\":" + embedding
                + ",\"faceModel\":\"DEEPFACE_ARCFACE_OPENCV_V1\",\"rfidTag\":\"ABCD1234\"}";
        mockMvc.perform(post("/registrations").header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON).content(registration))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.hasFace").value(true))
                .andExpect(jsonPath("$.enrollmentId").isNumber());
        assertThat(userRepository.findByEmail("student@example.test")).isPresent();
        assertThat(credentialRepository.count()).isEqualTo(1);
        assertThat(credentialRepository.findAll().get(0).getFaceModel()).isEqualTo("DEEPFACE_ARCFACE_OPENCV_V1");

        String other = registration.replace("90000000002", "90000000003")
                .replace("student@example.test", "other@example.test")
                .replace(embedding, "[0.0,1.0," + String.join(",", Collections.nCopies(510, "0.0")) + "]");
        mockMvc.perform(post("/registrations").header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON).content(other))
                .andExpect(status().isBadRequest());
        assertThat(userRepository.findByEmail("other@example.test")).isEmpty();
        assertThat(credentialRepository.count()).isEqualTo(1);

        mockMvc.perform(post("/registrations").header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"user\":" + user + ",\"faceEmbedding\":[1.0]}"))
                .andExpect(status().isBadRequest());
        String login = mockMvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"student@example.test\",\"password\":\"senha-aluno\"}"))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        String studentToken = JsonPath.read(login, "$.token");
        mockMvc.perform(post("/registrations").header("Authorization", "Bearer " + studentToken)
                        .contentType(MediaType.APPLICATION_JSON).content(other))
                .andExpect(status().isForbidden());
    }
}
