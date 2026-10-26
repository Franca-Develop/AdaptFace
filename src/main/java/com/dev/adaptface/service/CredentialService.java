package com.dev.adaptface.service;

import com.dev.adaptface.dto.request.CredentialCodeRequestDTO;
import com.dev.adaptface.dto.request.CredentialFaceRequestDTO;
import com.dev.adaptface.dto.request.CredentialRfidRequestDTO;
import com.dev.adaptface.dto.response.CredentialResponseDTO;
import com.dev.adaptface.entities.Credential;
import com.dev.adaptface.entities.Enrollment;
import com.dev.adaptface.exceptions.BusinessException;
import com.dev.adaptface.exceptions.NotFoundException;
import com.dev.adaptface.repository.CredentialRepository;
import com.dev.adaptface.repository.EnrollmentRepository;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.annotation.Isolation;

/**
 * Cadastro de credenciais (face, RFID, código) de uma matrícula.
 */
@Service
public class CredentialService {

    public static final String LEGACY_FACE_MODEL = "INSIGHTFACE_BUFFALO_L";
    public static final String DEEPFACE_FACE_MODEL = "DEEPFACE_ARCFACE_OPENCV_V1";

    private final CredentialRepository credentialRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final PasswordEncoder passwordEncoder;

    public CredentialService(CredentialRepository credentialRepository, EnrollmentRepository enrollmentRepository,
            PasswordEncoder passwordEncoder) {
        this.credentialRepository = credentialRepository;
        this.enrollmentRepository = enrollmentRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional(isolation = Isolation.SERIALIZABLE)
    public CredentialResponseDTO saveFace(Long enrollmentId, CredentialFaceRequestDTO request) {
        List<Float> embedding = normalizeEmbedding(request.getFaceEmbedding());
        String faceModel = normalizeFaceModel(request.getFaceModel());
        validateFaceDimensions(embedding, faceModel);
        boolean duplicated = credentialRepository.findFacialCredentials().stream()
                .filter(item -> !item.getEnrollment().getId().equals(enrollmentId))
                .filter(item -> faceModel.equals(item.getFaceModel() == null ? LEGACY_FACE_MODEL : item.getFaceModel()))
                .filter(item -> isValidEmbedding(item.getFaceEmbedding()))
                .anyMatch(item -> sameEmbedding(embedding, normalizeEmbedding(item.getFaceEmbedding())));
        if (duplicated) {
            throw new BusinessException("Credencial facial já vinculada a outro cadastro");
        }
        Credential credential = findOrCreate(enrollmentId);
        credential.setFaceEmbedding(embedding);
        credential.setFaceModel(faceModel);
        return toResponse(credentialRepository.save(credential));
    }

    @Transactional
    public CredentialResponseDTO saveRfid(Long enrollmentId, CredentialRfidRequestDTO request) {
        String rfidTag = normalizeRfid(request.getRfidTag());
        if (rfidTag.isBlank()) {
            throw new BusinessException("Tag RFID inválida");
        }
        credentialRepository.findByRfidTag(rfidTag)
                .filter(credential -> !credential.getEnrollment().getId().equals(enrollmentId))
                .ifPresent(credential -> {
                    throw new BusinessException("Tag RFID já cadastrada");
                });

        Credential credential = findOrCreate(enrollmentId);
        credential.setRfidTag(rfidTag);
        return toResponse(credentialRepository.save(credential));
    }

    @Transactional(isolation = Isolation.SERIALIZABLE)
    public CredentialResponseDTO saveCode(Long enrollmentId, CredentialCodeRequestDTO request) {
        String code = request.getCode();
        if (code == null || code.isBlank() || code.length() < 4 || code.length() > 64
                || code.getBytes(java.nio.charset.StandardCharsets.UTF_8).length > 72) {
            throw new BusinessException("Código de acesso inválido");
        }
        boolean duplicated = credentialRepository.findCodeCredentials().stream()
                .filter(item -> !item.getEnrollment().getId().equals(enrollmentId))
                .anyMatch(item -> passwordEncoder.matches(code, item.getAccessCode()));
        if (duplicated) {
            throw new BusinessException("Código de acesso já vinculado a outro cadastro");
        }
        Credential credential = findOrCreate(enrollmentId);
        credential.setAccessCode(passwordEncoder.encode(code));
        return toResponse(credentialRepository.save(credential));
    }

    @Transactional(readOnly = true)
    public CredentialResponseDTO findByEnrollment(Long enrollmentId) {
        Credential credential = credentialRepository.findByEnrollmentId(enrollmentId)
                .orElseThrow(() -> new NotFoundException("Credenciais não encontradas"));
        return toResponse(credential);
    }

    private Credential findOrCreate(Long enrollmentId) {
        return credentialRepository.findByEnrollmentId(enrollmentId).orElseGet(() -> {
            Enrollment enrollment = enrollmentRepository.findById(enrollmentId)
                    .orElseThrow(() -> new NotFoundException("Matrícula não encontrada"));
            Credential credential = new Credential();
            credential.setEnrollment(enrollment);
            return credential;
        });
    }

    private CredentialResponseDTO toResponse(Credential credential) {
        CredentialResponseDTO response = new CredentialResponseDTO();
        response.setId(credential.getId());
        response.setEnrollmentId(credential.getEnrollment().getId());
        response.setHasFace(credential.getFaceEmbedding() != null && !credential.getFaceEmbedding().isEmpty());
        response.setFaceModel(Boolean.TRUE.equals(response.getHasFace())
                ? credential.getFaceModel() == null ? LEGACY_FACE_MODEL : credential.getFaceModel() : null);
        response.setHasRfid(credential.getRfidTag() != null && !credential.getRfidTag().isBlank());
        response.setHasCode(credential.getAccessCode() != null && !credential.getAccessCode().isBlank());
        return response;
    }

    public static String normalizeRfid(String rfidTag) {
        return rfidTag.trim().replaceAll("[\\s:-]", "").toUpperCase(Locale.ROOT);
    }

    public static List<Float> normalizeEmbedding(List<Float> embedding) {
        if (embedding == null || embedding.isEmpty()
                || embedding.stream().anyMatch(value -> value == null || !Float.isFinite(value))) {
            throw new BusinessException("Embedding facial inválido");
        }

        double squaredNorm = embedding.stream()
                .mapToDouble(value -> (double) value * value)
                .sum();
        if (squaredNorm == 0) {
            throw new BusinessException("Embedding facial inválido");
        }

        double norm = Math.sqrt(squaredNorm);
        List<Float> normalized = new ArrayList<>(embedding.size());
        embedding.forEach(value -> normalized.add((float) (value / norm)));
        return normalized;
    }

    public static String normalizeFaceModel(String model) {
        if (model == null || model.isBlank()) {
            return LEGACY_FACE_MODEL;
        }
        if (!LEGACY_FACE_MODEL.equals(model) && !DEEPFACE_FACE_MODEL.equals(model)) {
            throw new BusinessException("Modelo facial não suportado");
        }
        return model;
    }

    public static void validateFaceDimensions(List<Float> embedding, String model) {
        if (embedding.size() != 512) {
            throw new BusinessException("A credencial facial deve possuir 512 valores");
        }
    }

    public static boolean isValidEmbedding(List<Float> embedding) {
        return embedding != null && embedding.size() == 512
                && embedding.stream().allMatch(value -> value != null && Float.isFinite(value))
                && embedding.stream().anyMatch(value -> value != 0);
    }

    private boolean sameEmbedding(List<Float> first, List<Float> second) {
        for (int index = 0; index < first.size(); index++) {
            if (Math.abs(first.get(index) - second.get(index)) > 0.000001) {
                return false;
            }
        }
        return true;
    }
}
