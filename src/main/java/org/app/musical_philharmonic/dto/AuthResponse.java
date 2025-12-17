package org.app.musical_philharmonic.dto;

import org.app.musical_philharmonic.entity.Role;

public class AuthResponse {
    private String token;
    private String name;
    private Role role;

    public AuthResponse() {
    }

    public AuthResponse(String token) {
        this.token = token;
    }

    public AuthResponse(String token, String name) {
        this.token = token;
        this.name = name;
    }

    public AuthResponse(String token, String name, Role role) {
        this.token = token;
        this.name = name;
        this.role = role;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Role getRole() {
        return role;
    }

    public void setRole(Role role) {
        this.role = role;
    }
}

