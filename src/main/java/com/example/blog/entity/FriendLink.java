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
    @Column(name = "category", length = 20)
    @Convert(converter = FriendCategoryConverter.class)
    private FriendCategory category;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private FriendLinkStatus status = FriendLinkStatus.PENDING;
} 

