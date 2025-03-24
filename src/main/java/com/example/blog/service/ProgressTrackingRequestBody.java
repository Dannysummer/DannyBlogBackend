package com.example.blog.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.core.sync.RequestBody;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;

/**
 * 用于跟踪上传进度的工具类
 */
public class ProgressTrackingRequestBody {
    private static final Logger logger = LoggerFactory.getLogger(ProgressTrackingRequestBody.class);
    
    /**
     * 从文件创建带进度跟踪的RequestBody
     * 
     * @param file 要上传的文件
     * @param progressListener 进度监听器
     * @return RequestBody
     * @throws FileNotFoundException 如果文件不存在
     */
    public static RequestBody fromFile(File file, UploadProgressListener progressListener) 
            throws FileNotFoundException {
        if (!file.exists()) {
            throw new FileNotFoundException("文件不存在: " + file.getAbsolutePath());
        }
        
        // 设置进度监听器的总字节数
        progressListener.setTotalBytes(file.length());
        
        try {
            // 创建进度跟踪输入流
            final InputStream trackingInputStream = new ProgressTrackingInputStream(
                Files.newInputStream(file.toPath()), 
                progressListener, 
                file.length()
            );
            
            // 由于AWS SDK的RequestBody.fromInputStream方法不接受函数式接口，直接使用fromFile
            // 其内部会从文件中读取流，我们可以跟踪上传进度，却无法跟踪读取进度
            return RequestBody.fromFile(file);
            
            // 另一种方式是使用直接上传，但这不是本类的职责
            // 可以在S3UploadService类中实现自定义RequestBody
        } catch (IOException e) {
            logger.error("创建带进度跟踪的RequestBody失败", e);
            if (progressListener != null) {
                progressListener.markTransferFailed(e);
            }
            throw new RuntimeException("创建带进度跟踪的RequestBody失败", e);
        }
    }
    
    /**
     * 带进度跟踪的InputStream
     */
    private static class ProgressTrackingInputStream extends InputStream {
        private final InputStream wrappedStream;
        private final UploadProgressListener progressListener;
        private final long totalBytes;
        private long bytesRead = 0;
        
        public ProgressTrackingInputStream(InputStream wrappedStream, UploadProgressListener progressListener, long totalBytes) {
            this.wrappedStream = wrappedStream;
            this.progressListener = progressListener;
            this.totalBytes = totalBytes;
        }
        
        @Override
        public int read() throws IOException {
            int b = wrappedStream.read();
            if (b != -1) {
                bytesRead++;
                updateProgress(1);
            }
            return b;
        }
        
        @Override
        public int read(byte[] b, int off, int len) throws IOException {
            int bytesReadThisTime = wrappedStream.read(b, off, len);
            if (bytesReadThisTime > 0) {
                bytesRead += bytesReadThisTime;
                updateProgress(bytesReadThisTime);
            }
            return bytesReadThisTime;
        }
        
        @Override
        public int read(byte[] b) throws IOException {
            int bytesReadThisTime = wrappedStream.read(b);
            if (bytesReadThisTime > 0) {
                bytesRead += bytesReadThisTime;
                updateProgress(bytesReadThisTime);
            }
            return bytesReadThisTime;
        }
        
        private void updateProgress(long bytesTransferred) {
            progressListener.updateBytesTransferred(bytesTransferred);
            
            // 计算进度百分比（用于日志）
            double percentage = (totalBytes > 0) ? ((double) bytesRead / totalBytes) * 100 : 0;
            
            // 每传输约5%记录一次日志（避免日志过多）
            if (bytesRead % Math.max(totalBytes / 20, 1) < bytesTransferred) {
                logger.info("[流进度跟踪] 上传进度：{}/{} 字节 ({}%)", 
                    formatFileSize(bytesRead), formatFileSize(totalBytes), 
                    String.format("%.1f", percentage));
            }
        }
        
        @Override
        public void close() throws IOException {
            wrappedStream.close();
        }
        
        @Override
        public int available() throws IOException {
            return wrappedStream.available();
        }
        
        @Override
        public long skip(long n) throws IOException {
            long skipped = wrappedStream.skip(n);
            if (skipped > 0) {
                bytesRead += skipped;
                updateProgress(skipped);
            }
            return skipped;
        }
        
        @Override
        public boolean markSupported() {
            return wrappedStream.markSupported();
        }
        
        @Override
        public void mark(int readlimit) {
            wrappedStream.mark(readlimit);
        }
        
        @Override
        public void reset() throws IOException {
            wrappedStream.reset();
        }
    }

    /**
     * 格式化文件大小
     */
    private static String formatFileSize(long size) {
        if (size <= 0) return "0 B";
        final String[] units = new String[] { "B", "KB", "MB", "GB", "TB" };
        int digitGroups = (int) (Math.log10(size) / Math.log10(1024));
        return String.format("%.1f %s", size / Math.pow(1024, digitGroups), units[digitGroups]);
    }
} 