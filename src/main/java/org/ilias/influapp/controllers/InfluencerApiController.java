package org.ilias.influapp.controllers;

import lombok.RequiredArgsConstructor;
import org.ilias.influapp.entities.Influencer;
import org.ilias.influapp.entities.User;
import org.ilias.influapp.exceptions.NotFoundException;
import org.ilias.influapp.repository.InfluencerRepository;
import org.ilias.influapp.services.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/influencer")
@RequiredArgsConstructor
public class InfluencerApiController {

    private final InfluencerRepository influencerRepository;
    private final UserService userService;

    @GetMapping("/{id}")
    public ResponseEntity<Influencer> getInfluencerById(@PathVariable Long id, Authentication authentication) {
        // Verify that the authenticated user can only access their own data
        User currentUser = userService.currentUser(authentication);
        if (!currentUser.getId().equals(id)) {
            return ResponseEntity.status(403).build(); // Forbidden
        }

        Influencer influencer = influencerRepository.findById(id)
                .orElseThrow(NotFoundException::new);

        return ResponseEntity.ok(influencer);
    }
}
