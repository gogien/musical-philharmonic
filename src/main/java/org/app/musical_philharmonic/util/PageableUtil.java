package org.app.musical_philharmonic.util;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

public class PageableUtil {
    
    public static Pageable toPageable(Integer page, Integer size, String sort) {
        if (page == null) page = 0;
        if (size == null) size = 20;
        
        if (sort == null || sort.isEmpty()) {
            return PageRequest.of(page, size);
        }
        
        String[] parts = sort.split(",");
        if (parts.length == 2) {
            String property = parts[0].trim();
            String direction = parts[1].trim().toUpperCase();
            Sort.Direction dir = direction.equals("DESC") ? Sort.Direction.DESC : Sort.Direction.ASC;
            return PageRequest.of(page, size, Sort.by(dir, property));
        }
        
        return PageRequest.of(page, size);
    }
}

