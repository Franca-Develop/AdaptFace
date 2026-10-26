package com.dev.adaptface.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Entrada do login.
 */
@Schema(description = "Credenciais da conta administrativa")
public class LoginRequestDTO {

    @NotBlank(message = "Email é obrigatório")
    @Email(message = "Email inválido")
    @Size(max = 255)
    @Schema(example = "admin@senai.br")
    private String email;

    @NotBlank(message = "Senha é obrigatória")
    @Size(max = 72)
    @Schema(example = "senha-segura", format = "password")
    private String password;

    public LoginRequestDTO() {
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}
