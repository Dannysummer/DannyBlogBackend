package com.example.blog.dto;

public class SystemStatsDTO {
    private Integer cpu;
    private Integer memory;
    private Integer disk;
    
    public SystemStatsDTO() {
    }
    
    public SystemStatsDTO(Integer cpu, Integer memory, Integer disk) {
        this.cpu = cpu;
        this.memory = memory;
        this.disk = disk;
    }
    
    public Integer getCpu() {
        return cpu;
    }
    
    public void setCpu(Integer cpu) {
        this.cpu = cpu;
    }
    
    public Integer getMemory() {
        return memory;
    }
    
    public void setMemory(Integer memory) {
        this.memory = memory;
    }
    
    public Integer getDisk() {
        return disk;
    }
    
    public void setDisk(Integer disk) {
        this.disk = disk;
    }
} 