package com.dev.adaptface.exceptions;

import com.dev.adaptface.dto.response.ErrorResponseDTO;
import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import java.util.stream.Collectors;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.DataAccessException;
import org.springframework.transaction.TransactionException;
import com.dev.adaptface.entities.enums.AccessResult;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.ServletRequestBindingException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ErrorResponseDTO> handleAuthentication(AuthenticationException exception, HttpServletRequest request) {
        return response(HttpStatus.UNAUTHORIZED, "Email ou senha inválidos", request);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponseDTO> handleDenied(AccessDeniedException exception, HttpServletRequest request) {
        return response(HttpStatus.FORBIDDEN, "Acesso negado", request);
    }

    @ExceptionHandler({ServletRequestBindingException.class, MethodArgumentTypeMismatchException.class})
    public ResponseEntity<ErrorResponseDTO> handleBinding(Exception exception, HttpServletRequest request) {
        return response(HttpStatus.BAD_REQUEST, "Parâmetro obrigatório ausente ou inválido", request);
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ErrorResponseDTO> handleMissingRoute(NoResourceFoundException exception, HttpServletRequest request) {
        return response(HttpStatus.NOT_FOUND, "Recurso não encontrado", request);
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ErrorResponseDTO> handleMethod(HttpRequestMethodNotSupportedException exception, HttpServletRequest request) {
        return response(HttpStatus.METHOD_NOT_ALLOWED, "Método não permitido", request);
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<ErrorResponseDTO> handleMedia(HttpMediaTypeNotSupportedException exception, HttpServletRequest request) {
        return response(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "Formato de requisição não suportado", request);
    }

    @ExceptionHandler({DataAccessException.class, TransactionException.class})
    public ResponseEntity<ErrorResponseDTO> handleUnavailable(Exception exception, HttpServletRequest request) {
        return response(HttpStatus.SERVICE_UNAVAILABLE, "Não foi possível confirmar a operação. Serviço temporariamente indisponível", request);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponseDTO> handleInvalidArgument(IllegalArgumentException exception, HttpServletRequest request) {
        return response(HttpStatus.BAD_REQUEST, "Parâmetro inválido", request);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponseDTO> handleUnexpected(Exception exception, HttpServletRequest request) {
        return response(HttpStatus.INTERNAL_SERVER_ERROR, "Não foi possível concluir a operação", request);
    }

    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<ErrorResponseDTO> handleNotFound(NotFoundException exception, HttpServletRequest request) {
        return response(HttpStatus.NOT_FOUND, exception.getMessage(), request);
    }

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ErrorResponseDTO> handleBusiness(BusinessException exception, HttpServletRequest request) {
        return response(HttpStatus.BAD_REQUEST, exception.getMessage(), request);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponseDTO> handleValidation(MethodArgumentNotValidException exception,
            HttpServletRequest request) {
        String message = exception.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .distinct()
                .collect(Collectors.joining("; "));
        return response(HttpStatus.BAD_REQUEST, message, request);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponseDTO> handleUnreadable(HttpMessageNotReadableException exception,
            HttpServletRequest request) {
        return response(HttpStatus.BAD_REQUEST, "Corpo da requisição inválido", request);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponseDTO> handleIntegrity(DataIntegrityViolationException exception,
            HttpServletRequest request) {
        return response(HttpStatus.CONFLICT, "Os dados informados já estão cadastrados", request);
    }

    private ResponseEntity<ErrorResponseDTO> response(HttpStatus status, String message, HttpServletRequest request) {
        ErrorResponseDTO error = new ErrorResponseDTO();
        error.setTimestamp(LocalDateTime.now());
        error.setStatus(status.value());
        error.setError(status.getReasonPhrase());
        error.setMessage(message);
        error.setPath(request.getRequestURI());
        if (request.getRequestURI().endsWith("/access/validate")) {
            error.setResult(AccessResult.DENIED);
        }
        return ResponseEntity.status(status).body(error);
    }
}
