package com.dev.adaptface.service;

import com.dev.adaptface.dto.request.CredentialFaceRequestDTO;
import com.dev.adaptface.dto.request.CredentialRfidRequestDTO;
import com.dev.adaptface.dto.request.EnrollmentRequestDTO;
import com.dev.adaptface.dto.request.RegistrationRequestDTO;
import com.dev.adaptface.dto.response.RegistrationResponseDTO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.annotation.Isolation;

@Service
public class RegistrationService {

    private final UserService userService;
    private final EnrollmentService enrollmentService;
    private final CredentialService credentialService;

    public RegistrationService(UserService userService, EnrollmentService enrollmentService,
            CredentialService credentialService) {
        this.userService = userService;
        this.enrollmentService = enrollmentService;
        this.credentialService = credentialService;
    }

    @Transactional(isolation = Isolation.SERIALIZABLE)
    public RegistrationResponseDTO create(RegistrationRequestDTO request) {
        CredentialService.normalizeEmbedding(request.getFaceEmbedding());
        var user = userService.create(request.getUser());
        EnrollmentRequestDTO enrollmentRequest = new EnrollmentRequestDTO();
        enrollmentRequest.setUserId(user.getId());
        var enrollment = enrollmentService.create(enrollmentRequest);

        CredentialFaceRequestDTO faceRequest = new CredentialFaceRequestDTO();
        faceRequest.setFaceEmbedding(request.getFaceEmbedding());
        faceRequest.setFaceModel(request.getFaceModel());
        credentialService.saveFace(enrollment.getId(), faceRequest);
        if (request.getRfidTag() != null && !request.getRfidTag().isBlank()) {
            CredentialRfidRequestDTO rfidRequest = new CredentialRfidRequestDTO();
            rfidRequest.setRfidTag(request.getRfidTag());
            credentialService.saveRfid(enrollment.getId(), rfidRequest);
        }
        return new RegistrationResponseDTO(user.getId(), enrollment.getId(), user.getName(), true);
    }
}
