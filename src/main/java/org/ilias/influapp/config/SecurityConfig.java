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
                        // Static assets under src/main/resources/static
                        .requestMatchers("/*.svg", "/*.png", "/*.jpg", "/*.jpeg", "/*.webp", "/*.ico").permitAll()
                        // Role landing pages
                        .requestMatchers("/influencer/**").hasRole("INFLUENCER")
                        .requestMatchers("/business/**").hasRole("BUSINESS")
                        // API rules
                        .requestMatchers("/api/influencer/**").hasRole("INFLUENCER")
                        .requestMatchers("/api/business/**").hasRole("BUSINESS")
                        .anyRequest().authenticated()
                )
                // Enable session-based login for the Thymeleaf login page
                .formLogin(form -> form
                        .loginPage("/login")
                        .successHandler((request, response, authentication) -> {
                            var authorities = authentication.getAuthorities();
                            boolean isInfluencer = authorities.stream().anyMatch(a -> "ROLE_INFLUENCER".equals(a.getAuthority()));
                            boolean isBusiness = authorities.stream().anyMatch(a -> "ROLE_BUSINESS".equals(a.getAuthority()));
                            if (isInfluencer) {
                                response.sendRedirect("/influencer/home");
                            } else if (isBusiness) {
                                response.sendRedirect("/business/home");
                            } else {
                                response.sendRedirect("/");
                            }
                        })
                        .permitAll()
                )
                .rememberMe(remember -> remember
                        .key("uniqueAndSecret")
                        .tokenValiditySeconds(7 * 24 * 60 * 60)// 7 days
                        .rememberMeParameter("remember-me")
                )
                .logout(logout -> logout
                        .logoutUrl("/logout")
                        .logoutSuccessUrl("/login?logout")
                        .invalidateHttpSession(true)
                        .deleteCookies("JSESSIONID", "remember-me")
                        .permitAll()
                )
                .build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}