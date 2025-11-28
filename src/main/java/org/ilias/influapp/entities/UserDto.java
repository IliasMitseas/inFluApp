package org.ilias.influapp.entities;

import lombok.Getter;

@Getter
public class UserDto {
    private Long id;
    private String email;
    private String password;
    private String role;
    private String createdAt;

    public UserDto(Long id, String email) {
        this.id = id;
        this.email = email;
    }
}
