package com.dev.adaptface.dto.request;

import com.dev.adaptface.entity.enums.CredentialType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

public class AccessValidationRequestDTO {

    @NotNull(message = "Tipo de credencial é obrigatório")
    private CredentialType credentialType;

    @Size(max = 512)
    private List<Float> faceEmbedding;

    @Size(max = 64)
    private String faceModel;

    @Size(max = 128)
    private String rfidTag;

    @Size(max = 64)
    private String code;

    @NotBlank(message = "Modelo da máquina é obrigatório")
    @Size(max = 255)
    private String machineModel;

    @NotNull(message = "O sentido do acesso é obrigatório")
    private Boolean entry;

    public AccessValidationRequestDTO() {
    }

    public CredentialType getCredentialType() {
        return credentialType;
    }

    public void setCredentialType(CredentialType credentialType) {
        this.credentialType = credentialType;
    }

    public List<Float> getFaceEmbedding() {
        return faceEmbedding;
    }

    public void setFaceEmbedding(List<Float> faceEmbedding) {
        this.faceEmbedding = faceEmbedding;
    }

    public String getFaceModel() {
        return faceModel;
    }

    public void setFaceModel(String faceModel) {
        this.faceModel = faceModel;
    }

    public String getRfidTag() {
        return rfidTag;
    }

    public void setRfidTag(String rfidTag) {
        this.rfidTag = rfidTag;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getMachineModel() {
        return machineModel;
    }

    public void setMachineModel(String machineModel) {
        this.machineModel = machineModel;
    }

    public Boolean getEntry() {
        return entry;
    }

    public void setEntry(Boolean entry) {
        this.entry = entry;
    }
}
