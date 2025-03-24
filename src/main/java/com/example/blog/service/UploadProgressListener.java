package com.example.blog.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import software.amazon.awssdk.transfer.s3.progress.TransferListener;

import java.util.UUID;

/**
 * 自定义上传进度监听器，实时发送上传进度到前端
 * 
 * 注意：这是一个简化实现，在实际项目中，需要根据使用的AWS SDK版本
 * 正确实现TransferListener接口的方法
 */
public class UploadProgressListener {
    private static final Logger logger = LoggerFactory.getLogger(UploadProgressListener.class);
    
    private final SimpMessagingTemplate messagingTemplate;
    private final String uploadId;
    private final String destination;
    private final String filename;
    
    // 记录总文件大小和已传输大小
    private long totalBytes = 0;
    private long transferredBytes = 0;
    
    /**
     * 构造函数
     *
     * @param messagingTemplate WebSocket消息模板
     * @param filename 上传的文件名
     * @param username 用户名，用于构建WebSocket目标地址
     */
    public UploadProgressListener(SimpMessagingTemplate messagingTemplate, String filename, String username) {
        this.messagingTemplate = messagingTemplate;
        this.uploadId = UUID.randomUUID().toString();
        this.destination = "/topic/upload-progress/" + username;
        this.filename = filename;
        
        // 发送初始化消息
        sendProgressMessage(0, 0, "STARTED");
    }
    
    /**
     * 设置总字节数
     */
    public void setTotalBytes(long totalBytes) {
        this.totalBytes = totalBytes;
        logger.info("[上传进度] 开始上传文件: {}，上传ID: {}，总大小: {}", 
            filename, uploadId, formatFileSize(totalBytes));
        sendProgressMessage(0, totalBytes, "INITIATED");
    }
    
    /**
     * 更新已传输字节数
     */
    public void updateBytesTransferred(long bytesTransferred) {
        this.transferredBytes += bytesTransferred;
        
        double percentage = (totalBytes > 0) ? (double) transferredBytes / totalBytes * 100 : 0;
        
        // 每传输约5%记录一次日志
        if (transferredBytes % Math.max(totalBytes / 20, 1) < bytesTransferred) {
            logger.info("[上传进度] 文件: {}，已上传: {} ({}%)，总大小: {}", 
                filename, formatFileSize(transferredBytes), String.format("%.1f", percentage), 
                formatFileSize(totalBytes));
        }
        
        sendProgressMessage(transferredBytes, totalBytes, "IN_PROGRESS");
    }
    
    /**
     * 标记传输完成
     */
    public void markTransferComplete() {
        logger.info("[上传进度] 文件上传完成: {}，大小: {}，上传ID: {}", 
            filename, formatFileSize(totalBytes), uploadId);
        
        sendProgressMessage(totalBytes, totalBytes, "COMPLETED");
    }
    
    /**
     * 标记传输失败
     */
    public void markTransferFailed(Throwable exception) {
        logger.error("[上传进度] 文件上传失败: {}，已上传: {} ({}%)，上传ID: {}，错误: {}", 
            filename, formatFileSize(transferredBytes), 
            String.format("%.1f", (double) transferredBytes / totalBytes * 100),
            uploadId, exception.getMessage());
        
        sendProgressMessage(transferredBytes, totalBytes, "FAILED", exception.getMessage());
    }
    
    /**
     * 发送进度消息到WebSocket
     */
    private void sendProgressMessage(long bytesTransferred, long totalBytes, String status) {
        sendProgressMessage(bytesTransferred, totalBytes, status, null);
    }
    
    /**
     * 发送进度消息到WebSocket（包含错误信息）
     */
    private void sendProgressMessage(long bytesTransferred, long totalBytes, String status, String errorMessage) {
        double percentage = (totalBytes > 0) ? (double) bytesTransferred / totalBytes * 100 : 0;
        
        // 构建消息对象
        ProgressMessage message = new ProgressMessage();
        message.setUploadId(uploadId);
        message.setFilename(filename);
        message.setBytesTransferred(bytesTransferred);
        message.setTotalBytes(totalBytes);
        message.setPercentage(Math.min(percentage, 100.0));
        message.setStatus(status);
        message.setErrorMessage(errorMessage);
        
        // 发送消息
        messagingTemplate.convertAndSend(destination, message);
    }
    
    /**
     * 格式化文件大小
     */
    private String formatFileSize(long size) {
        if (size <= 0) return "0 B";
        final String[] units = new String[] { "B", "KB", "MB", "GB", "TB" };
        int digitGroups = (int) (Math.log10(size) / Math.log10(1024));
        return String.format("%.1f %s", size / Math.pow(1024, digitGroups), units[digitGroups]);
    }
    
    /**
     * 进度消息类
     */
    public static class ProgressMessage {
        private String uploadId;
        private String filename;
        private long bytesTransferred;
        private long totalBytes;
        private double percentage;
        private String status;
        private String errorMessage;
        
        // Getters and setters
        public String getUploadId() {
            return uploadId;
        }
        
        public void setUploadId(String uploadId) {
            this.uploadId = uploadId;
        }
        
        public String getFilename() {
            return filename;
        }
        
        public void setFilename(String filename) {
            this.filename = filename;
        }
        
        public long getBytesTransferred() {
            return bytesTransferred;
        }
        
        public void setBytesTransferred(long bytesTransferred) {
            this.bytesTransferred = bytesTransferred;
        }
        
        public long getTotalBytes() {
            return totalBytes;
        }
        
        public void setTotalBytes(long totalBytes) {
            this.totalBytes = totalBytes;
        }
        
        public double getPercentage() {
            return percentage;
        }
        
        public void setPercentage(double percentage) {
            this.percentage = percentage;
        }
        
        public String getStatus() {
            return status;
        }
        
        public void setStatus(String status) {
            this.status = status;
        }
        
        public String getErrorMessage() {
            return errorMessage;
        }
        
        public void setErrorMessage(String errorMessage) {
            this.errorMessage = errorMessage;
        }
    }
} 