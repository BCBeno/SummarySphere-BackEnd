package com.beno.summaryspherebackend.services;

import com.beno.summaryspherebackend.entities.User;
import com.beno.summaryspherebackend.enums.Role;
import com.beno.summaryspherebackend.repositories.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserDetailsServiceImplTest {

    @Mock
    UserRepository userRepository;

    @InjectMocks
    UserDetailsServiceImpl userDetailsService;

    @Test
    void loadUserByUsername_returnsUserDetails_whenUserExists() {
        User user = User.builder()
                .email("test@test.com")
                .password("pw")
                .fullName("Test")
                .role(Role.USER)
                .build();

        when(userRepository.findByEmail("test@test.com")).thenReturn(Optional.of(user));

        var result = userDetailsService.loadUserByUsername("test@test.com");
        assertSame(user, result);
    }

    @Test
    void loadUserByUsername_throws_whenMissing() {
        when(userRepository.findByEmail("missing@test.com")).thenReturn(Optional.empty());
        assertThrows(UsernameNotFoundException.class,
                () -> userDetailsService.loadUserByUsername("missing@test.com"));
    }
}

