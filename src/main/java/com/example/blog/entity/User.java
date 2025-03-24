package com.example.blog.entity;

import com.example.blog.enums.UserStatus;
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
    
    @Column(length = 255)
    private String avatar;
    
    @Transient
    private String token;
    
    private LocalDateTime createTime;
    
    private LocalDateTime lastLoginTime;
    
    @Enumerated(EnumType.STRING)
    @Column(length = 20, nullable = false)
    private UserStatus status = UserStatus.NORMAL;
    
    private String phoneNumber;
    
    private String realName;
    
    private LocalDateTime updateTime;
    
    private String updatedBy;
    
    private String remark;
    
    @PrePersist
    public void prePersist() {
        createTime = LocalDateTime.now();
        updateTime = LocalDateTime.now();
        if (status == null) {
            status = UserStatus.NORMAL;
        }
    }
    
    @PreUpdate
    public void preUpdate() {
        updateTime = LocalDateTime.now();
    }
} 