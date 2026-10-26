package com.dev.adaptface.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.dev.adaptface.dto.request.AccessValidationRequestDTO;
import com.dev.adaptface.dto.response.AccessValidationResponseDTO;
import com.dev.adaptface.entities.Credential;
import com.dev.adaptface.entities.Enrollment;
import com.dev.adaptface.entities.User;
import com.dev.adaptface.entities.enums.AccessResult;
import com.dev.adaptface.entities.enums.CredentialType;
import com.dev.adaptface.repository.CredentialRepository;
import java.util.List;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class AccessServiceTests {

    @Mock
    private CredentialRepository credentialRepository;

    @Mock
    private AccessLogService accessLogService;

    @Mock
    private PasswordEncoder passwordEncoder;

    private AccessService accessService;

    @BeforeEach
    void setUp() {
        accessService = new AccessService(credentialRepository, accessLogService, passwordEncoder);
        ReflectionTestUtils.setField(accessService, "faceThreshold", 0.6);
    }

    @Test
    void shouldGrantAccessForRegisteredRfid() {
        Credential credential = credential(true);
        when(credentialRepository.findByRfidTag("A1B2C3D4")).thenReturn(Optional.of(credential));

        AccessValidationResponseDTO response = accessService.validate(rfidRequest("a1:b2-c3 d4"));

        assertEquals(AccessResult.GRANTED, response.getResult());
        assertEquals("Maria", response.getName());
        verify(accessLogService).save(eq(credential.getEnrollment()), any(), eq(AccessResult.GRANTED), eq(null),
                eq(null));
    }

    @Test
    void shouldDenyAccessForUnknownRfid() {
        when(credentialRepository.findByRfidTag("UNKNOWN")).thenReturn(Optional.empty());

        AccessValidationResponseDTO response = accessService.validate(rfidRequest("unknown"));

        assertEquals(AccessResult.DENIED, response.getResult());
        assertEquals("Credencial não reconhecida", response.getReason());
        verify(accessLogService).save(eq(null), any(), eq(AccessResult.DENIED), eq(null),
                eq("Credencial não reconhecida"));
    }

    @Test
    void shouldDenyAccessForInactiveEnrollment() {
        Credential credential = credential(false);
        when(credentialRepository.findByRfidTag("A1B2C3D4")).thenReturn(Optional.of(credential));

        AccessValidationResponseDTO response = accessService.validate(rfidRequest("A1B2C3D4"));

        assertEquals(AccessResult.DENIED, response.getResult());
        assertEquals("Matrícula inativa", response.getReason());
    }

    @Test
    void shouldGrantAccessForMatchingFace() {
        Credential credential = credential(true);
        credential.setFaceEmbedding(vector(1.0f, 0.0f));
        when(credentialRepository.findFacialCredentials()).thenReturn(List.of(credential));

        AccessValidationRequestDTO request = new AccessValidationRequestDTO();
        request.setCredentialType(CredentialType.FACE);
        request.setFaceEmbedding(vector(10.0f, 0.1f));
        request.setMachineModel("VISION-ROI");
        request.setEntry(true);

        AccessValidationResponseDTO response = accessService.validate(request);

        assertEquals(AccessResult.GRANTED, response.getResult());
        assertEquals("Maria", response.getName());
    }

    private AccessValidationRequestDTO rfidRequest(String rfidTag) {
        AccessValidationRequestDTO request = new AccessValidationRequestDTO();
        request.setCredentialType(CredentialType.RFID);
        request.setRfidTag(rfidTag);
        request.setMachineModel("RFID-READER");
        request.setEntry(true);
        return request;
    }

    @Test
    void shouldNeverCompareEmbeddingsFromDifferentModels() {
        Credential credential = credential(true);
        List<Float> embedding = new ArrayList<>(Collections.nCopies(512, 0.0f));
        embedding.set(0, 1.0f);
        credential.setFaceEmbedding(embedding);
        when(credentialRepository.findFacialCredentials()).thenReturn(List.of(credential));

        AccessValidationRequestDTO request = new AccessValidationRequestDTO();
        request.setCredentialType(CredentialType.FACE);
        request.setFaceEmbedding(embedding);
        request.setFaceModel(CredentialService.DEEPFACE_FACE_MODEL);
        request.setMachineModel("VISION-ROI");
        request.setEntry(true);
        assertEquals(AccessResult.DENIED, accessService.validate(request).getResult());

        credential.setFaceModel(CredentialService.DEEPFACE_FACE_MODEL);
        assertEquals(AccessResult.GRANTED, accessService.validate(request).getResult());
    }

    @Test
    void shouldRejectWrongDimensionForDeepFace() {
        AccessValidationRequestDTO request = new AccessValidationRequestDTO();
        request.setCredentialType(CredentialType.FACE);
        request.setFaceEmbedding(List.of(1.0f, 0.0f, 0.0f));
        request.setFaceModel(CredentialService.DEEPFACE_FACE_MODEL);
        assertEquals(AccessResult.DENIED, accessService.validate(request).getResult());
    }

    private Credential credential(boolean active) {
        User user = new User();
        user.setId(1L);
        user.setName("Maria");

        Enrollment enrollment = new Enrollment();
        enrollment.setId(1L);
        enrollment.setUser(user);
        enrollment.setActive(active);

        Credential credential = new Credential();
        credential.setEnrollment(enrollment);
        credential.setRfidTag("A1B2C3D4");
        return credential;
    }

    @Test
    void shouldDenyUnknownAndDistantFaces() {
        when(credentialRepository.findFacialCredentials()).thenReturn(List.of());
        assertEquals(AccessResult.DENIED, accessService.validate(faceRequest()).getResult());
        Credential other = credential(true);
        other.setFaceEmbedding(vector(0, 1));
        when(credentialRepository.findFacialCredentials()).thenReturn(List.of(other));
        assertEquals(AccessResult.DENIED, accessService.validate(faceRequest()).getResult());
        verify(accessLogService).save(eq(null), any(), eq(AccessResult.DENIED),
                org.mockito.ArgumentMatchers.argThat(value -> value != null && value.compareTo(java.math.BigDecimal.ONE) == 0),
                eq("Credencial não reconhecida"));
    }

    @Test
    void shouldKeepInactiveFaceIdentityInNegativeHistory() {
        Credential inactive = credential(false);
        inactive.setFaceEmbedding(vector(1, 0));
        when(credentialRepository.findFacialCredentials()).thenReturn(List.of(inactive));
        assertEquals("Matrícula inativa", accessService.validate(faceRequest()).getReason());
        verify(accessLogService).save(eq(inactive.getEnrollment()), any(), eq(AccessResult.DENIED), any(),
                eq("Matrícula inativa"));
    }

    @Test
    void shouldDenyInvalidVectorsAndModels() {
        List<List<Float>> invalid = new ArrayList<>();
        invalid.add(null);
        invalid.add(List.of());
        invalid.add(vector(0, 0));
        invalid.add(vector(Float.NaN, 0));
        invalid.add(vector(Float.POSITIVE_INFINITY, 0));
        invalid.add(List.of(1.0f));
        for (List<Float> embedding : invalid) {
            AccessValidationRequestDTO request = faceRequest();
            request.setFaceEmbedding(embedding);
            assertEquals(AccessResult.DENIED, accessService.validate(request).getResult());
        }
        AccessValidationRequestDTO request = faceRequest();
        request.setFaceModel("UNKNOWN_MODEL");
        assertEquals(AccessResult.DENIED, accessService.validate(request).getResult());
        org.mockito.Mockito.verifyNoInteractions(credentialRepository);
    }

    @Test
    void shouldSkipCorruptStoredVectorsAndRejectAmbiguity() {
        Credential corrupt = credential(true);
        corrupt.setFaceEmbedding(vector(Float.NaN, 0));
        when(credentialRepository.findFacialCredentials()).thenReturn(List.of(corrupt));
        assertEquals(AccessResult.DENIED, accessService.validate(faceRequest()).getResult());
        Credential first = credential(true);
        first.setFaceEmbedding(vector(1, 0));
        Credential second = credential(false);
        second.setFaceEmbedding(vector(1, 0));
        when(credentialRepository.findFacialCredentials()).thenReturn(List.of(first, second));
        assertEquals("Correspondência facial ambígua", accessService.validate(faceRequest()).getReason());
    }

    @Test
    void shouldDenyCodeForInactiveAndAmbiguousEnrollments() {
        Credential inactive = credential(false);
        inactive.setAccessCode("hash");
        when(passwordEncoder.matches("1234", "hash")).thenReturn(true);
        when(credentialRepository.findCodeCredentials()).thenReturn(List.of(inactive));
        AccessValidationRequestDTO request = new AccessValidationRequestDTO();
        request.setCredentialType(CredentialType.CODE);
        request.setCode("1234");
        assertEquals("Matrícula inativa", accessService.validate(request).getReason());
        when(credentialRepository.findCodeCredentials()).thenReturn(List.of(inactive, inactive));
        assertEquals("Código de acesso ambíguo", accessService.validate(request).getReason());
    }

    @Test
    void shouldDenyBrokenIdentityLinks() {
        Credential broken = credential(true);
        broken.getEnrollment().setUser(null);
        when(credentialRepository.findByRfidTag("A1B2C3D4")).thenReturn(Optional.of(broken));
        assertEquals("Vínculo de identidade inválido", accessService.validate(rfidRequest("A1B2C3D4")).getReason());
    }

    @Test
    void shouldNeverReturnGrantWhenDatabaseOrHistoryFails() {
        when(credentialRepository.findByRfidTag("A1B2C3D4"))
                .thenThrow(new org.springframework.dao.DataAccessResourceFailureException("synthetic"));
        assertThrows(org.springframework.dao.DataAccessException.class,
                () -> accessService.validate(rfidRequest("A1B2C3D4")));
        org.mockito.Mockito.verifyNoInteractions(accessLogService);
        org.mockito.Mockito.doReturn(Optional.of(credential(true))).when(credentialRepository).findByRfidTag("A1B2C3D4");
        org.mockito.Mockito.doThrow(new org.springframework.dao.DataAccessResourceFailureException("synthetic"))
                .when(accessLogService).save(any(), any(), any(), any(), any());
        assertThrows(org.springframework.dao.DataAccessException.class,
                () -> accessService.validate(rfidRequest("A1B2C3D4")));
    }

    @Test
    void shouldRejectUnsafeThresholdConfiguration() {
        for (double threshold : new double[] {-1, 1, Double.NaN, Double.POSITIVE_INFINITY}) {
            ReflectionTestUtils.setField(accessService, "faceThreshold", threshold);
            assertThrows(IllegalStateException.class, accessService::validateConfiguration);
        }
        ReflectionTestUtils.setField(accessService, "faceThreshold", 0.5);
        accessService.validateConfiguration();
    }

    private AccessValidationRequestDTO faceRequest() {
        AccessValidationRequestDTO request = new AccessValidationRequestDTO();
        request.setCredentialType(CredentialType.FACE);
        request.setFaceEmbedding(vector(1, 0));
        return request;
    }

    private List<Float> vector(float first, float second) {
        List<Float> embedding = new ArrayList<>(Collections.nCopies(512, 0.0f));
        embedding.set(0, first);
        embedding.set(1, second);
        return embedding;
    }
}
