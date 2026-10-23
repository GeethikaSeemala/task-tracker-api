package com.geethika.tasktracker.service;
import com.geethika.tasktracker.domain.*;
import com.geethika.tasktracker.dto.*;
import com.geethika.tasktracker.exception.*;
import com.geethika.tasktracker.repository.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
@ExtendWith(MockitoExtension.class)
class UserServiceTest {
    @Mock UserRepository repository;
    @Mock TaskRepository tasks;
    @InjectMocks UserService service;
    private final UserRequest request = new UserRequest(" Geethika ", "GEETHIKA@example.com", "DEVELOPER");
    @Test void createsAndNormalizes() {
        when(repository.saveAndFlush(any())).thenAnswer(i -> i.getArgument(0));
        var result = service.create(request);
        assertEquals("geethika@example.com", result.email());
        verify(repository).existsByEmail("geethika@example.com");
        var captor = ArgumentCaptor.forClass(User.class);
        verify(repository).saveAndFlush(captor.capture());
        assertEquals("Geethika", captor.getValue().getName());
    }
    @Test void rejectsDuplicateOnCreate() {
        when(repository.existsByEmail("geethika@example.com")).thenReturn(true);
        assertThrows(ConflictException.class, () -> service.create(request));
        verify(repository, never()).saveAndFlush(any());
    }
    @Test void returnsExisting() {
        var entity = new User(); entity.setName("Existing");
        when(repository.findById(1L)).thenReturn(Optional.of(entity));
        assertEquals("Existing", service.get(1L).name());
    }
    @Test void rejectsMissing() { assertThrows(NotFoundException.class, () -> service.get(99L)); }
    @Test void updatesExisting() {
        when(repository.findById(1L)).thenReturn(Optional.of(new User()));
        when(repository.saveAndFlush(any())).thenAnswer(i -> i.getArgument(0));
        assertEquals("geethika@example.com", service.update(1L, request).email());
        verify(repository).existsByEmailAndIdNot("geethika@example.com", 1L);
    }
    @Test void rejectsDuplicateOnUpdate() {
        when(repository.findById(1L)).thenReturn(Optional.of(new User()));
        when(repository.existsByEmailAndIdNot("geethika@example.com", 1L)).thenReturn(true);
        assertThrows(ConflictException.class, () -> service.update(1L, request));
        verify(repository, never()).saveAndFlush(any());
    }
    @Test void rejectsUpdateOfMissing() { assertThrows(NotFoundException.class, () -> service.update(99L, request)); }
    @Test void listsPage() {
        var pageable = PageRequest.of(0, 5);
        when(repository.findAll(pageable)).thenReturn(new PageImpl<>(List.of(new User()), pageable, 1));
        assertEquals(1, service.list(pageable).getTotalElements());
        verify(repository).findAll(pageable);
    }
    @Test void deletesUnreferenced() {
        var entity = new User();
        when(repository.findById(1L)).thenReturn(Optional.of(entity));
        service.delete(1L);
        verify(repository).delete(entity); verify(repository).flush();
    }
    @Test void rejectsReferencedDelete() {
        when(repository.findById(1L)).thenReturn(Optional.of(new User()));
        when(tasks.existsByAssigneeId(1L)).thenReturn(true);
        assertThrows(ConflictException.class, () -> service.delete(1L));
        verify(repository, never()).delete(any());
    }
    @Test void rejectsDeleteOfMissing() { assertThrows(NotFoundException.class, () -> service.delete(99L)); }
}
