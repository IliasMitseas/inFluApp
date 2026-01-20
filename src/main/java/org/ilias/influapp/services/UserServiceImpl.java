package org.ilias.influapp.services;

import org.ilias.influapp.entities.*;
import org.ilias.influapp.repository.UserRepository;
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

        UserRole role = request.getRole() == null ? UserRole.BUSINESS : request.getRole();

        // Create the correct subtype so JOINED inheritance stores rows in users + businesses/influencers.
        User user;
        if (role == UserRole.INFLUENCER) {
            Influencer influencer = new Influencer();
            // TODO: collect these fields in a dedicated InfluencerRegisterRequest
            influencer.setName(email.split("@", 2)[0]);
            // Influencer.category and influencerType are nullable=false in entity; we must set safe defaults.
            influencer.setCategory(Category.FASHION);
            influencer.setInfluencerType(InfluencerType.MICRO);
            user = influencer;
        } else {
            Business business = new Business();
            // TODO: collect these fields in a dedicated BusinessRegisterRequest
            business.setCompanyName(email.split("@", 2)[0]);
            business.setCategory(Category.FASHION);
            user = business;
        }

        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setRole(role);

        return userRepository.save(user);
    }

}
