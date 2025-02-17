package com.example.blog.entity;

import lombok.Data;
import jakarta.persistence.*;

@Data
@Entity
@Table(name = "friend_links")
public class FriendLink {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    private String avatar;
    
    private String cover;
    
    private String name;
    
    private String description;
    
    private String url;
    
    private String delay;
    
    @Enumerated(EnumType.STRING)
    private FriendCategory category;
}

enum FriendCategory {
    BIGSHOT, CLOSE, FRIEND, TECH
} 