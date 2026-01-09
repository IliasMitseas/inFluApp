package org.ilias.influapp.services;

import org.ilias.influapp.entities.User;
import org.ilias.influapp.entities.RegisterRequest;

public interface UserService {

    User register(RegisterRequest request);
}

