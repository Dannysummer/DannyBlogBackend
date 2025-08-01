package com.example.blog.service;

import com.example.blog.dto.HeatmapDTO;
import com.example.blog.dto.LatestArticleDTO;
import com.example.blog.dto.StatisticsDTO;
import com.example.blog.dto.SystemStatsDTO;
import com.example.blog.entity.Article;
import com.example.blog.repository.ArticleRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.management.ManagementFactory;
import java.lang.management.OperatingSystemMXBean;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import javax.sql.DataSource;

@Service
public class DashboardService {
    
    private static final Logger logger = LoggerFactory.getLogger(DashboardService.class);
    private static final int COMMAND_TIMEOUT_SECONDS = 5;

    @Autowired
    private ArticleRepository articleRepository;
    
    @Autowired
    private DataSource dataSource;

    /**
     * 获取系统状态
     */
    public SystemStatsDTO getSystemStats() {
        // 尝试使用传统方法获取系统状态
        int cpuUsage = getCpuUsage();
        int memoryUsage = getMemoryUsage();
        int diskUsage = getDiskUsage();
            
        logger.info("系统状态 - CPU: {}%, 内存: {}%, 磁盘: {}%", 
                 cpuUsage, memoryUsage, diskUsage);
            
        return new SystemStatsDTO(cpuUsage, memoryUsage, diskUsage);
    }
    
    /**
     * 执行命令并获取结果，带超时控制
     * @param cmd 要执行的命令
     * @return 命令输出结果
     */
    private List<String> executeCommandWithTimeout(String[] cmd) {
        List<String> output = new ArrayList<>();
        Process process = null;
        try {
            ProcessBuilder pb = new ProcessBuilder(cmd);
            pb.redirectErrorStream(true); // 合并标准错误和标准输出
            process = pb.start();
            
            // 设置超时
            boolean completed = process.waitFor(COMMAND_TIMEOUT_SECONDS, TimeUnit.SECONDS);
            if (!completed) {
                logger.warn("命令执行超时: {}", Arrays.toString(cmd));
                process.destroyForcibly();
                return output;
            }
            
            // 读取输出
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    output.add(line);
                }
            }
        } catch (IOException | InterruptedException e) {
            logger.error("执行命令失败: {}", Arrays.toString(cmd), e);
            if (process != null) {
                process.destroyForcibly();
            }
        }
        return output;
    }
    
    /**
     * 获取CPU使用率
     */
    private int getCpuUsage() {
        try {
            String osName = System.getProperty("os.name").toLowerCase();
            logger.debug("操作系统: {}", osName);
            
            if (osName.contains("win")) {
                // 使用JMX API获取CPU使用率 (Windows)
                try {
                    com.sun.management.OperatingSystemMXBean sunOsBean = 
                            (com.sun.management.OperatingSystemMXBean) ManagementFactory.getOperatingSystemMXBean();
                    
                    double systemCpuLoad = sunOsBean.getCpuLoad() * 100;
                    if (systemCpuLoad >= 0) {
                        logger.debug("JMX API CPU负载: {}%", systemCpuLoad);
                        return (int) Math.round(systemCpuLoad);
                    }
                } catch (Exception e) {
                    logger.debug("JMX API获取CPU负载失败", e);
                }
                
                // Windows系统 - 使用PowerShell获取CPU使用率
                List<String> output = executeCommandWithTimeout(new String[]{
                    "powershell", 
                    "-Command", 
                    "Get-WmiObject Win32_Processor | Measure-Object -Property LoadPercentage -Average | Select-Object -ExpandProperty Average"
                });
                
                if (!output.isEmpty()) {
                    String line = output.get(0).trim();
                    logger.debug("PowerShell CPU结果: {}", line);
                    try {
                        return Integer.parseInt(line);
                    } catch (NumberFormatException e) {
                        logger.error("无法解析CPU负载: {}", line, e);
                    }
                }
                
                // 尝试wmic命令
                output = executeCommandWithTimeout(new String[]{"wmic", "cpu", "get", "LoadPercentage"});
                if (output.size() > 1) {
                    String line = output.get(1).trim(); // 跳过标题行
                    logger.debug("WMIC CPU结果: {}", line);
                    try {
                        return Integer.parseInt(line);
                    } catch (NumberFormatException e) {
                        logger.error("无法解析CPU负载: {}", line, e);
                    }
                }
            } else {
                // Linux/Unix系统
                List<String> output = executeCommandWithTimeout(new String[]{"sh", "-c", "top -bn1 | grep 'Cpu(s)'"});
                if (!output.isEmpty()) {
                    String line = output.get(0);
                    logger.debug("Linux CPU结果: {}", line);
                    if (line.contains("id")) {
                        // 尝试提取空闲百分比并计算使用率
                        try {
                            int idleIndex = line.indexOf("id");
                            if (idleIndex > 0) {
                                // 从idleIndex向前找数字
                                int startIndex = idleIndex;
                                while (startIndex > 0 && 
                                      (Character.isDigit(line.charAt(startIndex - 1)) || 
                                       line.charAt(startIndex - 1) == '.' || 
                                       Character.isWhitespace(line.charAt(startIndex - 1)))) {
                                    startIndex--;
                                }
                                String idleStr = line.substring(startIndex, idleIndex).trim();
                                float idle = Float.parseFloat(idleStr);
                                return Math.round(100 - idle);
                            }
                        } catch (Exception e) {
                            logger.error("解析Linux CPU使用率失败", e);
                        }
                    }
                }
                
                // 尝试另一种方法
                output = executeCommandWithTimeout(new String[]{"sh", "-c", "vmstat 1 2 | tail -1 | awk '{print 100-$15}'"});
                if (!output.isEmpty()) {
                    try {
                        return Integer.parseInt(output.get(0).trim());
                    } catch (NumberFormatException e) {
                        logger.error("解析vmstat CPU使用率失败", e);
                    }
                }
            }
            
            // 使用Java内置方法计算近似CPU负载
            logger.warn("所有系统命令失败，使用Java API估算CPU使用率");
            
            // 尝试获取系统CPU负载
            try {
                com.sun.management.OperatingSystemMXBean sunOsBean = 
                        (com.sun.management.OperatingSystemMXBean) ManagementFactory.getOperatingSystemMXBean();
                
                double cpuLoad = sunOsBean.getSystemCpuLoad();
                if (cpuLoad >= 0) {
                    return (int) Math.round(cpuLoad * 100);
                }
                
                // 尝试进程CPU负载
                cpuLoad = sunOsBean.getProcessCpuLoad();
                if (cpuLoad >= 0) {
                    return (int) Math.round(cpuLoad * 100);
                }
            } catch (Exception e) {
                logger.debug("sun管理API获取CPU失败", e);
            }
            
            // 尝试标准API
            OperatingSystemMXBean osBean = ManagementFactory.getOperatingSystemMXBean();
            double loadAvg = osBean.getSystemLoadAverage();
            if (loadAvg >= 0) {
                int processors = Runtime.getRuntime().availableProcessors();
                double usage = (loadAvg / processors) * 100;
                return (int) Math.min(100, Math.round(usage));
            }
            
            // 最后的手段：使用线程数估算
            int threadCount = ManagementFactory.getThreadMXBean().getThreadCount();
            int processors = Runtime.getRuntime().availableProcessors();
            double load = ((double) threadCount / processors) * 25; // 基于经验的粗略估计
            return (int) Math.min(100, Math.round(load));
        } catch (Exception e) {
            logger.error("获取CPU使用率失败", e);
            return 50; // 返回中等负载作为默认值
        }
    }
    
    /**
     * 获取内存使用率
     */
    private int getMemoryUsage() {
        try {
            String osName = System.getProperty("os.name").toLowerCase();
            logger.debug("获取内存使用率 - 操作系统: {}", osName);
            
            // 首先尝试通用的JMX API方法
            try {
                com.sun.management.OperatingSystemMXBean sunOsBean = 
                        (com.sun.management.OperatingSystemMXBean) ManagementFactory.getOperatingSystemMXBean();
                
                long totalPhysicalMemory = sunOsBean.getTotalPhysicalMemorySize();
                long freePhysicalMemory = sunOsBean.getFreePhysicalMemorySize();
                
                if (totalPhysicalMemory > 0) {
                    // 使用安全的计算方法，避免整数溢出
                    double usedPercentage = (1.0 - ((double) freePhysicalMemory / totalPhysicalMemory)) * 100;
                    logger.debug("JMX API内存: total={}, free={}, 使用率={}%", 
                            totalPhysicalMemory, freePhysicalMemory, usedPercentage);
                    return (int) Math.round(usedPercentage);
                }
            } catch (Exception e) {
                logger.debug("JMX API获取内存使用率失败", e);
            }
            
            // 操作系统特定的命令
            if (osName.contains("win")) {
                // Windows系统
                List<String> output = executeCommandWithTimeout(new String[]{
                    "powershell", 
                    "-Command", 
                    "(Get-Counter '\\Memory\\% Committed Bytes In Use').CounterSamples.CookedValue"
                });
                
                if (!output.isEmpty()) {
                    try {
                        double memoryUsage = Double.parseDouble(output.get(0).trim());
                        logger.debug("PowerShell内存使用率: {}%", memoryUsage);
                        return (int) Math.round(memoryUsage);
                    } catch (NumberFormatException e) {
                        logger.error("解析PowerShell内存使用率失败", e);
                    }
                }
                
                // 备用方法：使用wmic
                output = executeCommandWithTimeout(new String[]{
                    "wmic", "OS", "get", "FreePhysicalMemory,TotalVisibleMemorySize"
                });
                
                if (output.size() > 1) {
                    try {
                        String[] parts = output.get(1).trim().split("\\s+");
                        if (parts.length >= 2) {
                            long free = Long.parseLong(parts[0]);
                            long total = Long.parseLong(parts[1]);
                            
                            if (total > 0) {
                                double usedPercentage = (1.0 - ((double) free / total)) * 100;
                                logger.debug("WMIC内存: total={}, free={}, 使用率={}%", 
                                        total, free, usedPercentage);
                                return (int) Math.round(usedPercentage);
                            }
                        }
                    } catch (Exception e) {
                        logger.error("解析WMIC内存输出失败", e);
                    }
                }
            } else {
                // Linux/Unix系统
                List<String> output = executeCommandWithTimeout(new String[]{"sh", "-c", "free | grep Mem:"});
                if (!output.isEmpty()) {
                    try {
                        String[] parts = output.get(0).trim().split("\\s+");
                        if (parts.length >= 4) { // Mem: total used free
                            long total = Long.parseLong(parts[1]);
                            long used = Long.parseLong(parts[2]);
                            
                            if (total > 0) {
                                double usedPercentage = ((double) used / total) * 100;
                                logger.debug("Linux内存: total={}, used={}, 使用率={}%", 
                                        total, used, usedPercentage);
                                return (int) Math.round(usedPercentage);
                            }
                        }
                    } catch (Exception e) {
                        logger.error("解析Linux内存输出失败", e);
                    }
                }
            }
            
            // 备用: 使用Runtime内存信息 (仅JVM内存，非系统内存)
            logger.warn("所有系统命令失败，使用Runtime获取JVM内存使用率");
            Runtime runtime = Runtime.getRuntime();
            long maxMemory = runtime.maxMemory();
            long totalMemory = runtime.totalMemory();
            long freeMemory = runtime.freeMemory();
            
            long usedMemory = totalMemory - freeMemory;
            
            if (maxMemory > 0) {
                double usedPercentage = ((double) usedMemory / maxMemory) * 100;
                logger.debug("JVM内存: max={}, used={}, 使用率={}%", 
                        maxMemory, usedMemory, usedPercentage);
                return (int) Math.round(usedPercentage);
            }
            
            return 60; // 默认值
        } catch (Exception e) {
            logger.error("获取内存使用率失败", e);
            return 60;
        }
    }
    
    /**
     * 获取磁盘使用率
     */
    private int getDiskUsage() {
        try {
            String osName = System.getProperty("os.name").toLowerCase();
            logger.debug("获取磁盘使用率 - 操作系统: {}", osName);
            
            if (osName.contains("win")) {
                // Windows系统 - 优先使用Java API (最可靠的方法)
                logger.debug("使用Java API获取磁盘使用率");
                File[] roots = File.listRoots();
                if (roots.length > 0) {
                    long totalSpace = 0;
                    long freeSpace = 0;
                    
                    for (File root : roots) {
                        try {
                            // 检查磁盘是否可访问
                            if (root.exists() && root.getTotalSpace() > 0) {
                                logger.debug("检查磁盘: {}", root.getAbsolutePath());
                                totalSpace += root.getTotalSpace();
                                freeSpace += root.getFreeSpace();
                            }
                        } catch (Exception e) {
                            logger.debug("访问磁盘信息失败: {}", root.getAbsolutePath(), e);
                        }
                    }
                    
                    if (totalSpace > 0) {
                        // 使用安全的计算方法，避免整数溢出
                        double usedPercentage = (1.0 - ((double) freeSpace / totalSpace)) * 100;
                        logger.debug("磁盘使用率: {}%", usedPercentage);
                        return (int) Math.round(usedPercentage);
                    }
                }
                
                // 备用方法: 使用简化版PowerShell命令 (避免复杂脚本和本地化问题)
                List<String> output = executeCommandWithTimeout(new String[]{
                    "powershell", 
                    "-Command",
                    "Get-PSDrive -PSProvider FileSystem | ForEach-Object {if($_.Used -ne $null -and $_.Free -ne $null) {$_.Used / ($_.Used + $_.Free) * 100}}"
                });
                
                // 尝试处理所有数字输出并计算平均值
                if (!output.isEmpty()) {
                    double total = 0;
                    int count = 0;
                    
                    for (String line : output) {
                        try {
                            if (!line.trim().isEmpty()) {
                                double value = Double.parseDouble(line.trim());
                                if (value >= 0 && value <= 100) {
                                    total += value;
                                    count++;
                                }
                            }
                        } catch (NumberFormatException e) {
                            logger.debug("跳过无效的PowerShell输出行: {}", line);
                        }
                    }
                    
                    if (count > 0) {
                        double average = total / count;
                        logger.debug("PowerShell磁盘使用率 (平均): {}%", average);
                        return (int) Math.round(average);
                    }
                }
                
                // 尝试wmic命令 (适用于旧版Windows)
                output = executeCommandWithTimeout(new String[]{
                    "wmic", "logicaldisk", "get", "size,freespace,caption"
                });
                
                if (output.size() > 1) {
                    try {
                        long totalSize = 0;
                        long totalFree = 0;
                        
                        // 跳过标题行
                        for (int i = 1; i < output.size(); i++) {
                            String line = output.get(i).trim();
                            String[] parts = line.split("\\s+");
                            if (parts.length >= 3) {
                                try {
                                    long free = Long.parseLong(parts[0]);
                                    long size = Long.parseLong(parts[1]);
                                    if (size > 0) {
                                        totalSize += size;
                                        totalFree += free;
                                    }
                                } catch (NumberFormatException e) {
                                    // 忽略无法解析的行
                                    logger.debug("无法解析WMIC行: {}", line);
                                }
                            }
                        }
                        
                        if (totalSize > 0) {
                            double usedPercentage = (1.0 - ((double) totalFree / totalSize)) * 100;
                            logger.debug("WMIC磁盘使用率: {}%", usedPercentage);
                            return (int) Math.round(usedPercentage);
                        }
                    } catch (Exception e) {
                        logger.debug("解析WMIC输出失败", e);
                    }
                }
                
                // 尝试获取系统盘使用率
                File systemDrive = new File(System.getenv("SystemDrive") + "\\");
                if (systemDrive.exists() && systemDrive.getTotalSpace() > 0) {
                    long totalSpace = systemDrive.getTotalSpace();
                    long freeSpace = systemDrive.getFreeSpace();
                    double usedPercentage = (1.0 - ((double) freeSpace / totalSpace)) * 100;
                    logger.debug("系统盘使用率: {}%", usedPercentage);
                    return (int) Math.round(usedPercentage);
                }
            } else {
                // Linux/Unix系统
                List<String> output = executeCommandWithTimeout(new String[]{"sh", "-c", "df -h / | tail -1"});
                if (!output.isEmpty()) {
                    try {
                        String[] parts = output.get(0).trim().split("\\s+");
                        // 典型输出: Filesystem Size Used Avail Use% Mounted
                        if (parts.length >= 5) {
                            String usageStr = parts[4].replace("%", "");
                            return Integer.parseInt(usageStr);
                        }
                    } catch (Exception e) {
                        logger.error("解析Linux df输出失败", e);
                    }
                }
                
                // 备用: Java File API获取根目录使用率
                File root = new File("/");
                if (root.exists() && root.getTotalSpace() > 0) {
                    long totalSpace = root.getTotalSpace();
                    long freeSpace = root.getFreeSpace();
                    double usedPercentage = (1.0 - ((double) freeSpace / totalSpace)) * 100;
                    return (int) Math.round(usedPercentage);
                }
            }
            
            // 通用备用方法
            logger.warn("使用通用方法获取磁盘使用率");
            File file = new File(System.getProperty("user.dir"));
            if (file.exists()) {
                File disk = file;
                
                // 尝试获取根目录
                while (disk.getParentFile() != null) {
                    disk = disk.getParentFile();
                }
                
                if (disk.getTotalSpace() > 0) {
                    long totalSpace = disk.getTotalSpace();
                    long freeSpace = disk.getFreeSpace();
                    double usedPercentage = (1.0 - ((double) freeSpace / totalSpace)) * 100;
                    logger.debug("当前路径磁盘使用率: {}%", usedPercentage);
                    return (int) Math.round(usedPercentage);
                }
            }
            
            return 50; // 默认值
        } catch (Exception e) {
            logger.error("获取磁盘使用率失败", e);
            return 50;
        }
    }

    /**
     * 获取统计数据 - 采用直接SQL查询以提高准确性
     */
    public StatisticsDTO getStatistics() {
        Connection conn = null;
        try {
            conn = dataSource.getConnection();
            
            // 文章总数
            int articleCount = countArticles(conn);
            
            // 分类总数
            int categoryCount = countCategories(conn);
            
            // 标签总数
            Set<String> allTags = getAllTags(conn);
            
            logger.info("统计数据 - 文章数: {}, 分类数: {}, 标签数: {}", 
                    articleCount, categoryCount, allTags.size());
            
            return new StatisticsDTO(articleCount, categoryCount, allTags.size());
        } catch (SQLException e) {
            logger.error("获取统计数据失败: {}", e.getMessage(), e);
            // 尝试使用备用方法
            return getStatisticsUsingRepository();
        } finally {
            closeConnection(conn);
        }
    }
    
    /**
     * 统计文章数量
     */
    private int countArticles(Connection conn) throws SQLException {
        try (PreparedStatement stmt = conn.prepareStatement("SELECT COUNT(*) FROM articles")) {
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        }
        return 0;
    }
    
    /**
     * 统计分类数量
     */
    private int countCategories(Connection conn) throws SQLException {
        try (PreparedStatement stmt = conn.prepareStatement(
                "SELECT COUNT(DISTINCT category) FROM articles WHERE category IS NOT NULL")) {
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        }
        return 0;
    }
    
    /**
     * 获取所有标签
     */
    private Set<String> getAllTags(Connection conn) throws SQLException {
        Set<String> allTags = new HashSet<>();
        try (PreparedStatement stmt = conn.prepareStatement(
                "SELECT tags FROM articles WHERE tags IS NOT NULL AND tags <> ''")) {
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    String tags = rs.getString("tags");
                    if (tags != null && !tags.trim().isEmpty()) {
                        // 尝试多种分隔符
                        String[] tagArray = tags.split("[,\\|;]");
                        for (String tag : tagArray) {
                            String trimmedTag = tag.trim();
                            if (!trimmedTag.isEmpty()) {
                                allTags.add(trimmedTag);
                            }
                        }
                    }
                }
            }
        }
        return allTags;
    }
    
    /**
     * 使用Repository获取统计数据（备用方法）
     */
    private StatisticsDTO getStatisticsUsingRepository() {
        try {
            // 获取所有文章并记录日志
            List<Article> articles = articleRepository.findAll();
            logger.info("通过Repository查询到文章数量: {}", articles.size());
            
            // 记录前5篇文章的信息，帮助调试
            articles.stream().limit(5).forEach(article -> {
                logger.info("文章ID: {}, 标题: {}, 分类: {}, 标签: {}", 
                    article.getId(), article.getTitle(), 
                    article.getCategory(), article.getTags());
            });
            
            // 获取所有不同的分类数量
            List<String> categories = articles.stream()
                    .map(Article::getCategory)
                    .filter(category -> category != null && !category.isEmpty())
                    .distinct()
                    .collect(Collectors.toList());
            
            logger.info("分类列表: {}", categories);
            
            // 获取所有不同的标签数量
            Set<String> allTags = new HashSet<>();
            articles.forEach(article -> {
                if (article.getTags() != null && !article.getTags().isEmpty()) {
                    String[] tagArray = article.getTags().split("[,\\|;]");
                    for (String tag : tagArray) {
                        String trimmedTag = tag.trim();
                        if (!trimmedTag.isEmpty()) {
                            allTags.add(trimmedTag);
                        }
                    }
                }
            });
            
            logger.info("标签列表: {}", allTags);
            
            return new StatisticsDTO(articles.size(), categories.size(), allTags.size());
        } catch (Exception e) {
            logger.error("使用Repository获取统计数据失败: {}", e.getMessage(), e);
            // 发生异常时返回空数据
            return new StatisticsDTO(0, 0, 0);
        }
    }

    /**
     * 获取文章热图数据
     */
    public List<HeatmapDTO> getArticleHeatmap() {
        // 获取过去一年的所有日期
        LocalDate today = LocalDate.now();
        LocalDate oneYearAgo = today.minusYears(1);
        
        // 创建日期到文章数量的映射
        Map<String, Integer> dateCountMap = new HashMap<>();
        
        // 初始化所有日期的计数为0
        for (LocalDate date = oneYearAgo; !date.isAfter(today); date = date.plusDays(1)) {
            dateCountMap.put(date.toString(), 0);
        }
        
        Connection conn = null;
        try {
            conn = dataSource.getConnection();
            
            // 使用SQL直接查询每天文章数量
            String sql = "SELECT DATE(created_at) as article_date, COUNT(*) as article_count " +
                         "FROM articles " +
                         "WHERE created_at >= ? AND created_at <= ? " +
                         "GROUP BY DATE(created_at)";
            
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setObject(1, oneYearAgo);
                stmt.setObject(2, today);
                
                try (ResultSet rs = stmt.executeQuery()) {
                    while (rs.next()) {
                        String dateStr = rs.getDate("article_date").toLocalDate().toString();
                        int count = rs.getInt("article_count");
                        dateCountMap.put(dateStr, count);
                    }
                }
            }
        } catch (SQLException e) {
            logger.error("SQL查询文章热图数据失败: {}", e.getMessage(), e);
            
            // 如果SQL查询失败，使用Repository作为后备
            try {
                DateTimeFormatter formatter = DateTimeFormatter.ISO_LOCAL_DATE;
                articleRepository.findAll().forEach(article -> {
                    LocalDateTime createdAt = article.getCreatedAt();
                    if (createdAt != null) {
                        String dateStr = createdAt.toLocalDate().format(formatter);
                        dateCountMap.computeIfPresent(dateStr, (k, v) -> v + 1);
                    }
                });
            } catch (Exception ex) {
                logger.error("Repository查询文章热图数据也失败: {}", ex.getMessage(), ex);
                // 如果两种方式都失败，返回空的热图数据
                return new ArrayList<>();
            }
        } finally {
            closeConnection(conn);
        }
        
        // 转换为DTO列表
        List<HeatmapDTO> result = new ArrayList<>();
        dateCountMap.forEach((date, count) -> {
            result.add(new HeatmapDTO(date, count));
        });
        
        logger.info("热图数据: 包含{}天, 文章总数: {}", result.size(), 
                result.stream().mapToInt(HeatmapDTO::getCount).sum());
        
        return result;
    }

    /**
     * 获取最新文章列表
     */
    public List<LatestArticleDTO> getLatestArticles() {
        List<LatestArticleDTO> result = new ArrayList<>();
        Connection conn = null;
        
        try {
            conn = dataSource.getConnection();
            
            // 使用SQL直接查询最新文章
            String sql = "SELECT id, title, created_at, description, tags " +
                         "FROM articles " +
                         "ORDER BY created_at DESC LIMIT 10";
            
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                try (ResultSet rs = stmt.executeQuery()) {
                    while (rs.next()) {
                        Long id = rs.getLong("id");
                        String title = rs.getString("title");
                        LocalDateTime createdAt = rs.getTimestamp("created_at").toLocalDateTime();
                        String description = rs.getString("description");
                        String tags = rs.getString("tags");
                        
                        // 处理可能为空的字段
                        String summary = (description != null) ? description : "";
                        String tagsStr = (tags != null) ? tags : "";
                        
                        result.add(new LatestArticleDTO(
                            id, title, createdAt.toString(), summary, tagsStr
                        ));
                    }
                }
            }
            
            if (!result.isEmpty()) {
                logger.info("SQL查询到{}篇最新文章", result.size());
                return result;
            }
        } catch (SQLException e) {
            logger.error("SQL查询最新文章失败: {}", e.getMessage(), e);
        } finally {
            closeConnection(conn);
        }
        
        // 备用方法：使用Repository
        try {
            List<Article> latestArticles = articleRepository.findAll().stream()
                    .sorted((a1, a2) -> a2.getCreatedAt().compareTo(a1.getCreatedAt()))
                    .limit(10)
                    .collect(Collectors.toList());
            
            if (latestArticles.isEmpty()) {
                logger.warn("未找到任何文章");
                return result; // 返回空列表
            }
            
            for (Article article : latestArticles) {
                String tagsStr = article.getTags() != null ? article.getTags() : "";
                String summary = article.getDescription() != null ? article.getDescription() : "";
                
                result.add(new LatestArticleDTO(
                    article.getId(),
                    article.getTitle(),
                    article.getCreatedAt().toString(),
                    summary,
                    tagsStr
                ));
            }
            
            logger.info("通过Repository查询到{}篇最新文章", result.size());
        } catch (Exception e) {
            logger.error("Repository查询最新文章失败: {}", e.getMessage(), e);
        }
        
        return result;
    }
    
    /**
     * 安全关闭数据库连接
     */
    private void closeConnection(Connection conn) {
        if (conn != null) {
            try {
                conn.close();
            } catch (SQLException e) {
                logger.error("关闭数据库连接失败", e);
            }
        }
    }
} 
