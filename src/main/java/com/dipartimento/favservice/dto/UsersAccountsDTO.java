package com.dipartimento.favservice.dto;

public class UsersAccountsDTO {
    private Long id;
    private String name;
    private String username;
    private String role;
    private String accessType;

    public UsersAccountsDTO() {}

    public UsersAccountsDTO(Long id, String name, String username, String role, String accessType) {
        this.id = id;
        this.name = name;
        this.username = username;
        this.role = role;
        this.accessType = accessType;
    }

    // getter e setter
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }

    public String getAccessType() { return accessType; }
    public void setAccessType(String accessType) { this.accessType = accessType; }
}
