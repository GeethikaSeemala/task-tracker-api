package com.geethika.tasktracker.dto;


import com.geethika.tasktracker.domain.Project;
import java.time.LocalDateTime;
public record ProjectResponse(Long id, String name, String description, LocalDateTime createdAt) {
    public static ProjectResponse from(Project p) { return new ProjectResponse(p.getId(), p.getName(), p.getDescription(), p.getCreatedAt()); }
}
