package com.dev.adaptface.dto.request;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

/**
 * Cadastro facial: recebe o vetor de embedding já calculado pelo serviço Python.
 */
public class CredentialFaceRequestDTO {

    @NotNull(message = "O embedding facial é obrigatório")
    @NotEmpty(message = "O vetor de embedding não pode estar vazio")
    @Size(min = 512, max = 512)
    private List<Float> faceEmbedding;

    @Size(max = 64)
    private String faceModel;

    public CredentialFaceRequestDTO() {
    }

    public CredentialFaceRequestDTO(
			@NotNull(message = "O embedding facial é obrigatório") @NotEmpty(message = "O vetor de embedding não pode estar vazio") List<Float> faceEmbedding) {
		this.faceEmbedding = faceEmbedding;
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
}
