package com.dev.adaptface.dto.response;

public record RegistrationResponseDTO(Long userId, Long enrollmentId, String name, boolean hasFace) {
}
