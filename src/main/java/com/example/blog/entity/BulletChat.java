package com.example.blog.entity;

import lombok.Data;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "bullet_chat")
public class BulletChat {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "bulletId")
    private Integer bulletId;
    
    @Column(name = "content")
    private String content;
    
    @Column(name = "avatar")
    private String avatar;
    
    @Column(name = "cratetime")
    private LocalDateTime createTime;
    
    @Column(name = "status")
    private String status;
    
    @PrePersist
    public void prePersist() {
        if (createTime == null) {
            createTime = LocalDateTime.now();
        }
        if (status == null) {
            status = "active";
        }
    }
} 