package org.ilias.influapp.config;

import org.ilias.influapp.entities.User;
import org.ilias.influapp.entities.UserRole;
import org.ilias.influapp.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public DataInitializer(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        // Check if the user already exists to avoid creating duplicates
        if (userRepository.findByEmail("user@example.com").isEmpty()) {
            User user = new User();
            user.setEmail("user@example.com");
            user.setUsername("user"); // Set a non-null unique username
            user.setPassword(passwordEncoder.encode("password"));
            user.setRole(UserRole.BUSINESS); // Set a default role
            userRepository.save(user);
        }
    }
}