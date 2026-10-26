package com.dev.adaptface.entities;

import java.time.LocalDate;

import com.dev.adaptface.entities.enums.Gender;
import com.dev.adaptface.entities.enums.UserType;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/**
 * Dados comuns de qualquer pessoa do sistema (login + cadastro básico). O
 * perfil específico fica em Student ou Employee, conforme userType. A senha
 * deve ser salva sempre com hash (BCrypt).
 */
@Entity
@Table(name = "users")
public class User {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(optional = false, fetch = FetchType.LAZY)
	@JoinColumn(name = "institution_id", nullable = false)
	private Institution institution;

	@Column(nullable = false)
	private String name;

	@Column(nullable = false, unique = true, length = 11)
	private String cpf;

	@Column(nullable = false, unique = true)
	private String email;

	@Column(nullable = false)
	private String password;

    private Long authVersion = 0L;

	private String phone;

	private LocalDate birthDate;
	
	@Enumerated(EnumType.STRING)
	private Gender gender;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private UserType userType;

	public User() {
	}
	
	public User(Long id, Institution institution, String name, String cpf, String email, String password, String phone,
			LocalDate birthDate, Gender gender, UserType userType) {
		this.id = id;
		this.institution = institution;
		this.name = name;
		this.cpf = cpf;
		this.email = email;
		this.password = password;
		this.phone = phone;
		this.birthDate = birthDate;
		this.gender = gender;
		this.userType = userType;
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public Institution getInstitution() {
		return institution;
	}

	public void setInstitution(Institution institution) {
		this.institution = institution;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getCpf() {
		return cpf;
	}

	public void setCpf(String cpf) {
		this.cpf = cpf;
	}

	public String getEmail() {
		return email;
	}

	public void setEmail(String email) {
		this.email = email;
	}

	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}

    public long getAuthVersion() {
        return authVersion == null ? 0L : authVersion;
    }

    public void setAuthVersion(Long authVersion) {
        this.authVersion = authVersion;
    }

	public String getPhone() {
		return phone;
	}

	public void setPhone(String phone) {
		this.phone = phone;
	}

	public LocalDate getBirthDate() {
		return birthDate;
	}

	public void setBirthDate(LocalDate birthDate) {
		this.birthDate = birthDate;
	}

	public Gender getGender() {
		return gender;
	}

	public void setGender(Gender gender) {
		this.gender = gender;
	}

	public UserType getUserType() {
		return userType;
	}

	public void setUserType(UserType userType) {
		this.userType = userType;
	}
}
