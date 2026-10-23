package com.geethika.tasktracker.dto;


import jakarta.validation.constraints.*;
public record ProjectRequest(@NotBlank @Size(max = 150) String name, @Size(max = 2000) String description) {}
