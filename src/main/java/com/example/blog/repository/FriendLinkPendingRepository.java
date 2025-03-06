package com.example.blog.repository;

import com.example.blog.entity.FriendLinkPending;
import com.example.blog.entity.FriendLinkPendingStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FriendLinkPendingRepository extends JpaRepository<FriendLinkPending, Long> {
    boolean existsByUrl(String url);
    List<FriendLinkPending> findByStatus(FriendLinkPendingStatus status);
} 