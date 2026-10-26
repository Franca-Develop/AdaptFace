package com.dev.adaptface.dto.request;

import java.time.LocalDate;

import com.dev.adaptface.entities.enums.Gender;
import com.dev.adaptface.entities.enums.UserType;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PastOrPresent;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Criação de usuário. registrationNumber/period/course valem para STUDENT; department vale para EMPLOYEE.
 */
@Schema(description = "Dados pessoais. Campos acadêmicos valem para STUDENT e department para EMPLOYEE/ADMIN.")
public class UserRequestDTO {

    @NotBlank(message = "Nome é obrigatório")
    @Size(max = 255)
    @Schema(example = "Maria da Silva")
    private String name;

    @NotBlank(message = "CPF é obrigatório")
    @Pattern(regexp = "\\d{11}", message = "CPF deve conter 11 números")
    @Schema(example = "52998224725", pattern = "\\d{11}")
    private String cpf;

    @NotBlank(message = "Email é obrigatório")
    @Email(message = "Email inválido")
    @Size(max = 255)
    @Schema(example = "maria@aluno.senai.br")
    private String email;

    @NotBlank(message = "Senha é obrigatória")
    @Size(min = 8, max = 72, message = "A senha deve possuir entre 8 e 72 caracteres")
    @Schema(example = "senha-segura", format = "password")
    private String password;

    @Size(max = 255)
    private String phone;

    @PastOrPresent
    private LocalDate birthDate;

    private Gender gender;

    @NotNull(message = "Tipo de usuário é obrigatório")
    @Schema(example = "STUDENT")
    private UserType userType;

    @Positive
    @Schema(example = "3640")
    private Integer registrationNumber;
    
    @NotNull(message = "ID da instituição é obrigatório")
    @Positive
    @Schema(example = "1")
    private Long institutionId;

    @Size(max = 255)
    private String period;

    @Size(max = 255)
    @Schema(example = "ADS")
    private String course;

    @Size(max = 255)
    private String department;

    public UserRequestDTO() {
    }
    
    public UserRequestDTO(@NotBlank(message = "Nome é obrigatório") String name,
			@NotBlank(message = "CPF é obrigatório") @Pattern(regexp = "\\d{11}", message = "CPF deve conter 11 números") String cpf,
			@NotBlank(message = "Email é obrigatório") @Email(message = "Email inválido") String email,
			@NotBlank(message = "Senha é obrigatória") @Size(min = 8, max = 72, message = "A senha deve possuir entre 8 e 72 caracteres") String password,
			String phone, LocalDate birthDate, Gender gender,
			@NotNull(message = "Tipo de usuário é obrigatório") UserType userType, Integer registrationNumber,
			@NotNull(message = "ID da instituição é obrigatório") Long institutionId, String period, String course,
			String department) {
		this.name = name;
		this.cpf = cpf;
		this.email = email;
		this.password = password;
		this.phone = phone;
		this.birthDate = birthDate;
		this.gender = gender;
		this.userType = userType;
		this.registrationNumber = registrationNumber;
		this.institutionId = institutionId;
		this.period = period;
		this.course = course;
		this.department = department;
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

    public Integer getRegistrationNumber() {
        return registrationNumber;
    }

    public void setRegistrationNumber(Integer registrationNumber) {
        this.registrationNumber = registrationNumber;
    }

    public Long getInstitutionId() {
		return institutionId;
	}

	public void setInstitutionId(Long institutionId) {
		this.institutionId = institutionId;
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

    public String getDepartment() {
        return department;
    }

    public void setDepartment(String department) {
        this.department = department;
    }
}
