package com.example.blog.service;

import com.example.blog.entity.Progress;
import com.example.blog.repository.ProgressRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class ProgressService {
    
    @Autowired
    private ProgressRepository progressRepository;
    
    public List<Progress> getAllProgress() {
        return progressRepository.findAll();
    }
    
    public Progress createProgress(Progress progress) {
        return progressRepository.save(progress);
    }
    
    public Progress updateProgress(Long id, Progress progress) {
        Optional<Progress> existingProgress = progressRepository.findById(id);
        if (existingProgress.isPresent()) {
            Progress progressToUpdate = existingProgress.get();
            progressToUpdate.setText(progress.getText());
            progressToUpdate.setCompleted(progress.getCompleted());
            return progressRepository.save(progressToUpdate);
        }
        return null;
    }
    
    public boolean deleteProgress(Long id) {
        if (progressRepository.existsById(id)) {
            progressRepository.deleteById(id);
            return true;
        }
        return false;
    }
} 