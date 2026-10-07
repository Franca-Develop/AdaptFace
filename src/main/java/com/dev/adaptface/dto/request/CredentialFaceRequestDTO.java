package com.dev.adaptface.dto.request;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;

/**
 * Cadastro facial: recebe o vetor de embedding já calculado pelo serviço Python.
 */
public class CredentialFaceRequestDTO {

    @NotNull(message = "O embedding facial é obrigatório")
    @NotEmpty(message = "O vetor de embedding não pode estar vazio")
    private List<Float> faceEmbedding;

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
}