package com.dev.adaptface.dto.response;

import java.time.LocalDate;

/**
 * Dados da matrícula.
 */
public class EnrollmentResponseDTO {

    private Long id;

    private Long userId;

    private LocalDate enrollmentDate;

    private Boolean active;

    public EnrollmentResponseDTO() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public LocalDate getEnrollmentDate() {
        return enrollmentDate;
    }

    public void setEnrollmentDate(LocalDate enrollmentDate) {
        this.enrollmentDate = enrollmentDate;
    }

    public Boolean getActive() {
        return active;
    }

    public void setActive(Boolean active) {
        this.active = active;
    }
}
