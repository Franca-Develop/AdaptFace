package com.dev.adaptface.controller;

import com.dev.adaptface.dto.request.BootstrapRequestDTO;
import com.dev.adaptface.dto.request.LoginRequestDTO;
import com.dev.adaptface.dto.response.LoginResponseDTO;
import com.dev.adaptface.service.AuthService;
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
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponseDTO> login(@Valid @RequestBody LoginRequestDTO request) {
        return ResponseEntity.ok(authService.login(request));
    }

    @GetMapping("/setup-status")
    public ResponseEntity<java.util.Map<String, Boolean>> setupStatus() {
        return ResponseEntity.ok(java.util.Map.of("requiresSetup", authService.requiresSetup()));
    }

    @PostMapping("/bootstrap")
    public ResponseEntity<LoginResponseDTO> bootstrap(@RequestHeader("X-Bootstrap-Key") String bootstrapKey,
            @Valid @RequestBody BootstrapRequestDTO request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.bootstrap(bootstrapKey, request));
    }
}
