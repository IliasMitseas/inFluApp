package org.ilias.influapp.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
                .csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(auth -> auth
                        // Public pages
                        .requestMatchers("/login", "/register", "/css/**", "/js/**", "/images/**").permitAll()
                        // Dev/testing endpoint: either open it, or change to .hasRole("BUSINESS") etc.
                        .requestMatchers("/api/users").permitAll()
                        .requestMatchers("/api/influencer/**").hasRole("INFLUENCER")
                        .requestMatchers("/api/business/**").hasRole("BUSINESS")
                        .anyRequest().authenticated()
                )
                // Enable session-based login for the Thymeleaf login page
                .formLogin(form -> form
                        .loginPage("/login")
                        .permitAll()
                )
                .logout(logout -> logout.permitAll())
                .build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}