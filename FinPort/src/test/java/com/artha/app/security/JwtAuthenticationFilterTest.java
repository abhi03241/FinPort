package com.artha.app.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class JwtAuthenticationFilterTest {

    private final JwtService jwtService = new JwtService(
            "test-secret-test-secret-test-secret-test-secret", 60, 14, "artha-test");
    {
        jwtService.initForTest();
    }
    private final JwtAuthenticationFilter filter = new JwtAuthenticationFilter(jwtService);

    @BeforeEach
    @AfterEach
    void clear() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void setsAuthenticationOnValidBearerToken() throws Exception {
        String token = jwtService.issueAccessToken("abhi");
        MockHttpServletRequest req = new MockHttpServletRequest();
        req.addHeader("Authorization", "Bearer " + token);
        MockHttpServletResponse res = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(req, res, chain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNotNull();
        assertThat(SecurityContextHolder.getContext().getAuthentication().getName()).isEqualTo("abhi");
        verify(chain).doFilter(req, res);
    }

    @Test
    void ignoresRefreshTokens() throws Exception {
        String refresh = jwtService.issueRefreshToken("abhi");
        MockHttpServletRequest req = new MockHttpServletRequest();
        req.addHeader("Authorization", "Bearer " + refresh);
        MockHttpServletResponse res = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(req, res, chain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(chain).doFilter(req, res);
    }

    @Test
    void doesNothingWhenNoAuthorizationHeader() throws Exception {
        MockHttpServletRequest req = new MockHttpServletRequest();
        MockHttpServletResponse res = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(req, res, chain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(chain).doFilter(req, res);
    }

    @Test
    void clearsContextOnInvalidToken() throws Exception {
        MockHttpServletRequest req = new MockHttpServletRequest();
        req.addHeader("Authorization", "Bearer garbage");
        MockHttpServletResponse res = new MockHttpServletResponse();
        FilterChain chain = new MockFilterChain();

        filter.doFilter(req, res, chain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }
}