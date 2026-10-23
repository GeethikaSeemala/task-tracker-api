package com.geethika.tasktracker.service;
import com.geethika.tasktracker.config.TaskProperties;
import com.geethika.tasktracker.domain.*;
import com.geethika.tasktracker.dto.*;
import com.geethika.tasktracker.exception.*;
import com.geethika.tasktracker.repository.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.*;
import java.time.LocalDate;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
@ExtendWith(MockitoExtension.class)
class TaskServiceTest {
    @Mock TaskRepository tasks;
    @Mock ProjectRepository projects;
    @Mock UserRepository users;
    TaskService service;
    @BeforeEach void setup() { service = new TaskService(tasks, projects, users, new TaskProperties(Priority.HIGH)); }
    private TaskRequest request(Long assignee) { return new TaskRequest(" Implement API ", null, null, 1L, assignee, null); }
    private void projectExists() { when(projects.findById(1L)).thenReturn(Optional.of(new Project())); }
    private void saves() { when(tasks.saveAndFlush(any())).thenAnswer(i -> i.getArgument(0)); }
    @Test void usesConfiguredPriorityAndDefaultStatus() {
        projectExists(); saves();
        var result = service.create(request(null));
        assertEquals(Priority.HIGH, result.priority()); assertEquals(Status.TODO, result.status());
        assertEquals("Implement API", result.title()); assertNull(result.assigneeId());
        verifyNoInteractions(users);
    }
    @Test void honorsExplicitValuesAndAssignee() {
        projectExists(); saves();
        var user = new User(); when(users.findById(2L)).thenReturn(Optional.of(user));
        var due = LocalDate.of(2030, 1, 1);
        var result = service.create(new TaskRequest("Task", Status.IN_PROGRESS, Priority.LOW, 1L, 2L, due));
        assertEquals(Status.IN_PROGRESS, result.status()); assertEquals(Priority.LOW, result.priority()); assertEquals(due, result.dueDate());
        var captor = ArgumentCaptor.forClass(Task.class); verify(tasks).saveAndFlush(captor.capture());
        assertSame(user, captor.getValue().getAssignee());
    }
    @Test void rejectsUnknownProject() {
        assertThrows(NotFoundException.class, () -> service.create(request(null)));
        verify(tasks, never()).saveAndFlush(any());
    }
    @Test void rejectsUnknownAssignee() {
        projectExists();
        assertThrows(NotFoundException.class, () -> service.create(request(2L)));
        verify(tasks, never()).saveAndFlush(any());
    }
    @Test void updatesAndClearsAssigneeAndDueDate() {
        var task = new Task(); task.setAssignee(new User()); task.setDueDate(LocalDate.now());
        when(tasks.findById(3L)).thenReturn(Optional.of(task)); projectExists(); saves();
        var result = service.update(3L, request(null));
        assertNull(result.assigneeId()); assertNull(result.dueDate()); assertEquals("Implement API", result.title());
    }
    @Test void failedReferenceDoesNotMutateTask() {
        var task = new Task(); task.setTitle("Original");
        when(tasks.findById(3L)).thenReturn(Optional.of(task)); projectExists();
        assertThrows(NotFoundException.class, () -> service.update(3L, request(2L)));
        assertEquals("Original", task.getTitle()); verify(tasks, never()).saveAndFlush(any());
    }
    @Test void rejectsMissingGet() { assertThrows(NotFoundException.class, () -> service.get(99L)); }
    @Test void rejectsMissingUpdate() { assertThrows(NotFoundException.class, () -> service.update(99L, request(null))); }
    @Test void listsWithCombinedFilters() {
        projectExists(); var page = PageRequest.of(0, 10);
        when(tasks.search(Status.DONE, 1L, page)).thenReturn(Page.empty(page));
        assertTrue(service.list(Status.DONE, 1L, page).isEmpty()); verify(tasks).search(Status.DONE, 1L, page);
    }
    @Test void listsWithoutFilters() {
        var page = PageRequest.of(0, 10); when(tasks.search(null, null, page)).thenReturn(Page.empty(page));
        assertTrue(service.list(null, null, page).isEmpty()); verifyNoInteractions(projects);
    }
    @Test void rejectsListForUnknownProject() {
        assertThrows(NotFoundException.class, () -> service.list(null, 99L, PageRequest.of(0, 10)));
        verifyNoInteractions(tasks);
    }
    @Test void deletesExisting() {
        var task = new Task(); when(tasks.findById(3L)).thenReturn(Optional.of(task));
        service.delete(3L); verify(tasks).delete(task); verify(tasks).flush();
    }
    @Test void rejectsMissingDelete() { assertThrows(NotFoundException.class, () -> service.delete(99L)); }
}
