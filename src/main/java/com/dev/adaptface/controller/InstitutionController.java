package com.dev.adaptface.controller;

import com.dev.adaptface.dto.request.InstitutionRequestDTO;
import com.dev.adaptface.dto.response.InstitutionResponseDTO;
import com.dev.adaptface.service.InstitutionService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/institutions")
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Instituições")
public class InstitutionController {

    private final InstitutionService institutionService;

    public InstitutionController(InstitutionService institutionService) {
        this.institutionService = institutionService;
    }

    @PostMapping
    @Operation(summary = "Criar instituição")
    @ApiResponses({@ApiResponse(responseCode = "201", description = "Instituição criada"),
            @ApiResponse(responseCode = "400", description = "Nome inválido"),
            @ApiResponse(responseCode = "409", description = "Instituição já cadastrada")})
    public ResponseEntity<InstitutionResponseDTO> create(@Valid @RequestBody InstitutionRequestDTO request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(institutionService.create(request));
    }

    @GetMapping
    @Operation(summary = "Listar instituições")
    @ApiResponse(responseCode = "200", description = "Instituições cadastradas")
    public ResponseEntity<List<InstitutionResponseDTO>> findAll() {
        return ResponseEntity.ok(institutionService.findAll());
    }
}
