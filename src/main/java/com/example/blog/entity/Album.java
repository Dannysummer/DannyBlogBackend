package com.example.blog.entity;

import lombok.Data;
import jakarta.persistence.*;
import java.util.List;

@Data
@Entity
@Table(name = "albums")
public class Album {
    @Id
    private String id;
    
    private String title;
    
    private String description;
    
    private String coverUrl;
    
    private Integer count;
    
    private String date;
    
    @OneToMany(mappedBy = "album", cascade = CascadeType.ALL)
    private List<AlbumImage> images;
} 