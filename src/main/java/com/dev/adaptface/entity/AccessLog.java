package com.dev.adaptface.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import com.dev.adaptface.entity.enums.AccessResult;
import com.dev.adaptface.entity.enums.CredentialType;
import jakarta.persistence.*;

/**
 * Histórico de tentativas de acesso (antes chamada Maquina). enrollment é nulo quando o usuário não foi identificado. entry = true para entrada, false para saída.
 */
@Entity
@Table(name = "access_logs")
public class AccessLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "enrollment_id")
    private Enrollment enrollment;

    private String machineModel;

    @Enumerated(EnumType.STRING)
    private CredentialType credentialType;

    @Column(nullable = false)
    private LocalDateTime loggedAt;

    private Boolean entry;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AccessResult result;

    private BigDecimal calculatedDistance;

    private String denialReason;

    public AccessLog() {
    }

    @PrePersist
    void prePersist() {
        if (loggedAt == null) {
            loggedAt = LocalDateTime.now();
        }
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Enrollment getEnrollment() {
        return enrollment;
    }

    public void setEnrollment(Enrollment enrollment) {
        this.enrollment = enrollment;
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
