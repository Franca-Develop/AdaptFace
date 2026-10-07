package com.dev.adaptface.dto.request;

import jakarta.validation.constraints.NotBlank;

/**
 * Cadastro da tag RFID.
 */
public class CredentialRfidRequestDTO {

    @NotBlank(message = "Tag RFID é obrigatória")
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
