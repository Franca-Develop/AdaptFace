package com.dev.adaptface.entities;

import jakarta.persistence.*;

/**
 * Perfil de funcionário. Compartilha o id com User (@MapsId).
 */
@Entity
@Table(name = "employees")
public class Employee {

    @Id
    private Long id;

    @OneToOne(optional = false, fetch = FetchType.LAZY)
    @MapsId
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    private String department;

    public Employee() {
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

    public String getDepartment() {
        return department;
    }

    public void setDepartment(String department) {
        this.department = department;
    }
}
