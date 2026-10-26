package com.dev.adaptface;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.dev.adaptface.dto.request.AccessValidationRequestDTO;
import com.dev.adaptface.dto.request.CredentialCodeRequestDTO;
import com.dev.adaptface.dto.request.CredentialFaceRequestDTO;
import com.dev.adaptface.dto.request.CredentialRfidRequestDTO;
import com.dev.adaptface.entities.AccessLog;
import com.dev.adaptface.entities.Enrollment;
import com.dev.adaptface.entities.enums.AccessResult;
import com.dev.adaptface.entities.enums.CredentialType;
import com.dev.adaptface.exceptions.BusinessException;
import com.dev.adaptface.repository.AccessLogRepository;
import com.dev.adaptface.repository.CredentialRepository;
import com.dev.adaptface.repository.EnrollmentRepository;
import com.dev.adaptface.service.AccessLogService;
import com.dev.adaptface.service.AccessService;
import com.dev.adaptface.service.CredentialService;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import org.junit.jupiter.api.Assertions;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import com.dev.adaptface.dto.request.InstitutionRequestDTO;
import com.dev.adaptface.dto.request.UserRequestDTO;
import com.dev.adaptface.entities.User;
import com.dev.adaptface.entities.enums.UserType;
import com.dev.adaptface.repository.UserRepository;
import com.dev.adaptface.security.JwtService;
import com.dev.adaptface.service.InstitutionService;
import com.dev.adaptface.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@Transactional
class SecurityAuditIntegrationTests {

    protected static final String MACHINE_KEY = "test-machine-key-with-at-least-32-characters";

    @Autowired protected MockMvc mvc;
    @Autowired protected UserRepository users;
    @Autowired private UserService userService;
    @Autowired private InstitutionService institutions;
    @Autowired private JwtService jwt;
    @Autowired private PasswordEncoder encoder;
    @Autowired private EnrollmentRepository enrollments;
    @Autowired private CredentialRepository credentials;
    @Autowired private CredentialService credentialService;
    @Autowired private AccessService accessService;
    @Autowired private AccessLogRepository logs;
    @Autowired private AccessLogService history;
    @Value("${jwt.secret}") private String signingSecret;

    private Long institutionId;
    private User admin;
    private String token;

    @BeforeEach
    void setUp() {
        InstitutionRequestDTO institution = new InstitutionRequestDTO();
        institution.setName("Instituição sintética");
        institutionId = institutions.create(institution).getId();
        UserRequestDTO request = new UserRequestDTO();
        request.setInstitutionId(institutionId);
        request.setName("Administrador sintético");
        request.setCpf("80000000001");
        request.setEmail("audit@example.test");
        request.setPassword("synthetic-password");
        request.setUserType(UserType.ADMIN);
        request.setDepartment("Testes");
        Long id = userService.create(request).getId();
        admin = users.findById(id).orElseThrow();
        token = jwt.generateToken(admin);
    }

    @Test
    void shouldRejectMalformedForgedAndOversizedRequests() throws Exception {
        for (String body : new String[] {"{", "{}", "{\"credentialType\":\"OTHER\"}",
                "{\"credentialType\":\"RFID\",\"credentialType\":\"FACE\"}",
                "{\"credentialType\":\"RFID\",\"rfidTag\":\"UNKNOWN\",\"result\":\"GRANTED\"}",
                "{\"credentialType\":\"RFID\",\"rfidTag\":\"UNKNOWN\",\"name\":\"forged\"}"}) {
            mvc.perform(post("/access/validate").header("X-API-Key", MACHINE_KEY)
                            .contentType(MediaType.APPLICATION_JSON).content(body))
                    .andExpect(status().isBadRequest()).andExpect(jsonPath("$.result").value("DENIED"));
        }
        mvc.perform(post("/access/validate").header("X-API-Key", MACHINE_KEY)
                        .contentType(MediaType.APPLICATION_JSON).content(" ".repeat(65537)))
                .andExpect(status().isContentTooLarge()).andExpect(jsonPath("$.result").value("DENIED"));
    }

    @Test
    void shouldProtectAdministrativeAndRecognitionRoutes() throws Exception {
        mvc.perform(get("/users")).andExpect(status().isUnauthorized());
        mvc.perform(get("/users").header("Authorization", "Bearer invalid"))
                .andExpect(status().isUnauthorized());
        mvc.perform(get("/users").header("X-API-Key", MACHINE_KEY)).andExpect(status().isUnauthorized());
        mvc.perform(post("/access/validate").header("X-API-Key", "wrong")
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isUnauthorized()).andExpect(jsonPath("$.result").value("DENIED"));
        mvc.perform(post("/access/validate").header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isUnauthorized());
        mvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"absent@example.test\",\"password\":\"synthetic-password\"}"))
                .andExpect(status().isUnauthorized()).andExpect(jsonPath("$.message").value("Email ou senha inválidos"));
    }

    @Test
    void shouldListEditAndRevokeTokensWithoutLeakingSecrets() throws Exception {
        assertThat(admin.getPassword()).isNotEqualTo("synthetic-password");
        assertThat(encoder.matches("synthetic-password", admin.getPassword())).isTrue();
        mvc.perform(get("/users?size=1000").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk()).andExpect(jsonPath("$.page.size").value(100))
                .andExpect(jsonPath("$.content[0].password").doesNotExist())
                .andExpect(jsonPath("$.content[0].authVersion").doesNotExist());
        String body = updateBody("audit@example.test", "80000000001", "new-synthetic-password");
        mvc.perform(put("/users/{id}", admin.getId()).header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk()).andExpect(jsonPath("$.name").value("Nome atualizado"))
                .andExpect(jsonPath("$.password").doesNotExist());
        assertThat(admin.getAuthVersion()).isEqualTo(1);
        assertThat(encoder.matches("new-synthetic-password", admin.getPassword())).isTrue();
        mvc.perform(get("/users").header("Authorization", "Bearer " + token)).andExpect(status().isUnauthorized());
        token = jwt.generateToken(admin);
        mvc.perform(get("/users/{id}", Long.MAX_VALUE).header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound());
        mvc.perform(put("/users/{id}", admin.getId()).header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON).content(body.replace("\"name\":", "\"userType\":\"STUDENT\",\"name\":")))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldRejectReassignedIdentityAndRoleWithOldToken() {
        assertThat(jwt.isValid(token, admin)).isTrue();
        User other = new User();
        other.setId(admin.getId() + 1);
        other.setEmail(admin.getEmail());
        other.setUserType(UserType.ADMIN);
        assertThat(jwt.isValid(token, other)).isFalse();
        other.setId(admin.getId());
        other.setUserType(UserType.STUDENT);
        assertThat(jwt.isValid(token, other)).isFalse();
        other.setUserType(UserType.ADMIN);
        other.setAuthVersion(1L);
        assertThat(jwt.isValid(token, other)).isFalse();
    }

    @Test
    void shouldPersistUnknownAndSemanticInvalidAttemptsAndPageHistory() throws Exception {
        for (String body : new String[] {"{\"credentialType\":\"RFID\",\"rfidTag\":\"UNKNOWN\"}",
                "{\"credentialType\":\"FACE\",\"faceEmbedding\":[0,0]}",
                "{\"credentialType\":\"CODE\",\"code\":\"\"}"}) {
            mvc.perform(post("/access/validate").header("X-API-Key", MACHINE_KEY)
                            .contentType(MediaType.APPLICATION_JSON).content(body.substring(0, body.length() - 1)
                                    + ",\"machineModel\":\"AUDIT\",\"entry\":true}"))
                    .andExpect(status().isOk()).andExpect(jsonPath("$.result").value("DENIED"));
        }
        mvc.perform(get("/access/logs?size=2").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk()).andExpect(jsonPath("$.page.totalElements").value(3))
                .andExpect(jsonPath("$.content.length()").value(2))
                .andExpect(jsonPath("$.content[0].result").value("DENIED"))
                .andExpect(jsonPath("$.content[0].enrollmentId").isEmpty());
        mvc.perform(get("/access/logs?page=1&size=2").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk()).andExpect(jsonPath("$.content.length()").value(1));
    }

    private String updateBody(String email, String cpf, String password) {
        return """
                {"institutionId":%d,"name":"Nome atualizado","cpf":"%s","email":"%s",
                 "password":"%s","department":"Auditoria"}
                """.formatted(institutionId, cpf, email, password);
    }

    @Test
    void shouldValidateRealStoredCredentialsAndRejectDuplicates() {
        var enrollment = new Enrollment();
        enrollment.setUser(admin);
        enrollment = enrollments.saveAndFlush(enrollment);
        var otherRequest = new UserRequestDTO();
        otherRequest.setInstitutionId(institutionId);
        otherRequest.setName("Outro sintético");
        otherRequest.setCpf("80000000002");
        otherRequest.setEmail("other-audit@example.test");
        otherRequest.setPassword("synthetic-password");
        otherRequest.setUserType(UserType.EMPLOYEE);
        otherRequest.setDepartment("Teste");
        var other = new Enrollment();
        other.setUser(users.findById(userService.create(otherRequest).getId()).orElseThrow());
        Long otherId = enrollments.saveAndFlush(other).getId();

        var vector = new ArrayList<>(Collections.nCopies(512, 0.0f));
        vector.set(0, 1.0f);
        var face = new CredentialFaceRequestDTO();
        face.setFaceEmbedding(vector);
        face.setFaceModel(CredentialService.DEEPFACE_FACE_MODEL);
        credentialService.saveFace(enrollment.getId(), face);
        var code = new CredentialCodeRequestDTO();
        code.setCode("synthetic-code");
        credentialService.saveCode(enrollment.getId(), code);
        var stored = credentials.findByEnrollmentId(enrollment.getId()).orElseThrow();
        assertThat(stored.getAccessCode()).isNotEqualTo("synthetic-code");
        assertThat(encoder.matches("synthetic-code", stored.getAccessCode())).isTrue();
        var rfid = new CredentialRfidRequestDTO();
        rfid.setRfidTag("aa:bb-1234");
        credentialService.saveRfid(enrollment.getId(), rfid);
        assertThat(stored.getRfidTag()).isEqualTo("AABB1234");
        var request = new AccessValidationRequestDTO();
        request.setCredentialType(CredentialType.FACE);
        request.setFaceEmbedding(vector);
        request.setFaceModel(face.getFaceModel());
        request.setEntry(true);
        request.setMachineModel("AUDIT");
        assertThat(accessService.validate(request).getResult()).isEqualTo(AccessResult.GRANTED);
        enrollment.setActive(false);
        enrollments.saveAndFlush(enrollment);
        assertThat(accessService.validate(request).getReason()).isEqualTo("Matrícula inativa");
        Assertions.assertThrows(BusinessException.class,
                () -> credentialService.saveFace(otherId, face));
        Assertions.assertThrows(BusinessException.class,
                () -> credentialService.saveCode(otherId, code));
        Assertions.assertThrows(BusinessException.class,
                () -> credentialService.saveRfid(otherId, rfid));
        assertThat(credentials.findByEnrollmentId(otherId)).isEmpty();
    }

    @Test
    void shouldRejectDuplicateEmailWithoutChangingUser() throws Exception {
        var request = new UserRequestDTO();
        request.setInstitutionId(institutionId);
        request.setName("Outro sintético");
        request.setCpf("80000000002");
        request.setEmail("other-audit@example.test");
        request.setPassword("synthetic-password");
        request.setUserType(UserType.EMPLOYEE);
        request.setDepartment("Teste");
        userService.create(request);
        mvc.perform(put("/users/{id}", admin.getId()).header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updateBody("other-audit@example.test", "80000000001", "new-synthetic-password")))
                .andExpect(status().isBadRequest());
        assertThat(admin.getEmail()).isEqualTo("audit@example.test");
        assertThat(admin.getName()).isEqualTo("Administrador sintético");
        assertThat(admin.getAuthVersion()).isZero();
    }

    @Test
    void shouldUseIdAsStableHistoryTieBreaker() {
        var time = LocalDateTime.of(2026, 1, 1, 10, 0);
        var first = new AccessLog();
        first.setLoggedAt(time);
        first.setResult(AccessResult.DENIED);
        logs.saveAndFlush(first);
        var second = new AccessLog();
        second.setLoggedAt(time);
        second.setResult(AccessResult.DENIED);
        logs.saveAndFlush(second);
        assertThat(history.findAll(null, PageRequest.of(0, 1))
                .getContent().get(0).getId()).isEqualTo(second.getId());
        assertThat(history.findAll(null, PageRequest.of(1, 1))
                .getContent().get(0).getId()).isEqualTo(first.getId());
    }

    @Test
    void shouldRejectExpiredSignedToken() throws Exception {
        String expired = Jwts.builder().setSubject(admin.getEmail())
                .claim("userId", admin.getId()).claim("userType", "EMPLOYEE")
                .setExpiration(new Date(System.currentTimeMillis() - 60000))
                .signWith(Keys.hmacShaKeyFor(Decoders.BASE64.decode(signingSecret)))
                .compact();
        mvc.perform(get("/users").header("Authorization", "Bearer " + expired))
                .andExpect(status().isUnauthorized());
    }
}
