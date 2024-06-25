package com.benny.socketApp.controller;

import com.benny.socketApp.dto.UserCreateRequest;
import com.benny.socketApp.dto.UserResponse;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

@RestController
public class UserController {

    private final JdbcTemplate jdbcTemplate;

    public UserController(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @PostMapping("/user")
    public void saveUser(@RequestBody UserCreateRequest request) {
        String sql = "INSERT INTO users (username) VALUES (?)";
        jdbcTemplate.update(sql,request.getUsername());
    }

    @GetMapping("/user")
    public List<UserResponse> getUsers() {
        String sql = "SELECT * FROM users";
        return jdbcTemplate.query(sql, new RowMapper<UserResponse>() {
            @Override
            public UserResponse mapRow(ResultSet rs, int rowNum) throws SQLException {
                long id = rs.getLong("id");
                String username = rs.getString("username");
                return new UserResponse(id, username);
            }
        });
    }

    @PostMapping("/login")
    public UserResponse login(@RequestBody UserCreateRequest request) {
        String sql = "SELECT * FROM users WHERE username = ?";
        List<UserResponse> users = jdbcTemplate.query(sql, new RowMapper<UserResponse>() {
            @Override
            public UserResponse mapRow(ResultSet rs, int rowNum) throws SQLException {
                long id = rs.getLong("id");
                String username = rs.getString("username");
                return new UserResponse(id, username);
            }
        }, request.getUsername());

        if (users.isEmpty()) {
            throw new IllegalArgumentException("Invalid username");
        }

        return users.get(0);
    }
}
