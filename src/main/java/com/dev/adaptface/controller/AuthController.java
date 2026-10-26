package com.dev.adaptface.controller;

import com.dev.adaptface.dto.request.BootstrapRequestDTO;
import com.dev.adaptface.dto.request.LoginRequestDTO;
import com.dev.adaptface.dto.response.LoginResponseDTO;
import com.dev.adaptface.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

/**
 * Endpoints de autenticação (login).
 */
@RestController
@RequestMapping("/auth")
@Tag(name = "Autenticação")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    @Operation(summary = "Autenticar administrador", description = "Valida email e senha e devolve um JWT com o perfil da conta.")
    @ApiResponses({@ApiResponse(responseCode = "200", description = "Login realizado"),
            @ApiResponse(responseCode = "400", description = "Dados inválidos"),
            @ApiResponse(responseCode = "401", description = "Credenciais inválidas")})
    public ResponseEntity<LoginResponseDTO> login(@Valid @RequestBody LoginRequestDTO request) {
        return ResponseEntity.ok(authService.login(request));
    }

    @GetMapping("/setup-status")
    @Operation(summary = "Consultar configuração inicial", description = "Informa se ainda é necessário criar o primeiro administrador.")
    @ApiResponse(responseCode = "200", description = "Situação retornada")
    public ResponseEntity<java.util.Map<String, Boolean>> setupStatus() {
        return ResponseEntity.ok(java.util.Map.of("requiresSetup", authService.requiresSetup()));
    }

    @PostMapping("/bootstrap")
    @Operation(summary = "Criar o primeiro administrador", description = "Uso único. Exige X-Bootstrap-Key e deixa de funcionar após existir um ADMIN.")
    @ApiResponses({@ApiResponse(responseCode = "201", description = "Administrador e instituição criados"),
            @ApiResponse(responseCode = "400", description = "Dados inválidos ou configuração já concluída"),
            @ApiResponse(responseCode = "403", description = "Chave de inicialização inválida"),
            @ApiResponse(responseCode = "409", description = "CPF ou email já cadastrado")})
    public ResponseEntity<LoginResponseDTO> bootstrap(@RequestHeader("X-Bootstrap-Key") String bootstrapKey,
            @Valid @RequestBody BootstrapRequestDTO request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.bootstrap(bootstrapKey, request));
    }
}
