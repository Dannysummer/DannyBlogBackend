package com.example.blog.entity;

import lombok.Data;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "essays")
public class Essay {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(columnDefinition = "TEXT")
    private String content;
    
    private LocalDateTime date;
    
    @Embedded
    private Weather weather;
    
    private String author;
    
    private String avatar;
    
    private Integer likes;
    
    private Boolean isLiked;
}

@Embeddable
@Data
class Weather {
    @Enumerated(EnumType.STRING)
    private WeatherType type;
    
    private Integer temperature;
}

enum WeatherType {
    SUNNY, CLOUDY, RAINY, SNOWY, WINDY
} 