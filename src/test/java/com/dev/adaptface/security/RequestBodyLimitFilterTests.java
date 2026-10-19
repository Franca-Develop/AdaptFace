package com.dev.adaptface.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class RequestBodyLimitFilterTests {

    @Test
    void shouldRejectOversizedBodyWithoutContentLength() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/access/validate") {
            @Override public long getContentLengthLong() { return -1; }
        };
        request.setContent(new byte[65537]);
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);
        new RequestBodyLimitFilter().doFilter(request, response, chain);
        assertThat(response.getStatus()).isEqualTo(413);
        assertThat(response.getContentAsString()).contains("DENIED");
        verifyNoInteractions(chain);
    }

    @Test
    void shouldKeepValidUtf8BodyAvailableToController() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("PUT", "/users/1");
        request.setContent("{\"name\":\"João sintético\"}".getBytes(StandardCharsets.UTF_8));
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);
        doAnswer(invocation -> {
            HttpServletRequest wrapped = invocation.getArgument(0);
            assertThat(wrapped.getReader().readLine()).contains("João sintético");
            return null;
        }).when(chain).doFilter(any(), any());
        new RequestBodyLimitFilter().doFilter(request, response, chain);
        verify(chain).doFilter(any(), any());
    }
}
