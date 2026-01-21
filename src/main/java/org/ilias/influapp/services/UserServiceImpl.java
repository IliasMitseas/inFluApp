package org.ilias.influapp.services;

import lombok.RequiredArgsConstructor;
import org.ilias.influapp.entities.*;
import org.ilias.influapp.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@RequiredArgsConstructor
@Service
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public User register(RegisterRequest request) {
        String email = request.getEmail() == null ? null : request.getEmail().trim().toLowerCase();
        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException("Email is required");
        }else if (userRepository.findByEmail(email).isPresent()) {
            throw new IllegalArgumentException("An account with this email already exists");
        }else if (request.getPassword() == null || request.getPassword().isBlank()) {
            throw new IllegalArgumentException("Password is required");
        }else if (!request.getPassword().equals(request.getConfirmPassword())) {
            throw new IllegalArgumentException("Passwords do not match");
        }

        UserRole role = request.getRole() == null ? UserRole.BUSINESS : request.getRole();
        User user;
        if (role == UserRole.INFLUENCER) {
            user = new Influencer();
        } else {
            user = new Business();
        }
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setRole(role);
        return userRepository.save(user);
    }

}
