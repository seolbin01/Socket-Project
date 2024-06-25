package com.benny.socketApp.dto;

public class UserResponse {

    private long id;
    private String username;

    public UserResponse(long id, String username) {
        this.id = id;
        this.username = username;
    }

    public long getId() {
        return id;
    }

    public String getUsername() {
        return username;
    }
}
