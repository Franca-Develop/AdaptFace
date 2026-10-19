package com.dev.adaptface.security;

import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

public final class ApiErrorWriter {

    public enum Failure {
        UNAUTHORIZED(401, "Autenticação necessária"),
        INVALID_TOKEN(401, "Token inválido ou expirado"),
        FORBIDDEN(403, "Acesso negado"),
        TOO_LARGE(413, "O corpo da requisição excede o limite permitido"),
        UNAVAILABLE(503, "Serviço temporariamente indisponível");

        private final int status;
        private final String message;

        Failure(int status, String message) {
            this.status = status;
            this.message = message;
        }
    }

    private ApiErrorWriter() {
    }

    public static void write(HttpServletResponse response, Failure failure) throws IOException {
        response.setStatus(failure.status);
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write("{\"status\":" + failure.status + ",\"message\":\""
                + failure.message + "\",\"result\":\"DENIED\"}");
    }
}
