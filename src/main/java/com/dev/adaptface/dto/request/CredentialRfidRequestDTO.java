package com.dev.adaptface.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Cadastro da tag RFID.
 */
public class CredentialRfidRequestDTO {

    @NotBlank(message = "Tag RFID é obrigatória")
    @Size(max = 128, message = "Tag RFID deve possuir no máximo 128 caracteres")
    private String rfidTag;

    public CredentialRfidRequestDTO() {
    }

    public String getRfidTag() {
        return rfidTag;
    }

    public void setRfidTag(String rfidTag) {
        this.rfidTag = rfidTag;
    }
}
