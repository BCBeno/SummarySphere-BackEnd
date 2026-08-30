package com.beno.summaryspherebackend.config;

import com.beno.summaryspherebackend.entities.User;
import com.beno.summaryspherebackend.enums.Role;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

class EmailVerificationFilterTest {

    private final EmailVerificationFilter filter = new EmailVerificationFilter();

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void unverifiedUserCannotAccessApplicationEndpoints() throws Exception {
        authenticate(false);
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/documents");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        assertEquals(403, response.getStatus());
        assertNull(chain.getRequest());
    }

    @Test
    void verifiedUserCanAccessApplicationEndpoints() throws Exception {
        authenticate(true);
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/documents");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        assertNotNull(chain.getRequest());
    }

    @Test
    void unverifiedUserCanStillDeleteOwnAccount() throws Exception {
        authenticate(false);
        MockHttpServletRequest request = new MockHttpServletRequest("DELETE", "/api/users/me");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        assertNotNull(chain.getRequest());
    }

    private void authenticate(boolean emailVerified) {
        User user = User.builder()
                .email("test@test.com")
                .password("pw")
                .fullName("Test")
                .role(Role.USER)
                .emailVerified(emailVerified)
                .build();
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities()));
    }
}
