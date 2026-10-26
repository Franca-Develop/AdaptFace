package com.dev.adaptface.controller;

import com.dev.adaptface.dto.request.InstitutionRequestDTO;
import com.dev.adaptface.dto.response.InstitutionResponseDTO;
import com.dev.adaptface.service.InstitutionService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
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
public class InstitutionController {

    private final InstitutionService institutionService;

    public InstitutionController(InstitutionService institutionService) {
        this.institutionService = institutionService;
    }

    @PostMapping
    public ResponseEntity<InstitutionResponseDTO> create(@Valid @RequestBody InstitutionRequestDTO request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(institutionService.create(request));
    }

    @GetMapping
    public ResponseEntity<List<InstitutionResponseDTO>> findAll() {
        return ResponseEntity.ok(institutionService.findAll());
    }
}
