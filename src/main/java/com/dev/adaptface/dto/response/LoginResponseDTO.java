package com.dev.adaptface.dto.response;

/**
 * Saída do login.
 */
public class LoginResponseDTO {

    private String token;

    public LoginResponseDTO() {
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }
}
