package org.ilias.influapp;

import org.ilias.influapp.entities.*;
import org.ilias.influapp.entities.Enums.Category;
import org.ilias.influapp.entities.Enums.InfluencerType;
import org.ilias.influapp.entities.Enums.UserRole;
import org.ilias.influapp.repository.InfluencerRepository;
import org.ilias.influapp.services.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class InFluAppApplicationTests {

    @Autowired
    private UserService userService;

    @Autowired
    private InfluencerRepository influencerRepository;

    @Test
    void contextLoads() {
    }

    @Test
    void registerBusiness_withoutCategory_shouldSucceed() {
        RegisterRequest req = new RegisterRequest();
        req.setEmail("business_no_cat_" + System.currentTimeMillis() + "@example.com");
        req.setPassword("pass1234");
        req.setConfirmPassword("pass1234");
        req.setRole(UserRole.BUSINESS);

        User saved = userService.register(req);
        assertNotNull(saved.getId());
        assertEquals(UserRole.BUSINESS, saved.getRole());
    }

    @Test
    void registerInfluencer_withoutCategory_shouldSucceed() {
        RegisterRequest req = new RegisterRequest();
        req.setEmail("influencer_no_cat_" + System.currentTimeMillis() + "@example.com");
        req.setPassword("pass1234");
        req.setConfirmPassword("pass1234");
        req.setRole(UserRole.INFLUENCER);

        User saved = userService.register(req);
        assertNotNull(saved.getId());
        assertEquals(UserRole.INFLUENCER, saved.getRole());
    }
}
