package com.dev.adaptface.controller;

import com.dev.adaptface.dto.request.AccessValidationRequestDTO;
import com.dev.adaptface.dto.response.AccessLogResponseDTO;
import com.dev.adaptface.dto.response.AccessValidationResponseDTO;
import com.dev.adaptface.service.AccessLogService;
import com.dev.adaptface.service.AccessService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Endpoints de validação de acesso e consulta do histórico.
 */
@RestController
@RequestMapping("/access")
public class AccessController {

    private final AccessService accessService;
    private final AccessLogService accessLogService;

    public AccessController(AccessService accessService, AccessLogService accessLogService) {
        this.accessService = accessService;
        this.accessLogService = accessLogService;
    }

    @PostMapping("/validate")
    @SecurityRequirement(name = "machineApiKey")
    public ResponseEntity<AccessValidationResponseDTO> validate(
            @Valid @RequestBody AccessValidationRequestDTO request) {
        return ResponseEntity.ok(accessService.validate(request));
    }

    @GetMapping("/logs")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasRole('EMPLOYEE')")
    public ResponseEntity<Page<AccessLogResponseDTO>> findLogs(
            @RequestParam(required = false) Long userId, Pageable pageable) {
        return ResponseEntity.ok(accessLogService.findAll(userId, pageable));
    }
}
