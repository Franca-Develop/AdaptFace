package com.dev.adaptface.controller;

import com.dev.adaptface.dto.request.CredentialCodeRequestDTO;
import com.dev.adaptface.dto.request.CredentialFaceRequestDTO;
import com.dev.adaptface.dto.request.CredentialRfidRequestDTO;
import com.dev.adaptface.dto.response.CredentialResponseDTO;
import com.dev.adaptface.service.CredentialService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Endpoints de credenciais.
 */
@RestController
@RequestMapping("/enrollments/{enrollmentId}/credentials")
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("hasRole('EMPLOYEE')")
public class CredentialController {

    private final CredentialService credentialService;

    public CredentialController(CredentialService credentialService) {
        this.credentialService = credentialService;
    }

    @PutMapping("/face")
    public ResponseEntity<CredentialResponseDTO> saveFace(@PathVariable Long enrollmentId,
            @Valid @RequestBody CredentialFaceRequestDTO request) {
        return ResponseEntity.ok(credentialService.saveFace(enrollmentId, request));
    }

    @PutMapping("/rfid")
    public ResponseEntity<CredentialResponseDTO> saveRfid(@PathVariable Long enrollmentId,
            @Valid @RequestBody CredentialRfidRequestDTO request) {
        return ResponseEntity.ok(credentialService.saveRfid(enrollmentId, request));
    }

    @PutMapping("/code")
    public ResponseEntity<CredentialResponseDTO> saveCode(@PathVariable Long enrollmentId,
            @Valid @RequestBody CredentialCodeRequestDTO request) {
        return ResponseEntity.ok(credentialService.saveCode(enrollmentId, request));
    }

    @GetMapping
    public ResponseEntity<CredentialResponseDTO> findByEnrollment(@PathVariable Long enrollmentId) {
        return ResponseEntity.ok(credentialService.findByEnrollment(enrollmentId));
    }
}
