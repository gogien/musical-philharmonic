package org.app.musical_philharmonic.dto;

import jakarta.validation.constraints.Size;
import org.app.musical_philharmonic.entity.Role;

public class UserUpdateRequest {
    private String name;
    @Size(max = 20)
    private String phone;
    private Role role;
    @Size(min = 6, max = 100)
    private String password;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public Role getRole() {
        return role;
    }

    public void setRole(Role role) {
        this.role = role;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}

