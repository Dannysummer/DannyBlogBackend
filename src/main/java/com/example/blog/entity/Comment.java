package com.example.blog.entity;

import lombok.Data;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Entity
@Table(name = "comments")
public class Comment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    private Integer floor;
    
    private String nickname;
    
    private String avatar;
    
    @Column(columnDefinition = "TEXT")
    private String content;
    
    private LocalDateTime time;
    
    private Integer likes;
    
    private String image;
    
    private Boolean isLiked;
    
    @ManyToOne
    @JoinColumn(name = "parent_id")
    private Comment parent;
    
    @OneToMany(mappedBy = "parent", cascade = CascadeType.ALL)
    private List<Comment> replies;
} 