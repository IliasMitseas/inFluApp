package org.ilias.influapp.controllers;

import org.ilias.influapp.entities.UserDto;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api")
public class Controller {

    private final JdbcTemplate jdbcTemplate;

    public Controller(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @GetMapping("/users")
    public List<UserDto> getAllUsers() {
        String sql = "SELECT id, email FROM users";
        return jdbcTemplate.query(sql, (rs, rowNum) ->
                new UserDto(
                        rs.getLong("id"),
                        rs.getString("email")
                )
        );
    }
}

