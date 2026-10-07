package com.dev.adaptface.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import com.dev.adaptface.entity.enums.AccessResult;
import com.dev.adaptface.entity.enums.CredentialType;

/**
 * Linha do histórico de acessos.
 */
public class AccessLogResponseDTO {

    private Long id;

    private Long enrollmentId;

    private String userName;

    private String machineModel;

    private CredentialType credentialType;

    private LocalDateTime loggedAt;

    private Boolean entry;

    private AccessResult result;

    private BigDecimal calculatedDistance;

    private String denialReason;

    public AccessLogResponseDTO() {
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

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getMachineModel() {
        return machineModel;
    }

    public void setMachineModel(String machineModel) {
        this.machineModel = machineModel;
    }

    public CredentialType getCredentialType() {
        return credentialType;
    }

    public void setCredentialType(CredentialType credentialType) {
        this.credentialType = credentialType;
    }

    public LocalDateTime getLoggedAt() {
        return loggedAt;
    }

    public void setLoggedAt(LocalDateTime loggedAt) {
        this.loggedAt = loggedAt;
    }

    public Boolean getEntry() {
        return entry;
    }

    public void setEntry(Boolean entry) {
        this.entry = entry;
    }

    public AccessResult getResult() {
        return result;
    }

    public void setResult(AccessResult result) {
        this.result = result;
    }

    public BigDecimal getCalculatedDistance() {
        return calculatedDistance;
    }

    public void setCalculatedDistance(BigDecimal calculatedDistance) {
        this.calculatedDistance = calculatedDistance;
    }

    public String getDenialReason() {
        return denialReason;
    }

    public void setDenialReason(String denialReason) {
        this.denialReason = denialReason;
    }
}
