package com.dev.adaptface.dto.request;

import java.time.LocalDate;
import com.dev.adaptface.entities.enums.Gender;
import jakarta.validation.constraints.*;

public class UserUpdateRequestDTO {

    @NotBlank(message = "Nome é obrigatório")
    @Size(max = 255)
    private String name;

    @NotBlank(message = "CPF é obrigatório")
    @Pattern(regexp = "\\d{11}", message = "CPF deve conter 11 números")
    private String cpf;

    @NotBlank(message = "Email é obrigatório")
    @Email(message = "Email inválido")
    @Size(max = 255)
    private String email;

    @Size(min = 8, max = 72, message = "Senha deve possuir entre 8 e 72 caracteres")
    private String password;

    @Size(max = 255)
    private String phone;

    @PastOrPresent
    private LocalDate birthDate;

    private Gender gender;

    @NotNull(message = "Instituição é obrigatória")
    @Positive
    private Long institutionId;

    @Positive
    private Integer registrationNumber;

    @Size(max = 255)
    private String period;

    @Size(max = 255)
    private String course;

    @Size(max = 255)
    private String department;

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

    public Long getInstitutionId() {
        return institutionId;
    }

    public void setInstitutionId(Long institutionId) {
        this.institutionId = institutionId;
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

    public String getDepartment() {
        return department;
    }

    public void setDepartment(String department) {
        this.department = department;
    }
}

