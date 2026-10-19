package com.dev.adaptface.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ReadListener;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Set;
import org.springframework.web.filter.OncePerRequestFilter;

public class RequestBodyLimitFilter extends OncePerRequestFilter {

    public static final int MAX_BODY_BYTES = 65536;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !Set.of("POST", "PUT", "PATCH").contains(request.getMethod());
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        if (request.getContentLengthLong() > MAX_BODY_BYTES) {
            ApiErrorWriter.write(response, ApiErrorWriter.Failure.TOO_LARGE);
            return;
        }
        byte[] body = request.getInputStream().readNBytes(MAX_BODY_BYTES + 1);
        if (body.length > MAX_BODY_BYTES) {
            ApiErrorWriter.write(response, ApiErrorWriter.Failure.TOO_LARGE);
            return;
        }
        ByteArrayInputStream input = new ByteArrayInputStream(body);
        HttpServletRequestWrapper wrapped = new HttpServletRequestWrapper(request) {
            @Override
            public ServletInputStream getInputStream() {
                return new ServletInputStream() {
                    @Override
                    public int read() {
                        return input.read();
                    }

                    @Override
                    public int read(byte[] bytes, int offset, int length) {
                        return input.read(bytes, offset, length);
                    }

                    @Override
                    public boolean isFinished() {
                        return input.available() == 0;
                    }

                    @Override
                    public boolean isReady() {
                        return true;
                    }

                    @Override
                    public void setReadListener(ReadListener listener) {
                        throw new UnsupportedOperationException("Leitura assíncrona não disponível");
                    }
                };
            }

            @Override
            public BufferedReader getReader() {
                return new BufferedReader(new InputStreamReader(getInputStream(), StandardCharsets.UTF_8));
            }
        };
        chain.doFilter(wrapped, response);
    }
}
