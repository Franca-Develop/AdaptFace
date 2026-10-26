package com.dev.adaptface.dto.request;

import java.util.List;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Cadastro transacional de pessoa, matrícula e credenciais")
public class RegistrationRequestDTO {

    @Valid
    @NotNull(message = "Dados pessoais são obrigatórios")
    private UserRequestDTO user;

    @NotNull(message = "Foto facial é obrigatória")
    @Size(min = 512, max = 512, message = "A credencial facial deve possuir 512 valores")
    @ArraySchema(minItems = 512, maxItems = 512, schema = @Schema(type = "number", format = "float"))
    private List<Float> faceEmbedding;

    @Size(max = 64)
    @Schema(example = "DEEPFACE_ARCFACE_OPENCV_V1")
    private String faceModel;

    @Size(max = 128, message = "A tag RFID deve possuir no máximo 128 caracteres")
    private String rfidTag;

    public UserRequestDTO getUser() {
        return user;
    }

    public void setUser(UserRequestDTO user) {
        this.user = user;
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
}
