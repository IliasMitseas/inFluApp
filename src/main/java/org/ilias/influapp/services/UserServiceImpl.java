package org.ilias.influapp.services;

import org.ilias.influapp.entities.User;
import org.ilias.influapp.entities.UserRole;
import org.ilias.influapp.repository.UserRepository;
import org.ilias.influapp.entities.RegisterRequest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserServiceImpl(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public User register(RegisterRequest request) {
        String email = request.getEmail() == null ? null : request.getEmail().trim().toLowerCase();
        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException("Email is required");
        }

        if (userRepository.findByEmail(email).isPresent()) {
            throw new IllegalArgumentException("An account with this email already exists");
        }

        if (request.getPassword() == null || request.getPassword().isBlank()) {
            throw new IllegalArgumentException("Password is required");
        }

        if (!request.getPassword().equals(request.getConfirmPassword())) {
            throw new IllegalArgumentException("Passwords do not match");
        }

        User user = new User();
        user.setEmail(email);
        user.setUsername(generateUniqueUsernameFromEmail(email));
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setRole(UserRole.BUSINESS);

        return userRepository.save(user);
    }

    private String generateUniqueUsernameFromEmail(String email) {
        String base = email.split("@", 2)[0]
                .replaceAll("[^a-zA-Z0-9._-]", "")
                .toLowerCase();

        if (base.isBlank()) {
            base = "user";
        }

        String candidate = base;
        int i = 1;
        while (userRepository.findByUsername(candidate).isPresent()) {
            candidate = base + i;
            i++;
        }
        return candidate;
    }
}
