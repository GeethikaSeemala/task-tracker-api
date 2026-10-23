package com.geethika.tasktracker.dto;


import com.geethika.tasktracker.domain.User;
public record UserResponse(Long id, String name, String email, String role) {
    public static UserResponse from(User u) { return new UserResponse(u.getId(), u.getName(), u.getEmail(), u.getRole()); }
}
