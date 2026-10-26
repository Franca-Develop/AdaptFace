package com.dev.adaptface.controller;

import com.dev.adaptface.dto.request.EnrollmentRequestDTO;
import com.dev.adaptface.dto.request.EnrollmentStatusRequestDTO;
import com.dev.adaptface.dto.response.EnrollmentResponseDTO;
import com.dev.adaptface.service.EnrollmentService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Endpoints de matrículas.
 */
@RestController
@RequestMapping("/enrollments")
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Matrículas")
public class EnrollmentController {

    private final EnrollmentService enrollmentService;

    public EnrollmentController(EnrollmentService enrollmentService) {
        this.enrollmentService = enrollmentService;
    }

    @PostMapping
    @Operation(summary = "Criar matrícula", description = "Vincula um usuário ainda não matriculado à sua instituição.")
    @ApiResponses({@ApiResponse(responseCode = "201", description = "Matrícula criada"),
            @ApiResponse(responseCode = "400", description = "Usuário inválido"),
            @ApiResponse(responseCode = "404", description = "Usuário não encontrado"),
            @ApiResponse(responseCode = "409", description = "Usuário já matriculado")})
    public ResponseEntity<EnrollmentResponseDTO> create(@Valid @RequestBody EnrollmentRequestDTO request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(enrollmentService.create(request));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Consultar matrícula")
    @ApiResponses({@ApiResponse(responseCode = "200", description = "Matrícula encontrada"),
            @ApiResponse(responseCode = "404", description = "Matrícula não encontrada")})
    public ResponseEntity<EnrollmentResponseDTO> findById(@PathVariable Long id) {
        return ResponseEntity.ok(enrollmentService.findById(id));
    }

    @PatchMapping("/{id}/status")
    @Operation(summary = "Ativar ou desativar matrícula", description = "Uma matrícula inativa sempre impede o acesso, sem apagar credenciais.")
    @ApiResponses({@ApiResponse(responseCode = "200", description = "Situação atualizada"),
            @ApiResponse(responseCode = "404", description = "Matrícula não encontrada")})
    public ResponseEntity<EnrollmentResponseDTO> updateStatus(@PathVariable Long id,
            @Valid @RequestBody EnrollmentStatusRequestDTO request) {
        return ResponseEntity.ok(enrollmentService.updateStatus(id, request));
    }
}
