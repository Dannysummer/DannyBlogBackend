package com.example.blog.entity;

import lombok.Data;
import jakarta.persistence.*;

@Data
@Entity
@Table(name = "album_images")
public class AlbumImage {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    private String url;
    
    private String title;
    
    private String description;
    
    @ManyToOne
    @JoinColumn(name = "album_id")
    private Album album;
} 