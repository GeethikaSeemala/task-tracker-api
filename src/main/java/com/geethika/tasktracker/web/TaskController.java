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
@RequestMapping("/api/tasks")
public class TaskController {
    private final TaskService service;
    private final Paging paging;
    
    public TaskController(TaskService service, Paging paging) {
        this.service = service; this.paging = paging; 
    }
    @PostMapping
    public ResponseEntity<TaskResponse> create(@Valid @RequestBody TaskRequest request) {
        var result = service.create(request);
        return ResponseEntity.created(URI.create("/api/tasks/" + result.id())).body(result);
    }
    @GetMapping
    public PageResponse<TaskResponse> list(@RequestParam(defaultValue = "0") int page, @RequestParam(required = false) Integer size,
            @RequestParam(defaultValue = "id,asc") String sort, @RequestParam(required = false) Status status, @RequestParam(required = false) Long projectId) {
        return PageResponse.from(service.list(status, projectId, paging.of(page, size, sort, Set.of("id", "title", "status", "priority", "dueDate"))));
    }
    @GetMapping("/{id}")
    public TaskResponse get(@PathVariable Long id) { return service.get(id); }
    @PutMapping("/{id}")
    public TaskResponse update(@PathVariable Long id, @Valid @RequestBody TaskRequest request) { return service.update(id, request); }
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) { service.delete(id); return ResponseEntity.noContent().build(); }

}
