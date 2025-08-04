package com.example.blog.entity;

import lombok.Data;
import jakarta.persistence.*;
import jakarta.persistence.Transient;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonIgnore;

@Data
@Entity
@Table(name = "friend_links_pending")
public class FriendLinkPending {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    private String avatar;
    
    private String cover;
    
    private String name;
    
    private String description;
    
    private String url;
    
    private String delay;

    @Column(name = "pending_email", nullable = false)
    @JsonProperty("pendingEmail")
    private String pendingEmail;

    @Column(nullable = false)
    @Convert(converter = FriendLinkPendingStatusConverter.class)
    private FriendLinkPendingStatus status = FriendLinkPendingStatus.PENDING;
    
    @Column(name = "category", length = 20)
    @Convert(converter = FriendCategoryConverter.class)
    @JsonProperty("category")
    private FriendCategory category = FriendCategory.FRIEND;
}
