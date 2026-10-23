package com.geethika.tasktracker.dto;


import com.geethika.tasktracker.domain.*;
import jakarta.validation.constraints.*;
import java.time.LocalDate;
public record TaskRequest(@NotBlank @Size(max = 200) String title, Status status, Priority priority,
                          @NotNull @Positive Long projectId, @Positive Long assigneeId, LocalDate dueDate) {}
