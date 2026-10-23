package com.geethika.tasktracker.service;


import com.geethika.tasktracker.domain.Project;
import com.geethika.tasktracker.dto.*;
import com.geethika.tasktracker.exception.*;
import com.geethika.tasktracker.repository.*;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.Locale;

@Service
@Transactional(readOnly = true)
public class ProjectService {
    private final ProjectRepository repository;
    private final TaskRepository tasks;
    public ProjectService(ProjectRepository repository, TaskRepository tasks) { this.repository = repository; this.tasks = tasks; }
    public Page<ProjectResponse> list(Pageable pageable) { return repository.findAll(pageable).map(ProjectResponse::from); }
    public ProjectResponse get(Long id) { return ProjectResponse.from(require(id)); }
    private Project require(Long id) { return repository.findById(id).orElseThrow(() -> new NotFoundException("Project " + id + " not found")); }
    @Transactional
    public ProjectResponse create(ProjectRequest request) {
        if (repository.existsByName(request.name().trim())) throw new ConflictException("Project name already exists");
        var entity = new Project();
        apply(entity, request);
        return ProjectResponse.from(repository.saveAndFlush(entity));
    }
    @Transactional
    public ProjectResponse update(Long id, ProjectRequest request) {
        var entity = require(id);
        if (repository.existsByNameAndIdNot(request.name().trim(), id)) throw new ConflictException("Project name already exists");
        apply(entity, request);
        return ProjectResponse.from(repository.saveAndFlush(entity));
    }
    @Transactional
    public void delete(Long id) {
        var entity = require(id);
        if (tasks.existsByProjectId(id)) throw new ConflictException("Remove project tasks before deleting this project");
        repository.delete(entity);
        repository.flush();
    }
    private void apply(Project entity, ProjectRequest request) {
        entity.setName(request.name().trim());
        entity.setDescription(request.description());
    }
}
