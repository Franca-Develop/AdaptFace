package com.dev.adaptface.dto.request;

import jakarta.validation.constraints.NotNull;

public class EnrollmentStatusRequestDTO {

    @NotNull(message = "Status é obrigatório")
    private Boolean active;

    public EnrollmentStatusRequestDTO() {
    }

    public Boolean getActive() {
        return active;
    }

    public void setActive(Boolean active) {
        this.active = active;
    }
}
