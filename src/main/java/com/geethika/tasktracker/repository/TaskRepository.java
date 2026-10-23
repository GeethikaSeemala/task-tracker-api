package com.geethika.tasktracker.repository;


import com.geethika.tasktracker.domain.*;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
public interface TaskRepository extends JpaRepository<Task, Long> {
    boolean existsByProjectId(Long projectId);
    boolean existsByAssigneeId(Long assigneeId);
    @EntityGraph(attributePaths = {"project", "assignee"})
    @Query("select t from Task t where (:status is null or t.status = :status) and (:projectId is null or t.project.id = :projectId)")
    Page<Task> search(@Param("status") Status status, @Param("projectId") Long projectId, Pageable pageable);
}
