package com.parking.backend.gate;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class ApiKeyFilter extends OncePerRequestFilter {

    private final byte[] expected;

    public ApiKeyFilter(@Value("${app.gate.api-key}") String apiKey) {
        if (apiKey == null || apiKey.length() < 16) {
            throw new IllegalStateException("app.gate.api-key must be at least 16 characters");
        }
        this.expected = apiKey.getBytes(StandardCharsets.UTF_8);
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !request.getRequestURI().startsWith("/api/detection/");   // the key works ONLY for the gate
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        String given = request.getHeader("X-API-Key");
        if (given != null && MessageDigest.isEqual(expected, given.getBytes(StandardCharsets.UTF_8))) {
            var auth = new UsernamePasswordAuthenticationToken(
                    "gate-camera", null, List.of(new SimpleGrantedAuthority("ROLE_GATE")));
            SecurityContextHolder.getContext().setAuthentication(auth);
        }
        chain.doFilter(request, response);       // no or wrong key: stay anonymous, the rules answer 401
    }
}