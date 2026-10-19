package com.dev.adaptface.service;

import com.dev.adaptface.dto.request.EnrollmentRequestDTO;
import com.dev.adaptface.dto.request.EnrollmentStatusRequestDTO;
import com.dev.adaptface.dto.response.EnrollmentResponseDTO;
import com.dev.adaptface.entity.Enrollment;
import com.dev.adaptface.entity.User;
import com.dev.adaptface.exceptions.BusinessException;
import com.dev.adaptface.exceptions.NotFoundException;
import com.dev.adaptface.repository.EnrollmentRepository;
import com.dev.adaptface.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Criação, ativação e desativação de matrículas.
 */
@Service
public class EnrollmentService {

    private final EnrollmentRepository enrollmentRepository;
    private final UserRepository userRepository;

    public EnrollmentService(EnrollmentRepository enrollmentRepository, UserRepository userRepository) {
        this.enrollmentRepository = enrollmentRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public EnrollmentResponseDTO create(EnrollmentRequestDTO request) {
        if (enrollmentRepository.findByUserId(request.getUserId()).isPresent()) {
            throw new BusinessException("Usuário já possui matrícula");
        }

        User user = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new NotFoundException("Usuário não encontrado"));
        Enrollment enrollment = new Enrollment();
        enrollment.setUser(user);
        return toResponse(enrollmentRepository.save(enrollment));
    }

    @Transactional(readOnly = true)
    public EnrollmentResponseDTO findById(Long id) {
        return toResponse(findEntity(id));
    }

    @Transactional
    public EnrollmentResponseDTO updateStatus(Long id, EnrollmentStatusRequestDTO request) {
        Enrollment enrollment = findEntity(id);
        enrollment.setActive(request.getActive());
        return toResponse(enrollmentRepository.save(enrollment));
    }

    private Enrollment findEntity(Long id) {
        return enrollmentRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Matrícula não encontrada"));
    }

    private EnrollmentResponseDTO toResponse(Enrollment enrollment) {
        EnrollmentResponseDTO response = new EnrollmentResponseDTO();
        response.setId(enrollment.getId());
        response.setUserId(enrollment.getUser().getId());
        response.setEnrollmentDate(enrollment.getEnrollmentDate());
        response.setActive(enrollment.getActive());
        return response;
    }
}
