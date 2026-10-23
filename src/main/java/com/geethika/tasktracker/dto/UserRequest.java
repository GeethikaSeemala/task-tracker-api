package com.geethika.tasktracker.dto;


import jakarta.validation.constraints.*;
public record UserRequest(@NotBlank @Size(max = 150) String name,
                          @NotBlank @Email @Size(max = 254) String email,
                          @Size(max = 50) String role) {}
