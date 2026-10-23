package com.geethika.tasktracker.config;


import com.geethika.tasktracker.domain.Priority;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;
@Validated
@ConfigurationProperties(prefix = "task")
public record TaskProperties(@NotNull Priority defaultPriority) {}
