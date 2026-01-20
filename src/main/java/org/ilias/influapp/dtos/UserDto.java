package org.ilias.influapp.dtos;

import lombok.Getter;
import org.ilias.influapp.entities.UserRole;

@Getter
public class UserDto {
    private Long id;
    private String email;
    private String password;
    private UserRole role;
    private String createdAt;

    public UserDto(Long id, String email, String createdAt) {
        this.id = id;
        this.email = email;
        this.createdAt = createdAt;
    }
}
