package com.dev.adaptface.dto.response;

/**
 * Informa quais credenciais existem, sem expor os valores.
 */
public class CredentialResponseDTO {

    private Long id;

    private Long enrollmentId;

    private Boolean hasFace;

    private Boolean hasRfid;

    private Boolean hasCode;

    public CredentialResponseDTO() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getEnrollmentId() {
        return enrollmentId;
    }

    public void setEnrollmentId(Long enrollmentId) {
        this.enrollmentId = enrollmentId;
    }

    public Boolean getHasFace() {
        return hasFace;
    }

    public void setHasFace(Boolean hasFace) {
        this.hasFace = hasFace;
    }

    public Boolean getHasRfid() {
        return hasRfid;
    }

    public void setHasRfid(Boolean hasRfid) {
        this.hasRfid = hasRfid;
    }

    public Boolean getHasCode() {
        return hasCode;
    }

    public void setHasCode(Boolean hasCode) {
        this.hasCode = hasCode;
    }
}
