package com.dev.adaptface.controller;

import com.dev.adaptface.dto.request.RegistrationRequestDTO;
import com.dev.adaptface.dto.response.RegistrationResponseDTO;
import com.dev.adaptface.service.RegistrationService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "Cadastros")
public class RegistrationController {

    private final RegistrationService registrationService;

    public RegistrationController(RegistrationService registrationService) {
        this.registrationService = registrationService;
    }

    @PostMapping
    @Operation(summary = "Realizar cadastro completo", description = "Cria pessoa, perfil, matrícula e credenciais facial/RFID na mesma transação.")
    @ApiResponses({@ApiResponse(responseCode = "201", description = "Cadastro concluído"),
            @ApiResponse(responseCode = "400", description = "Dados ou embedding inválidos"),
            @ApiResponse(responseCode = "401", description = "JWT ausente ou inválido"),
            @ApiResponse(responseCode = "403", description = "Conta sem perfil ADMIN"),
            @ApiResponse(responseCode = "409", description = "CPF, email ou credencial já cadastrados")})
    public ResponseEntity<RegistrationResponseDTO> create(@Valid @RequestBody RegistrationRequestDTO request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(registrationService.create(request));
    }
}
