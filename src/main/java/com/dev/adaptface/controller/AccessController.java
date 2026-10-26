package com.dev.adaptface.controller;

import com.dev.adaptface.dto.request.AccessValidationRequestDTO;
import com.dev.adaptface.dto.response.AccessLogResponseDTO;
import com.dev.adaptface.dto.response.AccessValidationResponseDTO;
import com.dev.adaptface.service.AccessLogService;
import com.dev.adaptface.service.AccessService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "Acesso")
public class AccessController {

    private final AccessService accessService;
    private final AccessLogService accessLogService;

    public AccessController(AccessService accessService, AccessLogService accessLogService) {
        this.accessService = accessService;
        this.accessLogService = accessLogService;
    }

    @PostMapping("/validate")
    @SecurityRequirement(name = "machineApiKey")
    @Operation(summary = "Validar uma tentativa de acesso", description = "Recebe exatamente uma credencial, registra a tentativa e retorna GRANTED ou DENIED.")
    @ApiResponses({@ApiResponse(responseCode = "200", description = "Decisão registrada"),
            @ApiResponse(responseCode = "400", description = "Credencial ou corpo inválido"),
            @ApiResponse(responseCode = "401", description = "Chave da máquina ausente ou inválida"),
            @ApiResponse(responseCode = "413", description = "Corpo excede 64 KiB"),
            @ApiResponse(responseCode = "503", description = "Persistência indisponível; acesso deve ser negado")})
    public ResponseEntity<AccessValidationResponseDTO> validate(
            @Valid @RequestBody AccessValidationRequestDTO request) {
        return ResponseEntity.ok(accessService.validate(request));
    }

    @GetMapping("/logs")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Listar histórico de acesso", description = "Lista paginada, opcionalmente filtrada por usuário, ordenada do registro mais recente.")
    @ApiResponses({@ApiResponse(responseCode = "200", description = "Página de registros"),
            @ApiResponse(responseCode = "401", description = "JWT ausente ou inválido"),
            @ApiResponse(responseCode = "403", description = "Conta sem perfil ADMIN")})
    public ResponseEntity<Page<AccessLogResponseDTO>> findLogs(
            @RequestParam(required = false) Long userId, Pageable pageable) {
        return ResponseEntity.ok(accessLogService.findAll(userId, pageable));
    }
}
