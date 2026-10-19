package com.dev.adaptface.service;

import com.dev.adaptface.dto.request.InstitutionRequestDTO;
import com.dev.adaptface.dto.response.InstitutionResponseDTO;
import com.dev.adaptface.entity.Institution;
import com.dev.adaptface.repository.InstitutionRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class InstitutionService {

    private final InstitutionRepository institutionRepository;

    public InstitutionService(InstitutionRepository institutionRepository) {
        this.institutionRepository = institutionRepository;
    }

    @Transactional
    public InstitutionResponseDTO create(InstitutionRequestDTO request) {
        Institution institution = new Institution();
        institution.setName(request.getName().trim());
        return toResponse(institutionRepository.save(institution));
    }

    @Transactional(readOnly = true)
    public List<InstitutionResponseDTO> findAll() {
        return institutionRepository.findAll().stream().map(this::toResponse).toList();
    }

    @Transactional
    public InstitutionResponseDTO findOrCreate(InstitutionRequestDTO request) {
        return institutionRepository.findFirstByNameIgnoreCase(request.getName().trim())
                .map(this::toResponse).orElseGet(() -> create(request));
    }

    private InstitutionResponseDTO toResponse(Institution institution) {
        InstitutionResponseDTO response = new InstitutionResponseDTO();
        response.setId(institution.getId());
        response.setName(institution.getName());
        return response;
    }
}
