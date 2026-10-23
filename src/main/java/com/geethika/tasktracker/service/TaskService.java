package com.geethika.tasktracker.service;


import com.geethika.tasktracker.config.TaskProperties;
import com.geethika.tasktracker.domain.*;
import com.geethika.tasktracker.dto.*;
import com.geethika.tasktracker.exception.NotFoundException;
import com.geethika.tasktracker.repository.*;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class TaskService {
    private final TaskRepository tasks;
    private final ProjectRepository projects;
    private final UserRepository users;
    private final TaskProperties properties;
    public TaskService(TaskRepository tasks, ProjectRepository projects, UserRepository users, TaskProperties properties) {
        this.tasks = tasks; this.projects = projects; this.users = users; this.properties = properties;
    }
    public Page<TaskResponse> list(Status status, Long projectId, Pageable pageable) {
        if (projectId != null) requireProject(projectId);
        return tasks.search(status, projectId, pageable).map(TaskResponse::from);
    }
    public TaskResponse get(Long id) { return TaskResponse.from(require(id)); }
    @Transactional
    public TaskResponse create(TaskRequest request) {
        var task = new Task();
        apply(task, request);
        return TaskResponse.from(tasks.saveAndFlush(task));
    }
    @Transactional
    public TaskResponse update(Long id, TaskRequest request) {
        var task = require(id);
        apply(task, request);
        return TaskResponse.from(tasks.saveAndFlush(task));
    }
    @Transactional
    public void delete(Long id) { tasks.delete(require(id)); tasks.flush(); }
    private Task require(Long id) { return tasks.findById(id).orElseThrow(() -> new NotFoundException("Task " + id + " not found")); }
    private Project requireProject(Long id) { return projects.findById(id).orElseThrow(() -> new NotFoundException("Project " + id + " not found")); }
    private void apply(Task task, TaskRequest request) {
        // Resolve references first so a failed lookup does not partially mutate the entity.
        var project = requireProject(request.projectId());
        var assignee = request.assigneeId() == null ? null : users.findById(request.assigneeId())
                .orElseThrow(() -> new NotFoundException("User " + request.assigneeId() + " not found"));
        task.setTitle(request.title().trim());
        task.setStatus(request.status() == null ? Status.TODO : request.status());
        task.setPriority(request.priority() == null ? properties.defaultPriority() : request.priority());
        task.setProject(project);
        task.setAssignee(assignee);
        task.setDueDate(request.dueDate());
    }
}
