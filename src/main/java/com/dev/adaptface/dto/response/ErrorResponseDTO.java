package com.dev.adaptface.dto.response;

import java.time.LocalDateTime;
import com.dev.adaptface.entity.enums.AccessResult;

/**
 * Formato padrão de erro da API (usado pelo GlobalExceptionHandler).
 */
public class ErrorResponseDTO {

    private LocalDateTime timestamp;

    private Integer status;

    private String error;

    private String message;

    private String path;

    private AccessResult result;

    public ErrorResponseDTO() {
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    public String getError() {
        return error;
    }

    public void setError(String error) {
        this.error = error;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public AccessResult getResult() {
        return result;
    }

    public void setResult(AccessResult result) {
        this.result = result;
    }
}
