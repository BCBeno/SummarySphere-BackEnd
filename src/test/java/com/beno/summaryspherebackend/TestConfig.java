package com.beno.summaryspherebackend;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import com.beno.summaryspherebackend.services.JwtService;

import static org.mockito.Mockito.mock;

@Configuration
public class TestConfig {

    @Bean
    @Primary
    public JwtService testJwtService() {
        // Provide a Mockito mock JwtService for tests so that context startup doesn't require full JWT wiring
        return mock(JwtService.class);
    }
}
