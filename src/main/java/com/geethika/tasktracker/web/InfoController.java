package com.geethika.tasktracker.web;


import com.geethika.tasktracker.config.AppProperties;
import org.springframework.web.bind.annotation.*;
@RestController
public class InfoController {
    private final AppProperties properties;
    public InfoController(AppProperties properties) { this.properties = properties; }
    @GetMapping("/api/info")
    public AppProperties info() { return properties; }
}
