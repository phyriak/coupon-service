package com.example.coupon.security;

import com.example.coupon.anonymizer.HashUtil;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.preauth.PreAuthenticatedAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class ApiKeyAuthFilter extends OncePerRequestFilter {
    private final Map<String, List<String>> hashToRoles;

    public ApiKeyAuthFilter(ApiKeyProperties properties) {
        this.hashToRoles = properties.apiKeys().stream()
                .collect(Collectors.toMap(
                        ApiKeyProperties.ApiKeyEntry::hash,
                        ApiKeyProperties.ApiKeyEntry::roles,
                        (a, b) -> { throw new IllegalStateException(
                                "Duplicate API key hash in security.api-keys configuration"); }));
    }

    @Override
    protected void doFilterInternal(HttpServletRequest req,
                                    HttpServletResponse res,
                                    FilterChain chain) throws ServletException, IOException {

        String key = req.getHeader("X-Api-Key");
        if (key != null && !key.isBlank()) {
            String shaed256 = HashUtil.sha256(key);
            List<String> roles = hashToRoles.get(shaed256);
            if (roles != null && !roles.isEmpty()) {
                var authorities = roles.stream()
                        .map(SimpleGrantedAuthority::new)
                        .toList();
                SecurityContextHolder.getContext().setAuthentication(
                        new PreAuthenticatedAuthenticationToken("api-client", null, authorities));
            }
        }
        chain.doFilter(req, res);
    }
}