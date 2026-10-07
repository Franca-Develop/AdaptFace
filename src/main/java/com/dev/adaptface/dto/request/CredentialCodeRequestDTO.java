package com.dev.adaptface.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Cadastro do código de acesso.
 */
public class CredentialCodeRequestDTO {

    @NotBlank(message = "Código é obrigatório")
    @Size(min = 4, message = "O código deve possuir no mínimo 4 caracteres")
    private String code;

    public CredentialCodeRequestDTO() {
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }
}
