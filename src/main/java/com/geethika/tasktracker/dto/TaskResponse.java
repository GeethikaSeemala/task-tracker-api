package com.geethika.tasktracker.dto;


import com.geethika.tasktracker.domain.*;
import java.time.LocalDate;
public record TaskResponse(Long id, String title, Status status, Priority priority, Long projectId, Long assigneeId, LocalDate dueDate) {
    public static TaskResponse from(Task t) {
        return new TaskResponse(t.getId(), t.getTitle(), t.getStatus(), t.getPriority(), t.getProject().getId(),
                t.getAssignee() == null ? null : t.getAssignee().getId(), t.getDueDate());
    }
}
