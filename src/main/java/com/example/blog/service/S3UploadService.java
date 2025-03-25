package com.example.blog.service;

import org.apache.commons.codec.binary.Hex;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
// import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.auth.credentials.AwsSessionCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectResponse;
// import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.beans.factory.annotation.Autowired;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;
import java.io.ByteArrayOutputStream;
// import java.util.regex.Matcher;
// import java.util.regex.Pattern;

@Service
public class S3UploadService {
    
    private static final Logger logger = LoggerFactory.getLogger(S3UploadService.class);
    
    @Value("${app.image.access-key}")
    private String accessKey;
    
    @Value("${app.image.secret-key}")
    private String secretKey;
    
    @Value("${app.image.bucket}")
    private String bucket;
    
    @Value("${app.image.allowed-types}")
    private String allowedTypes;
    
    @Value("${app.image.max-file-size}")
    private long maxFileSize;
    
    @Value("${app.image.custom-domain:}")
    private String customDomain;
    
    // @Autowired
    // private SimpMessagingTemplate messagingTemplate;
    
    /**
     * 获取上传策略和临时凭证
     * @param path 上传路径，不包含bucket
     * @return 上传所需的信息
     */
    public Map<String, Object> getUploadPolicy(String path) {
        try {
            String scope;
            // 如果没有提供path，生成一个默认的
            if (path == null || path.isEmpty()) {
                // 生成唯一文件名
                String fileName = UUID.randomUUID().toString().replace("-", "") + ".jpg";
                path = "images/" + fileName;
            }
            
            // 始终确保使用正确的scope格式：bucket:path
            scope = bucket + ":" + path;
            logger.info("构建上传策略scope: {}", scope);
            
            // 构建多吉云临时令牌API请求路径
            String apiPath = "/auth/tmp_token.json";
            
            // 构建请求体
            JSONObject requestJson = new JSONObject();
            requestJson.put("channel", "OSS_UPLOAD");
            requestJson.put("scopes", new String[]{scope});
            String requestBody = requestJson.toString();
            
            logger.info("请求体内容: {}", requestBody);
            
            // 构建签名字符串
            String stringToSign = apiPath + "\n" + requestBody;
            
            // 计算签名
            Mac mac = Mac.getInstance("HmacSHA1");
            mac.init(new SecretKeySpec(secretKey.getBytes(StandardCharsets.UTF_8), "HmacSHA1"));
            byte[] signData = mac.doFinal(stringToSign.getBytes(StandardCharsets.UTF_8));
            String signature = new String(Hex.encodeHex(signData));
            
            // 构建请求头
            String authorization = "TOKEN " + accessKey + ":" + signature;
            
            // 发送请求到多吉云API获取临时令牌
            Map<String, Object> tokenData = requestDogeCloudToken(apiPath, requestBody, authorization);
            
            if (tokenData != null) {
                // 准备返回给前端的数据
                Map<String, Object> result = new HashMap<>();
                result.put("fileName", path);
                result.put("path", path);
                result.put("bucket", bucket);
                result.put("allowedTypes", allowedTypes);
                result.put("maxFileSize", maxFileSize);
                
                // 添加临时凭证信息
                if (tokenData.containsKey("accessKeyId") && 
                    tokenData.containsKey("secretAccessKey") && 
                    tokenData.containsKey("sessionToken") && 
                    tokenData.containsKey("expires") &&
                    tokenData.containsKey("s3Bucket") &&
                    tokenData.containsKey("s3Endpoint")) {
                    
                    result.put("credentials", tokenData);
                    
                    // 将凭证信息添加到顶层，方便直接访问
                    result.put("accessKeyId", tokenData.get("accessKeyId"));
                    result.put("secretAccessKey", tokenData.get("secretAccessKey"));
                    result.put("sessionToken", tokenData.get("sessionToken"));
                    result.put("expires", tokenData.get("expires"));
                    result.put("s3Bucket", tokenData.get("s3Bucket"));
                    result.put("s3Endpoint", tokenData.get("s3Endpoint"));
                    
                    logger.info("成功获取上传策略，文件路径：{}，过期时间：{}", 
                        path, tokenData.get("expires"));
                    
                    return result;
                }
            }
            
            throw new RuntimeException("获取上传策略失败");
        } catch (Exception e) {
            logger.error("获取上传策略失败", e);
            throw new RuntimeException("获取上传策略失败: " + e.getMessage());
        }
    }
    
    /**
     * 使用S3 SDK直接上传文件
     * @param file 要上传的文件
     * @return 上传结果，包含文件URL
     */
    public Map<String, Object> uploadFile(MultipartFile file) {
        return uploadFile(file, null);
    }
    
    /**
     * 使用S3 SDK直接上传文件，支持自定义路径
     * @param file 要上传的文件
     * @param params 额外参数，可以包含自定义路径等信息
     * @return 上传结果，包含文件URL
     */
    public Map<String, Object> uploadFile(MultipartFile file, Map<String, Object> params) {
        try {
            // 确定文件路径
            String filePath;
            if (params != null && params.containsKey("path")) {
                filePath = (String) params.get("path");
                logger.info("使用自定义路径上传文件: {}", filePath);
            } else {
                // 生成默认路径
                String originalFilename = file.getOriginalFilename();
                String fileExtension = originalFilename != null && originalFilename.contains(".") 
                    ? originalFilename.substring(originalFilename.lastIndexOf(".")) 
                    : "";
                String fileName = UUID.randomUUID().toString().replace("-", "") + fileExtension;
                filePath = "images/" + fileName;
                logger.info("使用生成的默认路径: {}", filePath);
            }
            
            // 获取临时凭证 - 直接传递filePath，不需要构建scope
            Map<String, Object> policy = getUploadPolicy(filePath);
            Map<String, Object> credentials = (Map<String, Object>) policy.get("credentials");
            
            if (credentials == null) {
                logger.error("获取临时凭证失败，无法上传文件");
                throw new RuntimeException("获取临时凭证失败");
            }
            
            // 提取凭证信息
            String accessKeyId = (String) credentials.get("accessKeyId");
            String secretAccessKey = (String) credentials.get("secretAccessKey");
            String sessionToken = (String) credentials.get("sessionToken");
            String s3Bucket = (String) credentials.get("s3Bucket");
            String s3Endpoint = (String) credentials.get("s3Endpoint");
            
            if (accessKeyId == null || secretAccessKey == null || sessionToken == null || s3Bucket == null || s3Endpoint == null) {
                logger.error("临时凭证信息不完整: accessKeyId={}, secretKey={}, sessionToken={}, s3Bucket={}, s3Endpoint={}", 
                    accessKeyId != null, secretAccessKey != null, sessionToken != null, s3Bucket, s3Endpoint);
                throw new RuntimeException("临时凭证信息不完整");
            }
            
            logger.info("获取到原始S3终端节点: {}, 桶: {}", s3Endpoint, s3Bucket);
            
            // 创建AWS凭证
            AwsSessionCredentials awsCreds = AwsSessionCredentials.create(
                accessKeyId, secretAccessKey, sessionToken);
            
            // 修复S3端点URL
            s3Endpoint = fixS3EndpointUrl(s3Endpoint, s3Bucket);
            
            URI endpointUri;
            try {
                endpointUri = URI.create(s3Endpoint);
                logger.info("S3 endpoint URI: {}", endpointUri);
            } catch (Exception e) {
                logger.error("无效的S3终端节点URL: {}", s3Endpoint, e);
                throw new RuntimeException("无效的S3终端节点URL");
            }
            
            // 初始化S3客户端
            S3Client s3 = S3Client.builder()
                .credentialsProvider(StaticCredentialsProvider.create(awsCreds))
                .region(Region.of("automatic"))
                .endpointOverride(endpointUri)
                .build();
                
            // 构建文件上传请求
            PutObjectRequest putOb = PutObjectRequest.builder()
                .bucket(s3Bucket)
                .key(filePath)
                .contentType(file.getContentType())
                .build();
                
            // 上传文件    
            PutObjectResponse response;
            try {
                logger.info("正在上传文件，桶: {}, 路径: {}, 大小: {}", s3Bucket, filePath, file.getSize());
                response = s3.putObject(putOb, 
                    RequestBody.fromInputStream(file.getInputStream(), file.getSize()));
                logger.info("文件上传成功：{}, ETag: {}", filePath, response.eTag());
            } catch (Exception e) {
                logger.error("上传文件失败: {}", e.getMessage(), e);
                throw new IOException("上传文件失败: " + e.getMessage(), e);
            }
            
            // 构建返回结果
            Map<String, Object> result = new HashMap<>();
            
            // 如果配置了自定义域名，则使用自定义域名，否则使用默认的S3域名
            String fileUrl;
            if (customDomain != null && !customDomain.isEmpty()) {
                // 确保路径以/开头
                String pathWithSlash = filePath.startsWith("/") ? filePath : "/" + filePath;
                fileUrl = customDomain + pathWithSlash;
                logger.info("使用自定义域名生成URL: {}", fileUrl);
            } else {
                // 使用S3端点生成URL，端点已经被修复，不需要再处理斜杠
                fileUrl = s3Endpoint + "/" + filePath;
                logger.info("使用S3端点生成URL: {}", fileUrl);
            }
            logger.info("生成的文件URL: {}", fileUrl);
            result.put("url", fileUrl);
            result.put("filename", file.getOriginalFilename());
            result.put("size", file.getSize());
            result.put("path", filePath);
            
            return result;
        } catch (IOException e) {
            logger.error("文件上传失败", e);
            throw new RuntimeException("文件上传失败: " + e.getMessage());
        }
    }
    
    /**
     * 请求多吉云API获取临时令牌
     */
    private Map<String, Object> requestDogeCloudToken(String apiPath, String requestBody, String authorization) {
        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            HttpPost httpPost = new HttpPost("https://api.dogecloud.com" + apiPath);
            
            // 设置请求头
            httpPost.setHeader("Content-Type", "application/json");
            httpPost.setHeader("Authorization", authorization);
            
            // 设置请求体
            StringEntity entity = new StringEntity(requestBody, StandardCharsets.UTF_8);
            httpPost.setEntity(entity);
            
            logger.info("发送请求到多吉云: URL={}, 请求头={}", 
                       httpPost.getURI(), authorization);
            
            // 发送请求
            try (CloseableHttpResponse response = httpClient.execute(httpPost)) {
                HttpEntity responseEntity = response.getEntity();
                if (responseEntity != null) {
                    // 解析响应内容
                    String responseString = EntityUtils.toString(responseEntity);
                    logger.info("多吉云API响应: {}", responseString);
                    
                    JSONObject jsonResponse = new JSONObject(responseString);
                    
                    // 检查API响应状态
                    if (jsonResponse.getInt("code") == 200) {
                        JSONObject data = jsonResponse.getJSONObject("data");
                        Map<String, Object> result = new HashMap<>();
                        
                        // 提取临时凭证
                        if (data.has("Credentials")) {
                            JSONObject credentials = data.getJSONObject("Credentials");
                            if (credentials.has("accessKeyId") && credentials.has("secretAccessKey") && credentials.has("sessionToken")) {
                                result.put("accessKeyId", credentials.getString("accessKeyId"));
                                result.put("secretAccessKey", credentials.getString("secretAccessKey"));
                                result.put("sessionToken", credentials.getString("sessionToken"));
                            }
                        }
                        
                        // 设置过期时间
                        if (data.has("ExpiredAt")) {
                            result.put("expires", data.getLong("ExpiredAt"));
                        }
                        
                        // 提取存储桶信息
                        if (data.has("Buckets") && data.getJSONArray("Buckets").length() > 0) {
                            JSONObject bucket = data.getJSONArray("Buckets").getJSONObject(0);
                            if (bucket.has("s3Bucket")) {
                                result.put("s3Bucket", bucket.getString("s3Bucket"));
                            }
                            if (bucket.has("s3EndpointHost")) {
                                String s3EndpointHost = bucket.getString("s3EndpointHost");
                                // 确保endpoint是完整的URL，避免域名重复问题
                                if (!s3EndpointHost.startsWith("http")) {
                                    s3EndpointHost = "https://" + s3EndpointHost;
                                }
                                result.put("s3Endpoint", s3EndpointHost);
                                logger.info("获取到S3终端节点: {}", s3EndpointHost);
                            }
                        }
                        
                        return result;
                    } else {
                        logger.error("多吉云API返回错误：code={}, msg={}", 
                                    jsonResponse.getInt("code"), 
                                    jsonResponse.getString("msg"));
                    }
                }
            }
        } catch (Exception e) {
            logger.error("请求多吉云API失败", e);
        }
        return null;
    }
    
    /**
     * 获取删除操作的临时凭证
     * @param filePath 要删除的文件路径
     * @return 临时凭证信息
     */
    private Map<String, Object> getDeletePolicy(String filePath) {
        try {
            // 确保使用正确的scope格式：bucket:filePath
            String scope = bucket + ":" + filePath;
            logger.info("构建删除策略scope: {}", scope);
            
            // 构建多吉云临时令牌API请求路径
            String apiPath = "/auth/tmp_token.json";
            
            // 构建请求体
            JSONObject requestJson = new JSONObject();
            requestJson.put("channel", "OSS_FULL");  // 使用OSS_FULL作为channel，而不是OSS_DELETE
            requestJson.put("scopes", new String[]{scope});
            String requestBody = requestJson.toString();
            
            logger.info("删除操作请求体内容: {}", requestBody);
            
            // 构建签名字符串
            String stringToSign = apiPath + "\n" + requestBody;
            
            // 计算签名
            Mac mac = Mac.getInstance("HmacSHA1");
            mac.init(new SecretKeySpec(secretKey.getBytes(StandardCharsets.UTF_8), "HmacSHA1"));
            byte[] signData = mac.doFinal(stringToSign.getBytes(StandardCharsets.UTF_8));
            String signature = new String(Hex.encodeHex(signData));
            
            // 构建请求头
            String authorization = "TOKEN " + accessKey + ":" + signature;
            
            // 发送请求到多吉云API获取临时令牌
            Map<String, Object> tokenData = requestDogeCloudToken(apiPath, requestBody, authorization);
            
            if (tokenData != null) {
                logger.info("成功获取删除操作的临时凭证，文件路径：{}", filePath);
                return tokenData;
            }
            
            throw new RuntimeException("获取删除操作的临时凭证失败");
        } catch (Exception e) {
            logger.error("获取删除操作的临时凭证失败", e);
            throw new RuntimeException("获取删除操作的临时凭证失败: " + e.getMessage());
        }
    }
    
    /**
     * 删除多吉云存储桶中的文件
     * @param filePath 文件路径
     */
    public void deleteFile(String filePath) {
        try {
            // 获取删除操作的临时凭证
            Map<String, Object> credentials = getDeletePolicy(filePath);
            
            if (credentials == null) {
                logger.error("获取临时凭证失败，无法删除文件");
                throw new RuntimeException("获取临时凭证失败");
            }
            
            // 提取凭证信息
            String accessKeyId = (String) credentials.get("accessKeyId");
            String secretAccessKey = (String) credentials.get("secretAccessKey");
            String sessionToken = (String) credentials.get("sessionToken");
            String s3Bucket = (String) credentials.get("s3Bucket");
            String s3Endpoint = (String) credentials.get("s3Endpoint");
            
            if (accessKeyId == null || secretAccessKey == null || sessionToken == null || s3Bucket == null || s3Endpoint == null) {
                logger.error("临时凭证信息不完整: accessKeyId={}, secretKey={}, sessionToken={}, s3Bucket={}, s3Endpoint={}", 
                    accessKeyId != null, secretAccessKey != null, sessionToken != null, s3Bucket, s3Endpoint);
                throw new RuntimeException("临时凭证信息不完整");
            }
            
            logger.info("正在准备删除S3对象，桶：{}，文件路径：{}", s3Bucket, filePath);
            logger.info("获取到原始S3终端节点: {}, 桶: {}", s3Endpoint, s3Bucket);
            
            // 创建AWS凭证
            AwsSessionCredentials awsCreds = AwsSessionCredentials.create(
                accessKeyId, secretAccessKey, sessionToken);
            
            // 修复S3端点URL
            s3Endpoint = fixS3EndpointUrl(s3Endpoint, s3Bucket);
            
            URI endpointUri;
            try {
                endpointUri = URI.create(s3Endpoint);
                logger.info("S3 endpoint URI: {}", endpointUri);
            } catch (Exception e) {
                logger.error("无效的S3终端节点URL: {}", s3Endpoint, e);
                throw new RuntimeException("无效的S3终端节点URL");
            }
            
            // 初始化S3客户端
            S3Client s3 = S3Client.builder()
                .credentialsProvider(StaticCredentialsProvider.create(awsCreds))
                .region(Region.of("automatic"))
                .endpointOverride(endpointUri)
                .build();
            
            try {
                // 使用多吉云推荐的批量删除API
                // 创建要删除的对象标识符列表
                ArrayList<software.amazon.awssdk.services.s3.model.ObjectIdentifier> keys = new ArrayList<>();
                keys.add(software.amazon.awssdk.services.s3.model.ObjectIdentifier.builder()
                    .key(filePath)  // 使用传入的filePath
                    .build());
                
                // 构建Delete对象
                software.amazon.awssdk.services.s3.model.Delete del = 
                    software.amazon.awssdk.services.s3.model.Delete.builder()
                        .objects(keys)
                        .build();
                
                // 构建DeleteObjectsRequest
                software.amazon.awssdk.services.s3.model.DeleteObjectsRequest multiObjectDeleteRequest = 
                    software.amazon.awssdk.services.s3.model.DeleteObjectsRequest.builder()
                        .bucket(s3Bucket) // 使用多吉云返回的s3Bucket
                        .delete(del)
                        .build();
                
                logger.info("准备发送删除请求，桶：{}，文件路径：{}", s3Bucket, filePath);
                
                // 执行批量删除操作
                software.amazon.awssdk.services.s3.model.DeleteObjectsResponse deleteResponse = s3.deleteObjects(multiObjectDeleteRequest);
                
                // 打印删除操作的响应
                if (deleteResponse.hasErrors()) {
                    logger.error("删除操作存在错误：");
                    deleteResponse.errors().forEach(error -> 
                        logger.error("对象：{}，错误代码：{}，错误消息：{}", 
                            error.key(), error.code(), error.message()));
                }
                
                if (deleteResponse.hasDeleted()) {
                    logger.info("成功删除的对象：");
                    deleteResponse.deleted().forEach(deleted -> 
                        logger.info("对象：{}，删除标记：{}", deleted.key(), deleted.deleteMarker()));
                }
                
                logger.info("删除操作完成，完整响应：{}", deleteResponse);
            } catch (Exception e) {
                logger.error("删除S3对象失败: {}", e.getMessage(), e);
                throw new RuntimeException("删除S3对象失败: " + e.getMessage());
            } finally {
                s3.close();
            }
        } catch (Exception e) {
            logger.error("删除多吉云存储桶中的文件失败：{}", filePath, e);
            throw new RuntimeException("删除文件失败：" + e.getMessage());
        }
    }
    
    /**
     * 上传文件并跟踪进度
     * 
     * @param file 要上传的文件
     * @param progressListener 进度监听器
     * @return 上传后的文件信息
     * @throws IOException 如果上传失败
     */
    public Map<String, Object> uploadFileWithProgress(MultipartFile file, UploadProgressListener progressListener) throws IOException {
        return uploadFileWithProgress(file, progressListener, null);
    }
    
    /**
     * 上传文件并跟踪进度，支持自定义路径
     * 
     * @param file 要上传的文件
     * @param progressListener 进度监听器
     * @param params 额外参数，可以包含自定义路径等信息
     * @return 上传后的文件信息
     * @throws IOException 如果上传失败
     */
    public Map<String, Object> uploadFileWithProgress(MultipartFile file, UploadProgressListener progressListener, Map<String, Object> params) throws IOException {
        try {
            // 获取自定义路径，如果提供的话
            String filePath;
            if (params != null && params.containsKey("path")) {
                filePath = (String) params.get("path");
                logger.info("使用自定义路径上传文件: {}", filePath);
            } else {
                // 生成唯一文件名
                String originalFilename = file.getOriginalFilename();
                String fileExtension = originalFilename != null && originalFilename.contains(".") 
                    ? originalFilename.substring(originalFilename.lastIndexOf(".")) 
                    : "";
                String fileName = UUID.randomUUID().toString().replace("-", "") + fileExtension;
                filePath = "images/" + fileName;
                logger.info("使用生成的默认路径: {}", filePath);
            }
            
            // 获取上传策略 - 直接传递filePath，不需要构建scope
            Map<String, Object> policy = getUploadPolicy(filePath);
            if (policy == null) {
                throw new IOException("获取上传策略失败");
            }
            
            // 设置AWS凭证
            String accessKeyId = (String) policy.get("accessKeyId");
            String secretAccessKey = (String) policy.get("secretAccessKey");
            String sessionToken = (String) policy.get("sessionToken");
            
            // 创建AWS凭证
            AwsSessionCredentials awsCreds = AwsSessionCredentials.create(
                accessKeyId, secretAccessKey, sessionToken);
            StaticCredentialsProvider credentialsProvider = StaticCredentialsProvider.create(awsCreds);
            
            // 设置S3终端节点
            String s3Endpoint = (String) policy.get("s3Endpoint");
            String s3Bucket = (String) policy.get("s3Bucket");
            
            logger.info("获取到原始S3终端节点: {}, 桶: {}", s3Endpoint, s3Bucket);
            
            // 修复S3端点URL
            s3Endpoint = fixS3EndpointUrl(s3Endpoint, s3Bucket);
            
            URI endpointUri;
            try {
                endpointUri = URI.create(s3Endpoint);
                logger.info("S3 endpoint URI: {}", endpointUri);
            } catch (Exception e) {
                logger.error("无效的S3终端节点URL: {}", s3Endpoint, e);
                throw new IOException("S3终端节点URL格式错误: " + e.getMessage());
            }
            
            S3Client s3 = S3Client.builder()
                .credentialsProvider(credentialsProvider)
                .region(Region.of("automatic"))
                .endpointOverride(endpointUri)
                .build();
                
            // 构建文件上传请求
            PutObjectRequest putOb = PutObjectRequest.builder()
                .bucket(s3Bucket)
                .key(filePath)
                .contentType(file.getContentType())
                .build();
                
            // 将MultipartFile转换为File以便进行进度跟踪
            File tempFile = File.createTempFile("upload_", getFileExtension(file.getOriginalFilename()));
            try {
                file.transferTo(tempFile);
                
                // 上传文件    
                logger.info("正在上传文件，桶: {}, 路径: {}, 大小: {}", s3Bucket, filePath, tempFile.length());
                
                // 进度更新
                if (progressListener != null) {
                    progressListener.setTotalBytes(tempFile.length());
                }
                
                // 读取文件并上传，同时跟踪进度
                PutObjectResponse response;
                try (InputStream inputStream = Files.newInputStream(tempFile.toPath())) {
                    byte[] buffer = new byte[8192];
                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    int bytesRead;
                    long totalBytesRead = 0;
                    
                    // 先读取整个文件到内存中，同时更新进度
                    while ((bytesRead = inputStream.read(buffer)) != -1) {
                        baos.write(buffer, 0, bytesRead);
                        totalBytesRead += bytesRead;
                        
                        if (progressListener != null) {
                            progressListener.updateBytesTransferred(bytesRead);
                            
                            // 每上传约5%记录一次日志
                            if (totalBytesRead % Math.max(tempFile.length() / 20, 1) < bytesRead) {
                                double percentage = ((double) totalBytesRead / tempFile.length()) * 100;
                                logger.info("文件 [{}] 上传进度: {}/{} 字节 ({}%) - ETag: 待生成", 
                                    file.getOriginalFilename(),
                                    totalBytesRead, tempFile.length(), 
                                    String.format("%.1f", percentage));
                            }
                        }
                    }
                    
                    // 创建RequestBody并上传
                    RequestBody requestBody = RequestBody.fromBytes(baos.toByteArray());
                    response = s3.putObject(putOb, requestBody);
                    
                    // 标记传输完成
                    if (progressListener != null) {
                        progressListener.markTransferComplete();
                    }
                } catch (Exception e) {
                    logger.error("上传文件失败: {}", e.getMessage(), e);
                    if (progressListener != null) {
                        progressListener.markTransferFailed(e);
                    }
                    throw new IOException("上传文件失败: " + e.getMessage(), e);
                }
                
                logger.info("文件 [{}] 上传完成: 路径={}, 大小={}, ETag={}", 
                    file.getOriginalFilename(), filePath, 
                    formatFileSize(tempFile.length()), response.eTag());
            
                // 构建返回结果
                Map<String, Object> result = new HashMap<>();
                
                // 如果配置了自定义域名，则使用自定义域名，否则使用默认的S3域名
                String fileUrl;
                if (customDomain != null && !customDomain.isEmpty()) {
                    // 确保路径以/开头
                    String pathWithSlash = filePath.startsWith("/") ? filePath : "/" + filePath;
                    fileUrl = customDomain + pathWithSlash;
                    logger.info("使用自定义域名生成URL: {}", fileUrl);
                } else {
                    // 使用S3端点生成URL，端点已经被修复，不需要再处理斜杠
                    fileUrl = s3Endpoint + "/" + filePath;
                    logger.info("使用S3端点生成URL: {}", fileUrl);
                }
                logger.info("生成的文件URL: {}", fileUrl);
                result.put("url", fileUrl);
                result.put("filename", file.getOriginalFilename());
                result.put("size", file.getSize());
                result.put("path", filePath);
                
                return result;
            } finally {
                // 删除临时文件
                if (tempFile.exists()) {
                    if (tempFile.delete()) {
                        logger.debug("临时文件已删除: {}", tempFile.getAbsolutePath());
                    } else {
                        logger.warn("无法删除临时文件: {}", tempFile.getAbsolutePath());
                    }
                }
            }
        } catch (IOException e) {
            logger.error("文件上传失败", e);
            if (progressListener != null) {
                progressListener.markTransferFailed(e);
            }
            throw new IOException("文件上传失败: " + e.getMessage(), e);
        }
    }
    
    /**
     * 从文件名获取扩展名
     */
    private String getFileExtension(String filename) {
        if (filename == null || filename.isEmpty() || !filename.contains(".")) {
            return ".tmp";
        }
        return filename.substring(filename.lastIndexOf("."));
    }
    
    /**
     * 格式化文件大小显示
     */
    private String formatFileSize(long size) {
        if (size <= 0) return "0 B";
        final String[] units = new String[] { "B", "KB", "MB", "GB", "TB" };
        int digitGroups = (int) (Math.log10(size) / Math.log10(1024));
        return String.format("%.1f %s", size / Math.pow(1024, digitGroups), units[digitGroups]);
    }
    
    /**
     * 修复S3端点URL，处理常见的格式问题
     * @param s3Endpoint 原始S3端点URL
     * @param s3Bucket 存储桶名称
     * @return 修复后的S3端点URL
     */
    private String fixS3EndpointUrl(String s3Endpoint, String s3Bucket) {
        if (s3Endpoint == null || s3Endpoint.isEmpty()) {
            return s3Endpoint;
        }

        // 移除重复的bucket名称
        String bucketPattern = s3Bucket + "\\." + s3Bucket;
        if (s3Endpoint.contains(bucketPattern)) {
            s3Endpoint = s3Endpoint.replaceFirst(bucketPattern, s3Bucket);
            logger.info("移除重复的bucket名称后: {}", s3Endpoint);
        }

        // 修复重复的域名段
        if (s3Endpoint.contains("cos.ap-chengdu.myqcloud.com.cos.ap-chengdu.myqcloud.com")) {
            s3Endpoint = s3Endpoint.replace("cos.ap-chengdu.myqcloud.com.cos.ap-chengdu.myqcloud.com", "cos.ap-chengdu.myqcloud.com");
            logger.info("修复重复的域名段后: {}", s3Endpoint);
        }

        // 确保URL以http开头
        if (!s3Endpoint.startsWith("http")) {
            s3Endpoint = "https://" + s3Endpoint;
            logger.info("添加https协议后: {}", s3Endpoint);
        }

        // 处理腾讯云COS的特殊情况
        if (s3Endpoint.contains("cos.ap-chengdu.myqcloud.com")) {
            // 移除可能存在的重复bucket名称
            String baseUrl = "cos.ap-chengdu.myqcloud.com";
            if (s3Endpoint.contains(s3Bucket + "." + baseUrl)) {
                s3Endpoint = s3Endpoint.replace(s3Bucket + "." + baseUrl, baseUrl);
                logger.info("处理腾讯云COS特殊情况后: {}", s3Endpoint);
            }
        }

        try {
            URI uri = new URI(s3Endpoint);
            logger.info("S3 endpoint URI: {}", uri);
            return uri.toString();
        } catch (URISyntaxException e) {
            logger.error("无效的S3 endpoint URL: {}", s3Endpoint, e);
            return s3Endpoint;
        }
    }
} 