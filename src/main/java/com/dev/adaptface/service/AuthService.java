package com.dev.adaptface.service;

import com.dev.adaptface.dto.request.BootstrapRequestDTO;
import com.dev.adaptface.dto.request.InstitutionRequestDTO;
import com.dev.adaptface.dto.request.LoginRequestDTO;
import com.dev.adaptface.dto.request.UserRequestDTO;
import com.dev.adaptface.dto.response.LoginResponseDTO;
import com.dev.adaptface.entities.User;
import com.dev.adaptface.entities.enums.UserType;
import com.dev.adaptface.exceptions.BusinessException;
import com.dev.adaptface.repository.UserRepository;
import com.dev.adaptface.security.JwtService;
import jakarta.annotation.PostConstruct;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.security.authentication.BadCredentialsException;
import java.util.Locale;

/**
 * Login: valida email/senha e gera o token JWT.
 */
@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final InstitutionService institutionService;
    private final UserService userService;

    @Value("${app.bootstrap.key}")
    private String bootstrapKey;

    private String dummyHash;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder, JwtService jwtService,
            InstitutionService institutionService, UserService userService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.institutionService = institutionService;
        this.userService = userService;
    }

    @PostConstruct
    void validateConfiguration() {
        if (bootstrapKey == null || bootstrapKey.length() < 32) {
            throw new IllegalStateException("BOOTSTRAP_KEY deve possuir no mínimo 32 caracteres");
        }
        dummyHash = passwordEncoder.encode(java.util.UUID.randomUUID().toString());
    }

    @Transactional(readOnly = true)
    public LoginResponseDTO login(LoginRequestDTO request) {
        User user = userRepository.findByEmail(request.getEmail().trim().toLowerCase(Locale.ROOT)).orElse(null);
        String hash = user == null ? dummyHash : user.getPassword();
        if (request.getPassword().getBytes(StandardCharsets.UTF_8).length > 72
                || !passwordEncoder.matches(request.getPassword(), hash) || user == null) {
            throw new BadCredentialsException("Email ou senha inválidos");
        }

        LoginResponseDTO response = new LoginResponseDTO();
        response.setToken(jwtService.generateToken(user));
        response.setUserType(user.getUserType());
        return response;
    }

    @Transactional(isolation = Isolation.SERIALIZABLE)
    public synchronized LoginResponseDTO bootstrap(String informedKey, BootstrapRequestDTO request) {
        if (!matches(informedKey, bootstrapKey)) {
            throw new BusinessException("Chave de configuração inicial inválida");
        }
        if (userRepository.existsByUserType(UserType.ADMIN)) {
            throw new BusinessException("Configuração inicial já realizada");
        }

        InstitutionRequestDTO institutionRequest = new InstitutionRequestDTO();
        institutionRequest.setName(request.getInstitutionName());
        Long institutionId = institutionService.findOrCreate(institutionRequest).getId();

        UserRequestDTO userRequest = new UserRequestDTO();
        userRequest.setInstitutionId(institutionId);
        userRequest.setName(request.getName());
        userRequest.setCpf(request.getCpf());
        userRequest.setEmail(request.getEmail());
        userRequest.setPassword(request.getPassword());
        userRequest.setPhone(request.getPhone());
        userRequest.setBirthDate(request.getBirthDate());
        userRequest.setGender(request.getGender());
        userRequest.setUserType(UserType.ADMIN);
        userRequest.setDepartment(request.getDepartment());
        Long userId = userService.create(userRequest).getId();

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException("Não foi possível concluir a configuração inicial"));
        LoginResponseDTO response = new LoginResponseDTO();
        response.setToken(jwtService.generateToken(user));
        response.setUserType(user.getUserType());
        return response;
    }

    private boolean matches(String first, String second) {
        return first != null && MessageDigest.isEqual(first.getBytes(StandardCharsets.UTF_8),
                second.getBytes(StandardCharsets.UTF_8));
    }

    @Transactional(readOnly = true)
    public boolean requiresSetup() {
        return !userRepository.existsByUserType(UserType.ADMIN);
    }
}
