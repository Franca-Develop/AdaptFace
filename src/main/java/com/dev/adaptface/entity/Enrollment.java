package com.dev.adaptface.entity;

import java.time.LocalDate;
import jakarta.persistence.*;

/**
 * Matrícula do usuário no controle de acesso. Se active = false, o acesso é negado. É a ponte entre User, Credential e AccessLog.
 */
@Entity
@Table(name = "enrollments")
public class Enrollment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Column(nullable = false)
    private LocalDate enrollmentDate;

    @Column(nullable = false)
    private Boolean active;

    @OneToOne(mappedBy = "enrollment")
    private Credential credential;

    public Enrollment() {
    }

    @PrePersist
    void prePersist() {
        if (enrollmentDate == null) {
            enrollmentDate = LocalDate.now();
        }
        if (active == null) {
            active = true;
        }
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
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

    public Credential getCredential() {
        return credential;
    }

    public void setCredential(Credential credential) {
        this.credential = credential;
    }
}
