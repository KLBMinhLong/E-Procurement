package com.eprocure.iam.infrastructure.security;

import com.eprocure.iam.application.service.SessionService;
import com.eprocure.iam.common.security.UserPrincipal;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Arrays;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class SessionAuthenticationFilter extends OncePerRequestFilter {
    private final SessionService sessionService;
    private final String cookieName;

    public SessionAuthenticationFilter(
            SessionService sessionService,
            @Value("${eprocure.security.cookie-name:ep_session}") String cookieName) {
        this.sessionService = sessionService;
        this.cookieName = cookieName;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {
        if (SecurityContextHolder.getContext().getAuthentication() == null) {
            extractCookie(request)
                    .flatMap(sessionService::authenticate)
                    .ifPresent(this::authenticate);
        }
        filterChain.doFilter(request, response);
    }

    private Optional<String> extractCookie(HttpServletRequest request) {
        return Optional.ofNullable(request.getCookies())
                .stream()
                .flatMap(Arrays::stream)
                .filter(cookie -> cookieName.equals(cookie.getName()))
                .map(Cookie::getValue)
                .filter(value -> !value.isBlank())
                .findFirst();
    }

    private void authenticate(UserPrincipal principal) {
        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                principal,
                "",
                principal.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }
}
