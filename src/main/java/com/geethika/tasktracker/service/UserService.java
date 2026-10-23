package com.geethika.tasktracker.service;


import com.geethika.tasktracker.domain.User;
import com.geethika.tasktracker.dto.*;
import com.geethika.tasktracker.exception.*;
import com.geethika.tasktracker.repository.*;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.Locale;

@Service
@Transactional(readOnly = true)
public class UserService {
    private final UserRepository repository;
    private final TaskRepository tasks;
    public UserService(UserRepository repository, TaskRepository tasks) { this.repository = repository; this.tasks = tasks; }
    public Page<UserResponse> list(Pageable pageable) { return repository.findAll(pageable).map(UserResponse::from); }
    public UserResponse get(Long id) { return UserResponse.from(require(id)); }
    private User require(Long id) { return repository.findById(id).orElseThrow(() -> new NotFoundException("User " + id + " not found")); }
    @Transactional
    public UserResponse create(UserRequest request) {
        if (repository.existsByEmail(request.email().trim().toLowerCase(Locale.ROOT))) throw new ConflictException("User email already exists");
        var entity = new User();
        apply(entity, request);
        return UserResponse.from(repository.saveAndFlush(entity));
    }
    @Transactional
    public UserResponse update(Long id, UserRequest request) {
        var entity = require(id);
        if (repository.existsByEmailAndIdNot(request.email().trim().toLowerCase(Locale.ROOT), id)) throw new ConflictException("User email already exists");
        apply(entity, request);
        return UserResponse.from(repository.saveAndFlush(entity));
    }
    @Transactional
    public void delete(Long id) {
        var entity = require(id);
        if (tasks.existsByAssigneeId(id)) throw new ConflictException("Remove task assignments before deleting this user");
        repository.delete(entity);
        repository.flush();
    }
    private void apply(User entity, UserRequest request) {
        entity.setName(request.name().trim());
        entity.setEmail(request.email().trim().toLowerCase(Locale.ROOT));
        entity.setRole(request.role());
    }
}
