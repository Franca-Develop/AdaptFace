package com.dev.adaptface.service;

import com.dev.adaptface.dto.request.AccessValidationRequestDTO;
import com.dev.adaptface.dto.response.AccessLogResponseDTO;
import com.dev.adaptface.entity.AccessLog;
import com.dev.adaptface.entity.Enrollment;
import com.dev.adaptface.entity.enums.AccessResult;
import com.dev.adaptface.repository.AccessLogRepository;
import java.math.BigDecimal;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Grava e consulta o histórico de acessos (AccessLog).
 */
@Service
public class AccessLogService {

    private final AccessLogRepository accessLogRepository;

    public AccessLogService(AccessLogRepository accessLogRepository) {
        this.accessLogRepository = accessLogRepository;
    }

    @Transactional
    public AccessLog save(Enrollment enrollment, AccessValidationRequestDTO request, AccessResult result,
            BigDecimal calculatedDistance, String denialReason) {
        AccessLog accessLog = new AccessLog();
        accessLog.setEnrollment(enrollment);
        accessLog.setMachineModel(request.getMachineModel());
        accessLog.setCredentialType(request.getCredentialType());
        accessLog.setEntry(request.getEntry());
        accessLog.setResult(result);
        accessLog.setCalculatedDistance(calculatedDistance);
        accessLog.setDenialReason(denialReason);
        return accessLogRepository.saveAndFlush(accessLog);
    }

    @Transactional(readOnly = true)
    public Page<AccessLogResponseDTO> findAll(Long userId, Pageable pageable) {
        Pageable bounded = PageRequest.of(pageable.getPageNumber(), Math.min(pageable.getPageSize(), 100));
        Page<AccessLog> logs = userId == null
                ? accessLogRepository.findAllByOrderByLoggedAtDescIdDesc(bounded)
                : accessLogRepository.findByEnrollmentUserIdOrderByLoggedAtDescIdDesc(userId, bounded);
        return logs.map(this::toResponse);
    }

    private AccessLogResponseDTO toResponse(AccessLog accessLog) {
        AccessLogResponseDTO response = new AccessLogResponseDTO();
        response.setId(accessLog.getId());
        if (accessLog.getEnrollment() != null) {
            response.setEnrollmentId(accessLog.getEnrollment().getId());
            response.setUserName(accessLog.getEnrollment().getUser().getName());
        }
        response.setMachineModel(accessLog.getMachineModel());
        response.setCredentialType(accessLog.getCredentialType());
        response.setLoggedAt(accessLog.getLoggedAt());
        response.setEntry(accessLog.getEntry());
        response.setResult(accessLog.getResult());
        response.setCalculatedDistance(accessLog.getCalculatedDistance());
        response.setDenialReason(accessLog.getDenialReason());
        return response;
    }
}
