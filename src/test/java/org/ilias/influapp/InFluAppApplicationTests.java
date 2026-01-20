package org.ilias.influapp;

import org.ilias.influapp.entities.RegisterRequest;
import org.ilias.influapp.entities.User;
import org.ilias.influapp.entities.UserRole;
import org.ilias.influapp.services.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class InFluAppApplicationTests {

    @Autowired
    private UserService userService;

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
