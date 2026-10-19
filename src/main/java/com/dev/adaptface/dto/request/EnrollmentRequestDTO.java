package com.dev.adaptface.dto.request;

import jakarta.validation.constraints.NotNull;

public class EnrollmentRequestDTO {

    @NotNull(message = "ID do usuário é obrigatório")
    private Long userId;

    public EnrollmentRequestDTO() {
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }
}
