package org.ilias.influapp.services;

import org.ilias.influapp.entities.User;
import org.ilias.influapp.entities.RegisterRequest;
import org.springframework.security.core.Authentication;

public interface UserService {

    User register(RegisterRequest request);

    User currentUser(Authentication authentication);
}

