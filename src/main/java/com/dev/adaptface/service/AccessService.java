package com.dev.adaptface.service;

import com.dev.adaptface.dto.request.AccessValidationRequestDTO;
import com.dev.adaptface.dto.response.AccessValidationResponseDTO;
import com.dev.adaptface.entities.Credential;
import com.dev.adaptface.entities.Enrollment;
import com.dev.adaptface.entities.enums.AccessResult;
import com.dev.adaptface.exceptions.BusinessException;
import com.dev.adaptface.repository.CredentialRepository;
import jakarta.annotation.PostConstruct;
import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Regra de negócio: valida uma tentativa de acesso e decide GRANTED/DENIED.
 */
@Service
public class AccessService {

    private final CredentialRepository credentialRepository;
    private final AccessLogService accessLogService;
    private final PasswordEncoder passwordEncoder;

    @Value("${access.face.threshold:0.5}")
    private double faceThreshold;

    public AccessService(CredentialRepository credentialRepository, AccessLogService accessLogService,
            PasswordEncoder passwordEncoder) {
        this.credentialRepository = credentialRepository;
        this.accessLogService = accessLogService;
        this.passwordEncoder = passwordEncoder;
    }

    @PostConstruct
    void validateConfiguration() {
        if (!Double.isFinite(faceThreshold) || faceThreshold < 0 || faceThreshold >= 1) {
            throw new IllegalStateException("FACE_DISTANCE_THRESHOLD deve estar entre 0 (inclusivo) e 1 (exclusivo)");
        }
    }

    @Transactional
    public AccessValidationResponseDTO validate(AccessValidationRequestDTO request) {
        Match match;
        try {
            match = switch (request.getCredentialType()) {
                case RFID -> findByRfid(request.getRfidTag());
                case CODE -> findByCode(request.getCode());
                case FACE -> findByFace(request.getFaceEmbedding(), request.getFaceModel());
            };
        } catch (BusinessException exception) {
            return deny(request, null, null, exception.getMessage());
        }

        if (match.credential() == null) {
            return deny(request, null, match.distance(), match.reason());
        }

        Enrollment enrollment = match.credential().getEnrollment();
        if (enrollment == null || enrollment.getId() == null || enrollment.getUser() == null || enrollment.getUser().getId() == null
                || enrollment.getUser().getName() == null || enrollment.getUser().getName().isBlank()) {
            return deny(request, null, match.distance(), "Vínculo de identidade inválido");
        }
        if (!Boolean.TRUE.equals(enrollment.getActive())) {
            return deny(request, enrollment, match.distance(), "Matrícula inativa");
        }

        accessLogService.save(enrollment, request, AccessResult.GRANTED, match.distance(), null);
        AccessValidationResponseDTO response = new AccessValidationResponseDTO();
        response.setResult(AccessResult.GRANTED);
        response.setName(enrollment.getUser().getName());
        return response;
    }

    private Match findByRfid(String rfidTag) {
        if (rfidTag == null || rfidTag.isBlank() || rfidTag.length() > 128) {
            throw new BusinessException("Tag RFID é obrigatória");
        }
        String normalized = CredentialService.normalizeRfid(rfidTag);
        if (normalized.isEmpty()) {
            throw new BusinessException("Tag RFID inválida");
        }
        return new Match(credentialRepository.findByRfidTag(normalized).orElse(null), null);
    }

    private Match findByCode(String code) {
        if (code == null || code.isBlank() || code.length() < 4 || code.length() > 64
                || code.getBytes(java.nio.charset.StandardCharsets.UTF_8).length > 72) {
            throw new BusinessException("Código é obrigatório");
        }
        List<Credential> matches = credentialRepository.findCodeCredentials().stream()
                .filter(item -> passwordEncoder.matches(code, item.getAccessCode()))
                .limit(2).toList();
        if (matches.size() > 1) {
            return new Match(null, null, "Código de acesso ambíguo");
        }
        return new Match(matches.isEmpty() ? null : matches.get(0), null);
    }

    private Match findByFace(List<Float> faceEmbedding, String faceModel) {
        List<Float> normalizedEmbedding = CredentialService.normalizeEmbedding(faceEmbedding);
        String model = CredentialService.normalizeFaceModel(faceModel);
        CredentialService.validateFaceDimensions(normalizedEmbedding, model);

        List<Match> candidates = credentialRepository.findFacialCredentials().stream()
                .filter(credential -> CredentialService.isValidEmbedding(credential.getFaceEmbedding()))
                .filter(credential -> model.equals(credential.getFaceModel() == null
                        ? CredentialService.LEGACY_FACE_MODEL : credential.getFaceModel()))
                .map(credential -> new Match(credential,
                        cosineDistance(normalizedEmbedding, credential.getFaceEmbedding())))
                .sorted(Comparator.comparing(Match::distance)).limit(2).toList();
        Match nearest = candidates.isEmpty() ? new Match(null, null) : candidates.get(0);

        if (nearest.distance() == null || nearest.distance().doubleValue() > faceThreshold) {
            return new Match(null, nearest.distance());
        }
        if (candidates.size() > 1 && nearest.distance().subtract(candidates.get(1).distance()).abs()
                .compareTo(BigDecimal.valueOf(0.000001)) <= 0) {
            return new Match(null, nearest.distance(), "Correspondência facial ambígua");
        }
        return nearest;
    }

    private BigDecimal cosineDistance(List<Float> first, List<Float> second) {
        double dotProduct = 0;
        double firstNorm = 0;
        double secondNorm = 0;
        for (int index = 0; index < first.size(); index++) {
            double firstValue = first.get(index);
            double secondValue = second.get(index);
            dotProduct += firstValue * secondValue;
            firstNorm += firstValue * firstValue;
            secondNorm += secondValue * secondValue;
        }
        if (firstNorm == 0 || secondNorm == 0) {
            return BigDecimal.ONE;
        }
        double similarity = dotProduct / (Math.sqrt(firstNorm) * Math.sqrt(secondNorm));
        return BigDecimal.valueOf(1 - Math.max(-1, Math.min(1, similarity)));
    }

    private AccessValidationResponseDTO deny(AccessValidationRequestDTO request, Enrollment enrollment,
            BigDecimal distance, String reason) {
        accessLogService.save(enrollment, request, AccessResult.DENIED, distance, reason);
        AccessValidationResponseDTO response = new AccessValidationResponseDTO();
        response.setResult(AccessResult.DENIED);
        response.setReason(reason);
        return response;
    }

    private record Match(Credential credential, BigDecimal distance, String reason) {
        private Match(Credential credential, BigDecimal distance) {
            this(credential, distance, "Credencial não reconhecida");
        }
    }
}
