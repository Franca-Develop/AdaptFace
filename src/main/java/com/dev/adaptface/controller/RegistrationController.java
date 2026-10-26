package com.dev.adaptface.controller;

import com.dev.adaptface.dto.request.RegistrationRequestDTO;
import com.dev.adaptface.dto.response.RegistrationResponseDTO;
import com.dev.adaptface.service.RegistrationService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/registrations")
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("hasRole('ADMIN')")
public class RegistrationController {

    private final RegistrationService registrationService;

    public RegistrationController(RegistrationService registrationService) {
        this.registrationService = registrationService;
    }

    @PostMapping
    public ResponseEntity<RegistrationResponseDTO> create(@Valid @RequestBody RegistrationRequestDTO request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(registrationService.create(request));
    }
}
