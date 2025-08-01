package com.example.blog.util;

import org.springframework.stereotype.Component;

/**
 * 雪花算法ID生成器
 * 
 * 结构：
 * 1位符号位 + 41位时间戳 + 10位工作机器ID（5位数据中心ID + 5位机器ID） + 12位序列号
 */
@Component
public class SnowflakeIdGenerator {

    // 起始时间戳（2023-01-01 00:00:00）
    private final long startTimestamp = 1672531200000L;
    
    // 数据中心ID所占位数
    private final long dataCenterIdBits = 5L;
    
    // 机器ID所占位数
    private final long workerIdBits = 5L;
    
    // 序列号所占位数
    private final long sequenceBits = 12L;
    
    // 数据中心ID最大值
    private final long maxDataCenterId = -1L ^ (-1L << dataCenterIdBits);
    
    // 机器ID最大值
    private final long maxWorkerId = -1L ^ (-1L << workerIdBits);
    
    // 序列号掩码（4095）
    private final long sequenceMask = -1L ^ (-1L << sequenceBits);
    
    // 机器ID左移位数
    private final long workerIdShift = sequenceBits;
    
    // 数据中心ID左移位数
    private final long dataCenterIdShift = sequenceBits + workerIdBits;
    
    // 时间戳左移位数
    private final long timestampShift = sequenceBits + workerIdBits + dataCenterIdBits;
    
    // 数据中心ID
    private long dataCenterId;
    
    // 机器ID
    private long workerId;
    
    // 序列号
    private long sequence = 0L;
    
    // 上次生成ID的时间戳
    private long lastTimestamp = -1L;
    
    /**
     * 构造函数
     * 
     * @param dataCenterId 数据中心ID (0~31)
     * @param workerId 机器ID (0~31)
     */
    public SnowflakeIdGenerator(long dataCenterId, long workerId) {
        // 检查数据中心ID和机器ID的合法性
        if (dataCenterId > maxDataCenterId || dataCenterId < 0) {
            throw new IllegalArgumentException("数据中心ID不能大于" + maxDataCenterId + "或小于0");
        }
        if (workerId > maxWorkerId || workerId < 0) {
            throw new IllegalArgumentException("机器ID不能大于" + maxWorkerId + "或小于0");
        }
        
        this.dataCenterId = dataCenterId;
        this.workerId = workerId;
    }
    
    /**
     * 默认构造函数，使用默认的数据中心ID和机器ID
     */
    public SnowflakeIdGenerator() {
        this(1, 1);
    }
    
    /**
     * 生成下一个ID
     * 
     * @return 唯一ID
     */
    public synchronized long nextId() {
        long timestamp = getCurrentTimestamp();
        
        // 如果当前时间小于上一次ID生成的时间戳，说明系统时钟回退过，抛出异常
        if (timestamp < lastTimestamp) {
            throw new RuntimeException("时钟向后移动，拒绝生成ID");
        }
        
        // 如果是同一时间生成的，则进行序列号递增
        if (timestamp == lastTimestamp) {
            sequence = (sequence + 1) & sequenceMask;
            // 序列号溢出，等待下一毫秒
            if (sequence == 0) {
                timestamp = waitNextMillis(lastTimestamp);
            }
        } else {
            // 时间戳改变，序列号重置
            sequence = 0L;
        }
        
        // 保存上次生成ID的时间戳
        lastTimestamp = timestamp;
        
        // 组合生成ID
        return ((timestamp - startTimestamp) << timestampShift) |
               (dataCenterId << dataCenterIdShift) |
               (workerId << workerIdShift) |
               sequence;
    }
    
    /**
     * 等待下一个毫秒到来
     * 
     * @param lastTimestamp 上次生成ID的时间戳
     * @return 新的时间戳
     */
    private long waitNextMillis(long lastTimestamp) {
        long timestamp = getCurrentTimestamp();
        while (timestamp <= lastTimestamp) {
            timestamp = getCurrentTimestamp();
        }
        return timestamp;
    }
    
    /**
     * 获取当前时间戳
     * 
     * @return 当前时间戳（毫秒）
     */
    private long getCurrentTimestamp() {
        return System.currentTimeMillis();
    }
} 