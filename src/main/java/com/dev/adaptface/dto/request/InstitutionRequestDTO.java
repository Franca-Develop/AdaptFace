package com.dev.adaptface.dto.request;

import jakarta.validation.constraints.NotBlank;

public class InstitutionRequestDTO {

    @NotBlank(message = "Nome da instituição é obrigatório")
    private String name;

    public InstitutionRequestDTO() {
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
