package org.app.musical_philharmonic.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/about")
public class AboutController {

    @GetMapping
    public Map<String, Object> getAboutInfo() {
        Map<String, Object> about = new HashMap<>();
        
        // Author information
        about.put("authorName", "Ваше Имя"); // Replace with actual name
        about.put("group", "Ваша Группа/Учебное Заведение"); // Replace with actual group
        about.put("contactEmail", "your.email@example.com"); // Replace with actual email
        about.put("contactPhone", "+7 (XXX) XXX-XX-XX"); // Replace with actual phone
        
        // Technology experience
        about.put("technologies", new String[]{
            "Java 17 - полгода",
            "Spring Boot - 2 месяца",
            "PostgreSQL - 1 год",
            "Maven - 1 год",
            "JWT Authentication - полгода",
            "HTML/CSS/JavaScript - 2 года",
            "REST API - 1 год",
            "Swagger/OpenAPI - 1 год"
        });
        
        // Project dates
        about.put("projectStartDate", "2024-11-01"); 
        about.put("projectEndDate", LocalDate.now().toString());
        
        return about;
    }
}

