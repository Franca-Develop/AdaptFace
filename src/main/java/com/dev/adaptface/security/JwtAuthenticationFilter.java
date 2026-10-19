package com.dev.adaptface.security;

import com.dev.adaptface.entity.User;
import com.dev.adaptface.repository.UserRepository;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.dao.DataAccessException;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final UserRepository userRepository;

    public JwtAuthenticationFilter(JwtService jwtService, UserRepository userRepository) {
        this.jwtService = jwtService;
        this.userRepository = userRepository;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String authorization = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        try {
            String token = authorization.substring(7);
            String email = jwtService.extractSubject(token);
            if (SecurityContextHolder.getContext().getAuthentication() == null) {
                userRepository.findByEmail(email).filter(user -> jwtService.isValid(token, user)).ifPresent(user -> {
                    UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                            user.getEmail(), null, authorities(user));
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                });
            }
        } catch (JwtException | IllegalArgumentException exception) {
            ApiErrorWriter.write(response, ApiErrorWriter.Failure.INVALID_TOKEN);
            return;
        } catch (DataAccessException exception) {
            ApiErrorWriter.write(response, ApiErrorWriter.Failure.UNAVAILABLE);
            return;
        }
        filterChain.doFilter(request, response);
    }

    private List<SimpleGrantedAuthority> authorities(User user) {
        return List.of(new SimpleGrantedAuthority("ROLE_" + user.getUserType().name()));
    }
}
