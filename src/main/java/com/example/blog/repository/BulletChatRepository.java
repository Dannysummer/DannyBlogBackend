package com.example.blog.repository;

import com.example.blog.entity.BulletChat;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface BulletChatRepository extends JpaRepository<BulletChat, Integer> {
    List<BulletChat> findByStatus(String status);
    List<BulletChat> findByOrderByCreateTimeDesc();
} 