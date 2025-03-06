package com.example.blog.repository;

import com.example.blog.entity.FriendLink;
import com.example.blog.entity.FriendCategory;
import com.example.blog.entity.FriendLinkStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FriendLinkRepository extends JpaRepository<FriendLink, Long> {
    List<FriendLink> findByCategory(FriendCategory category);
    boolean existsByUrl(String url);
    List<FriendLink> findByStatus(FriendLinkStatus status);
} 