package com.geethika.tasktracker.web;


import com.geethika.tasktracker.config.PaginationProperties;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Component;
import java.util.Set;
@Component
public class Paging {
    private final PaginationProperties properties;
    public Paging(PaginationProperties properties) { this.properties = properties; }
    public Pageable of(int page, Integer size, String sort, Set<String> allowed) {
        int resolvedSize = size == null ? properties.defaultPageSize() : size;
        if (page < 0 || resolvedSize < 1 || resolvedSize > 100) throw new IllegalArgumentException("page must be >= 0 and size between 1 and 100");
        String[] parts = sort.split(",", -1);
        if (parts.length > 2 || !allowed.contains(parts[0])) throw new IllegalArgumentException("Unsupported sort field; allowed: " + allowed);
        Sort.Direction direction;
        try { direction = parts.length == 1 ? Sort.Direction.ASC : Sort.Direction.fromString(parts[1]); }
        catch (IllegalArgumentException e) { throw new IllegalArgumentException("Sort direction must be asc or desc"); }
        var ordering = Sort.by(direction, parts[0]);
        if (!parts[0].equals("id")) ordering = ordering.and(Sort.by("id"));
        return PageRequest.of(page, resolvedSize, ordering);
    }
}
