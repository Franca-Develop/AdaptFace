package com.dev.adaptface.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.dev.adaptface.controller.AccessController;
import com.dev.adaptface.entity.Credential;
import com.dev.adaptface.entity.Enrollment;
import com.dev.adaptface.entity.User;
import com.dev.adaptface.exceptions.GlobalExceptionHandler;
import com.dev.adaptface.repository.AccessLogRepository;
import com.dev.adaptface.repository.CredentialRepository;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class AccessFailureHttpTests {

    private CredentialRepository credentials;
    private AccessLogRepository logs;
    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        credentials = mock(CredentialRepository.class);
        logs = mock(AccessLogRepository.class);
        AccessLogService history = new AccessLogService(logs);
        AccessService service = new AccessService(credentials, history, new BCryptPasswordEncoder());
        mvc = MockMvcBuilders.standaloneSetup(new AccessController(service, history))
                .setControllerAdvice(new GlobalExceptionHandler()).build();
    }

    @Test
    void shouldReturnSafeDenialWhenHistoryCannotBeSaved() throws Exception {
        User user = new User();
        user.setId(1L);
        user.setName("Pessoa sintética");
        Enrollment enrollment = new Enrollment();
        enrollment.setId(1L);
        enrollment.setUser(user);
        enrollment.setActive(true);
        Credential credential = new Credential();
        credential.setEnrollment(enrollment);
        when(credentials.findByRfidTag("SYNTHETIC")).thenReturn(Optional.of(credential));
        when(logs.saveAndFlush(any())).thenThrow(new DataAccessResourceFailureException("synthetic-secret"));
        performUnavailable();
        verify(logs).saveAndFlush(any());
    }

    @Test
    void shouldReturnSafeDenialWhenDatabaseCannotBeRead() throws Exception {
        when(credentials.findByRfidTag("SYNTHETIC"))
                .thenThrow(new DataAccessResourceFailureException("synthetic-secret"));
        performUnavailable();
        verifyNoInteractions(logs);
    }

    private void performUnavailable() throws Exception {
        mvc.perform(post("/access/validate").contentType(MediaType.APPLICATION_JSON).content("""
                        {"credentialType":"RFID","rfidTag":"SYNTHETIC","machineModel":"AUDIT","entry":true}
                        """))
                .andExpect(status().isServiceUnavailable()).andExpect(jsonPath("$.result").value("DENIED"))
                .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("synthetic-secret"))))
                .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("GRANTED"))));
    }
}
