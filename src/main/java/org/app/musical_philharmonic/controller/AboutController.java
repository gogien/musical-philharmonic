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
            "Java 17 - опыт разработки на Java с использованием современных возможностей языка",
            "Spring Boot - создание RESTful API, работа с Spring Security, JPA/Hibernate",
            "PostgreSQL - проектирование и работа с реляционными базами данных",
            "Maven - управление зависимостями и сборка проектов",
            "JWT Authentication - реализация безопасной аутентификации",
            "HTML/CSS/JavaScript - создание интерактивного пользовательского интерфейса",
            "REST API - проектирование и реализация RESTful архитектуры",
            "Swagger/OpenAPI - документирование API"
        });
        
        // Project dates
        about.put("projectStartDate", "2024-12-01"); // Replace with actual start date
        about.put("projectEndDate", LocalDate.now().toString());
        
        return about;
    }
}

