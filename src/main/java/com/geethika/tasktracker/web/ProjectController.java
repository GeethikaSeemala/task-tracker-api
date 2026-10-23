package com.geethika.tasktracker.web;


import com.geethika.tasktracker.dto.*;
import com.geethika.tasktracker.domain.Status;
import com.geethika.tasktracker.service.*;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.net.URI;
import java.util.Set;
@RestController
@RequestMapping("/api/projects")
public class ProjectController {
    private final ProjectService service;
    private final Paging paging;
    private final TaskService taskService;
    public ProjectController(ProjectService service, Paging paging, TaskService taskService) {
        this.service = service; this.paging = paging; this.taskService = taskService;
    }
    @PostMapping
    public ResponseEntity<ProjectResponse> create(@Valid @RequestBody ProjectRequest request) {
        var result = service.create(request);
        return ResponseEntity.created(URI.create("/api/projects/" + result.id())).body(result);
    }
    @GetMapping
    public PageResponse<ProjectResponse> list(@RequestParam(defaultValue = "0") int page, @RequestParam(required = false) Integer size,
            @RequestParam(defaultValue = "id,asc") String sort) {
        return PageResponse.from(service.list(paging.of(page, size, sort, Set.of("id", "name", "createdAt"))));
    }
    @GetMapping("/{id}")
    public ProjectResponse get(@PathVariable Long id) { return service.get(id); }
    @PutMapping("/{id}")
    public ProjectResponse update(@PathVariable Long id, @Valid @RequestBody ProjectRequest request) { return service.update(id, request); }
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) { service.delete(id); return ResponseEntity.noContent().build(); }

    @GetMapping("/{id}/tasks")
    public PageResponse<TaskResponse> tasks(@PathVariable Long id, @RequestParam(defaultValue = "0") int page,
            @RequestParam(required = false) Integer size, @RequestParam(defaultValue = "id,asc") String sort) {
        return PageResponse.from(taskService.list(null, id, paging.of(page, size, sort, Set.of("id", "title", "status", "priority", "dueDate"))));
    }

}
