package com.dev.adaptface.service;

import com.dev.adaptface.dto.request.UserRequestDTO;
import com.dev.adaptface.dto.request.UserUpdateRequestDTO;
import com.dev.adaptface.dto.response.UserResponseDTO;
import com.dev.adaptface.entities.Employee;
import com.dev.adaptface.entities.Institution;
import com.dev.adaptface.entities.Student;
import com.dev.adaptface.entities.User;
import com.dev.adaptface.entities.enums.UserType;
import com.dev.adaptface.exceptions.BusinessException;
import com.dev.adaptface.exceptions.NotFoundException;
import com.dev.adaptface.repository.EmployeeRepository;
import com.dev.adaptface.repository.InstitutionRepository;
import com.dev.adaptface.repository.StudentRepository;
import com.dev.adaptface.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import java.util.Locale;
import java.nio.charset.StandardCharsets;

/**
 * Cadastro e consulta de usuários (User + Student/Employee).
 */
@Service
public class UserService {

    private final UserRepository userRepository;
    private final StudentRepository studentRepository;
    private final EmployeeRepository employeeRepository;
    private final InstitutionRepository institutionRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository, StudentRepository studentRepository,
            EmployeeRepository employeeRepository, InstitutionRepository institutionRepository,
            PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.studentRepository = studentRepository;
        this.employeeRepository = employeeRepository;
        this.institutionRepository = institutionRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public UserResponseDTO create(UserRequestDTO request) {
        String email = request.getEmail().trim().toLowerCase(Locale.ROOT);
        validatePassword(request.getPassword());
        if (userRepository.existsByEmail(email)) {
            throw new BusinessException("Email já cadastrado");
        }
        if (userRepository.existsByCpf(request.getCpf())) {
            throw new BusinessException("CPF já cadastrado");
        }

        validateProfile(request);
        Institution institution = institutionRepository.findById(request.getInstitutionId())
                .orElseThrow(() -> new NotFoundException("Instituição não encontrada"));

        User user = new User();
        user.setInstitution(institution);
        user.setName(request.getName().trim());
        user.setCpf(request.getCpf());
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setPhone(trimToNull(request.getPhone()));
        user.setBirthDate(request.getBirthDate());
        user.setGender(request.getGender());
        user.setUserType(request.getUserType());
        userRepository.save(user);

        if (user.getUserType() == UserType.STUDENT) {
            Student student = new Student();
            student.setUser(user);
            student.setRegistrationNumber(request.getRegistrationNumber());
            student.setPeriod(request.getPeriod().trim());
            student.setCourse(request.getCourse().trim());
            studentRepository.save(student);
        } else {
            Employee employee = new Employee();
            employee.setUser(user);
            employee.setDepartment(request.getDepartment().trim());
            employeeRepository.save(employee);
        }

        return toResponse(user);
    }

    @Transactional(readOnly = true)
    public UserResponseDTO findById(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Usuário não encontrado"));
        return toResponse(user);
    }

    @Transactional(readOnly = true)
    public Page<UserResponseDTO> findAll(Pageable pageable) {
        PageRequest bounded = PageRequest.of(pageable.getPageNumber(), Math.min(pageable.getPageSize(), 100),
                Sort.by("id").ascending());
        return userRepository.findAll(bounded).map(this::toResponse);
    }

    @Transactional
    public UserResponseDTO update(Long id, UserUpdateRequestDTO request) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Usuário não encontrado"));
        String email = request.getEmail().trim().toLowerCase(Locale.ROOT);
        userRepository.findByEmail(email).filter(other -> !other.getId().equals(id)).ifPresent(other -> {
            throw new BusinessException("Email já cadastrado");
        });
        userRepository.findByCpf(request.getCpf()).filter(other -> !other.getId().equals(id)).ifPresent(other -> {
            throw new BusinessException("CPF já cadastrado");
        });
        UserRequestDTO profile = new UserRequestDTO();
        profile.setUserType(user.getUserType());
        profile.setRegistrationNumber(request.getRegistrationNumber());
        profile.setCourse(request.getCourse());
        profile.setPeriod(request.getPeriod());
        profile.setDepartment(request.getDepartment());
        validateProfile(profile);
        Institution institution = institutionRepository.findById(request.getInstitutionId())
                .orElseThrow(() -> new NotFoundException("Instituição não encontrada"));

        if (user.getUserType() == UserType.STUDENT) {
            Student student = studentRepository.findByUserId(id)
                    .orElseThrow(() -> new BusinessException("Perfil de aluno não encontrado"));
            student.setRegistrationNumber(request.getRegistrationNumber());
            student.setCourse(request.getCourse().trim());
            student.setPeriod(request.getPeriod().trim());
        } else {
            Employee employee = employeeRepository.findByUserId(id)
                    .orElseThrow(() -> new BusinessException("Perfil de funcionário não encontrado"));
            employee.setDepartment(request.getDepartment().trim());
        }
        user.setName(request.getName().trim());
        user.setCpf(request.getCpf());
        user.setEmail(email);
        user.setInstitution(institution);
        user.setPhone(trimToNull(request.getPhone()));
        user.setBirthDate(request.getBirthDate());
        user.setGender(request.getGender());
        if (request.getPassword() != null) {
            validatePassword(request.getPassword());
            user.setPassword(passwordEncoder.encode(request.getPassword()));
            user.setAuthVersion(Math.addExact(user.getAuthVersion(), 1));
        }
        return toResponse(userRepository.save(user));
    }

    private void validatePassword(String password) {
        if (password == null || password.isBlank() || password.length() < 8
                || password.getBytes(StandardCharsets.UTF_8).length > 72) {
            throw new BusinessException("Senha deve possuir pelo menos 8 caracteres e no máximo 72 bytes UTF-8");
        }
    }

    private void validateProfile(UserRequestDTO request) {
        if (request.getUserType() == UserType.STUDENT) {
            if (request.getRegistrationNumber() == null || request.getRegistrationNumber() <= 0
                    || isBlank(request.getPeriod()) || isBlank(request.getCourse())) {
                throw new BusinessException("RM, período e curso são obrigatórios para alunos");
            }
        } else if (isBlank(request.getDepartment())) {
            throw new BusinessException("Departamento é obrigatório para funcionários");
        }
    }

    private UserResponseDTO toResponse(User user) {
        UserResponseDTO response = new UserResponseDTO();
        response.setId(user.getId());
        response.setInstitutionId(user.getInstitution().getId());
        response.setName(user.getName());
        response.setCpf(user.getCpf());
        response.setEmail(user.getEmail());
        response.setPhone(user.getPhone());
        response.setBirthDate(user.getBirthDate());
        response.setGender(user.getGender());
        response.setUserType(user.getUserType());

        if (user.getUserType() == UserType.STUDENT) {
            studentRepository.findByUserId(user.getId()).ifPresent(student -> {
                response.setRegistrationNumber(student.getRegistrationNumber());
                response.setPeriod(student.getPeriod());
                response.setCourse(student.getCourse());
            });
        } else {
            employeeRepository.findByUserId(user.getId())
                    .ifPresent(employee -> response.setDepartment(employee.getDepartment()));
        }
        return response;
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private String trimToNull(String value) {
        return isBlank(value) ? null : value.trim();
    }
}
