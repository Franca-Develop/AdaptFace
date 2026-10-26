package com.dev.adaptface.dto.response;

import com.dev.adaptface.entities.enums.UserType;

/**
 * Saída do login.
 */
public class LoginResponseDTO {

    private String token;
    private UserType userType;

    public LoginResponseDTO() {
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public UserType getUserType() {
        return userType;
    }

    public void setUserType(UserType userType) {
        this.userType = userType;
    }
}
