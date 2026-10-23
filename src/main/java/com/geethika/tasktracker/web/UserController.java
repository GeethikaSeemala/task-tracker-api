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
@RequestMapping("/api/users")
public class UserController {
    private final UserService service;
    private final Paging paging;
    
    public UserController(UserService service, Paging paging) {
        this.service = service; this.paging = paging; 
    }
    @PostMapping
    public ResponseEntity<UserResponse> create(@Valid @RequestBody UserRequest request) {
        var result = service.create(request);
        return ResponseEntity.created(URI.create("/api/users/" + result.id())).body(result);
    }
    @GetMapping
    public PageResponse<UserResponse> list(@RequestParam(defaultValue = "0") int page, @RequestParam(required = false) Integer size,
            @RequestParam(defaultValue = "id,asc") String sort) {
        return PageResponse.from(service.list(paging.of(page, size, sort, Set.of("id", "name", "email", "role"))));
    }
    @GetMapping("/{id}")
    public UserResponse get(@PathVariable Long id) { return service.get(id); }
    @PutMapping("/{id}")
    public UserResponse update(@PathVariable Long id, @Valid @RequestBody UserRequest request) { return service.update(id, request); }
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) { service.delete(id); return ResponseEntity.noContent().build(); }

}
