package com.example.blog.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "users")
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(unique = true)
    private String username;
    
    private String password;
    
    @Column(unique = true)
    private String email;
    
    @Column(length = 20)
    private String role;
    
    @Transient
    private String token;
    
    private LocalDateTime createTime;
    
    private LocalDateTime lastLoginTime;
    
    @PrePersist
    public void prePersist() {
        createTime = LocalDateTime.now();
    }
} 