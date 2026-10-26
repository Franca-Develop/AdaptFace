package com.dev.adaptface.entities;

import jakarta.persistence.*;

/**
 * Perfil de aluno. Compartilha o id com User (@MapsId). registrationNumber é o RM.
 */
@Entity
@Table(name = "students")
public class Student {

    @Id
    private Long id;

    @OneToOne(optional = false, fetch = FetchType.LAZY)
    @MapsId
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "registration_number")
    private Integer registrationNumber;

    private String period;

    private String course;

    public Student() {
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

    public Integer getRegistrationNumber() {
        return registrationNumber;
    }

    public void setRegistrationNumber(Integer registrationNumber) {
        this.registrationNumber = registrationNumber;
    }

    public String getPeriod() {
        return period;
    }

    public void setPeriod(String period) {
        this.period = period;
    }

    public String getCourse() {
        return course;
    }

    public void setCourse(String course) {
        this.course = course;
    }
}
