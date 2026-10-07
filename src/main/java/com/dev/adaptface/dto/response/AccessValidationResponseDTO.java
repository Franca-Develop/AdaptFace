package com.dev.adaptface.dto.response;

import com.dev.adaptface.entity.enums.AccessResult;

/**
 * Resposta enviada à máquina após validar um acesso.
 */
public class AccessValidationResponseDTO {

    private AccessResult result;

    private String name;

    private String reason;

    public AccessValidationResponseDTO() {
    }

    public AccessResult getResult() {
        return result;
    }

    public void setResult(AccessResult result) {
        this.result = result;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }
}
