package com.geethika.tasktracker.web;
import com.geethika.tasktracker.config.PaginationProperties;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Sort;
import java.util.Set;
import static org.junit.jupiter.api.Assertions.*;
class PagingTest {
    private final Paging paging = new Paging(new PaginationProperties(7));
    private final Set<String> allowed = Set.of("id", "title");
    @Test void usesConfigAndStableOrdering() {
        var result = paging.of(0, null, "title,desc", allowed);
        assertEquals(7, result.getPageSize());
        assertEquals(Sort.Direction.DESC, result.getSort().getOrderFor("title").getDirection());
        assertNotNull(result.getSort().getOrderFor("id"));
    }
    @Test void validatesPaginationAndSort() {
        assertThrows(IllegalArgumentException.class, () -> paging.of(-1, 5, "id", allowed));
        assertThrows(IllegalArgumentException.class, () -> paging.of(0, 0, "id", allowed));
        assertThrows(IllegalArgumentException.class, () -> paging.of(0, 101, "id", allowed));
        assertThrows(IllegalArgumentException.class, () -> paging.of(0, 5, "password", allowed));
        assertThrows(IllegalArgumentException.class, () -> paging.of(0, 5, "id,sideways", allowed));
        assertThrows(IllegalArgumentException.class, () -> paging.of(0, 5, "id,asc,extra", allowed));
    }
}
