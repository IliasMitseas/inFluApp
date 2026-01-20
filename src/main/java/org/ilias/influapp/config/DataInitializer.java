package org.ilias.influapp.config;

import lombok.RequiredArgsConstructor;
import org.ilias.influapp.entities.*;
import org.ilias.influapp.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        seedOrUpgradeDemoBusiness();
    }

    private void seedOrUpgradeDemoBusiness() {
        var existing = userRepository.findByEmail("user@example.com");
        if (existing.isPresent() && !(existing.get() instanceof Business)) {
            userRepository.deleteById(existing.get().getId());
        }

        if (userRepository.findByEmail("user@example.com").isEmpty()) {
            Business user = new Business();
            user.setEmail("user@example.com");
            user.setUsername("user");
            user.setPassword(passwordEncoder.encode("password"));
            user.setRole(UserRole.BUSINESS);
            user.setCompanyName("Demo Business");
            user.setCategory(Category.BUSINESS);
            userRepository.save(user);
        }
    }
}