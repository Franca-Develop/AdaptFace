package com.dev.adaptface.controller;

import com.dev.adaptface.dto.request.CredentialCodeRequestDTO;
import com.dev.adaptface.dto.request.CredentialFaceRequestDTO;
import com.dev.adaptface.dto.request.CredentialRfidRequestDTO;
import com.dev.adaptface.dto.response.CredentialResponseDTO;
import com.dev.adaptface.service.CredentialService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Credenciais")
public class CredentialController {

    private final CredentialService credentialService;

    public CredentialController(CredentialService credentialService) {
        this.credentialService = credentialService;
    }

    @PutMapping("/face")
    @Operation(summary = "Cadastrar ou substituir credencial facial", description = "Recebe um embedding ArcFace normalizado de exatamente 512 posições.")
    @ApiResponses({@ApiResponse(responseCode = "200", description = "Credencial facial salva"),
            @ApiResponse(responseCode = "400", description = "Embedding inválido"),
            @ApiResponse(responseCode = "404", description = "Matrícula não encontrada"),
            @ApiResponse(responseCode = "409", description = "Face já vinculada a outra matrícula")})
    public ResponseEntity<CredentialResponseDTO> saveFace(@PathVariable Long enrollmentId,
            @Valid @RequestBody CredentialFaceRequestDTO request) {
        return ResponseEntity.ok(credentialService.saveFace(enrollmentId, request));
    }

    @PutMapping("/rfid")
    @Operation(summary = "Cadastrar ou substituir credencial RFID")
    @ApiResponses({@ApiResponse(responseCode = "200", description = "RFID salvo"),
            @ApiResponse(responseCode = "404", description = "Matrícula não encontrada"),
            @ApiResponse(responseCode = "409", description = "RFID já utilizado")})
    public ResponseEntity<CredentialResponseDTO> saveRfid(@PathVariable Long enrollmentId,
            @Valid @RequestBody CredentialRfidRequestDTO request) {
        return ResponseEntity.ok(credentialService.saveRfid(enrollmentId, request));
    }

    @PutMapping("/code")
    @Operation(summary = "Cadastrar ou substituir código de acesso")
    @ApiResponses({@ApiResponse(responseCode = "200", description = "Código salvo"),
            @ApiResponse(responseCode = "404", description = "Matrícula não encontrada"),
            @ApiResponse(responseCode = "409", description = "Código já utilizado")})
    public ResponseEntity<CredentialResponseDTO> saveCode(@PathVariable Long enrollmentId,
            @Valid @RequestBody CredentialCodeRequestDTO request) {
        return ResponseEntity.ok(credentialService.saveCode(enrollmentId, request));
    }

    @GetMapping
    @Operation(summary = "Consultar credenciais da matrícula", description = "Retorna metadados sem expor embeddings, RFID ou código em texto puro.")
    @ApiResponses({@ApiResponse(responseCode = "200", description = "Credenciais encontradas"),
            @ApiResponse(responseCode = "404", description = "Matrícula ou credencial não encontrada")})
    public ResponseEntity<CredentialResponseDTO> findByEnrollment(@PathVariable Long enrollmentId) {
        return ResponseEntity.ok(credentialService.findByEnrollment(enrollmentId));
    }
}
