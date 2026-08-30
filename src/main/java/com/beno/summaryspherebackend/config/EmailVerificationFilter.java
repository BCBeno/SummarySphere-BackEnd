package com.beno.summaryspherebackend.config;

import com.beno.summaryspherebackend.entities.User;
import com.beno.summaryspherebackend.enums.Role;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

public class EmailVerificationFilter extends OncePerRequestFilter {

    private static final String VERIFICATION_REQUIRED =
            "Verify your email to upload documents and use application features.";

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication != null && authentication.getPrincipal() instanceof User user
                && user.getRole() == Role.USER
                && !user.isEmailVerified()) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN, VERIFICATION_REQUIRED);
            return;
        }

        filterChain.doFilter(request, response);
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        boolean authEndpoint = path.startsWith("/api/auth/");
        boolean deleteOwnAccount = "DELETE".equals(request.getMethod()) && "/api/users/me".equals(path);

        return authEndpoint || deleteOwnAccount;
    }
}
