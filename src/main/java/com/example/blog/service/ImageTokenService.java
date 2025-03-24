package com.example.blog.service;

import org.apache.commons.codec.binary.Hex;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class ImageTokenService {
    
    private static final Logger logger = LoggerFactory.getLogger(ImageTokenService.class);
    
    @Value("${app.image.access-key}")
    private String accessKey;
    
    @Value("${app.image.secret-key}")
    private String secretKey;
    
    @Value("${app.image.bucket}")
    private String bucket;
    
    @Value("${app.image.upload-url}")
    private String uploadUrl;
    
    @Value("${app.image.allowed-types}")
    private String allowedTypes;
    
    @Value("${app.image.max-file-size}")
    private long maxFileSize;
    
    @Value("${app.image.custom-domain:}")
    private String customDomain;
    
    // 存储用户上传临时密钥的缓存
    private final ConcurrentHashMap<String, Long> tempTokenCache = new ConcurrentHashMap<>();
    
    /**
     * 生成图床上传令牌
     * 按照多吉云文档要求生成签名和令牌
     */
    public Map<String, Object> generateUploadToken() {
        try {
            // 生成唯一文件名，避免文件覆盖
            String fileName = UUID.randomUUID().toString().replace("-", "") + ".jpg";
            // 正确格式化存储范围
            String scope = bucket + ":" + "user/avatars/" + fileName;
            
            // 构建API请求路径 - 获取临时上传令牌
            String apiPath = "/auth/tmp_token.json";
            
            // 构建JSON请求体 - 按照多吉云文档要求
            JSONObject requestJson = new JSONObject();
            requestJson.put("channel", "OSS_UPLOAD");
            // scopes应该是一个数组字段
            requestJson.put("scopes", new String[]{scope});
            String requestBody = requestJson.toString();
            
            logger.info("请求体内容: {}", requestBody);
            
            // 构建签名字符串
            String stringToSign = apiPath + "\n" + requestBody;
            
            // 使用HMAC-SHA1计算签名 - 使用十六进制编码
            Mac mac = Mac.getInstance("HmacSHA1");
            mac.init(new SecretKeySpec(secretKey.getBytes(StandardCharsets.UTF_8), "HmacSHA1"));
            byte[] signData = mac.doFinal(stringToSign.getBytes(StandardCharsets.UTF_8));
            String signature = new String(Hex.encodeHex(signData));
            
            // 构建Authorization请求头
            String authorization = "TOKEN " + accessKey + ":" + signature;
            
            // 记录日志
            logger.info("生成多吉云上传令牌，API路径：{}，签名：{}", apiPath, signature);
            
            // 发送HTTP请求到多吉云获取临时上传令牌
            Map<String, Object> dogeCloudResponse = requestDogeCloudToken(apiPath, requestBody, authorization);
            
            // 返回给前端的令牌信息
            Map<String, Object> result = new HashMap<>();
            
            // 如果配置了自定义域名，优先使用
            if (customDomain != null && !customDomain.isEmpty()) {
                result.put("uploadUrl", customDomain);
                logger.info("使用自定义域名作为上传URL: {}", customDomain);
            } else {
                result.put("uploadUrl", uploadUrl);
            }
            
            result.put("fileName", fileName);
            result.put("bucket", bucket);
            result.put("path", "user/avatars/" + fileName); // 不要包含bucket，只是路径
            result.put("allowedTypes", allowedTypes);
            result.put("maxFileSize", maxFileSize);
            
            // 如果成功获取了多吉云的临时令牌，将其添加到结果中
            if (dogeCloudResponse != null && dogeCloudResponse.containsKey("token")) {
                result.put("token", dogeCloudResponse.get("token"));
                result.put("expiresAt", dogeCloudResponse.get("expires"));
                
                // 如果配置了自定义域名，优先使用
                if (customDomain != null && !customDomain.isEmpty()) {
                    // 不需要修改uploadUrl，因为前面已经设置为自定义域名
                    logger.info("使用自定义域名: {}", customDomain);
                }
                // 如果没有自定义域名但有存储桶终端节点信息，使用官方终端节点
                else if (dogeCloudResponse.containsKey("s3EndpointHost")) {
                    // 更新上传URL为实际的终端节点，确保正确上传
                    String s3EndpointHost = (String) dogeCloudResponse.get("s3EndpointHost");
                    result.put("uploadUrl", "https://" + s3EndpointHost);
                    logger.info("使用存储桶终端节点作为上传URL: {}", result.get("uploadUrl"));
                }
                
                logger.info("成功获取多吉云临时上传令牌，文件名：{}，过期时间：{}", 
                           fileName, dogeCloudResponse.get("expires"));
            } else {
                // 模拟令牌（实际应用中应使用多吉云返回的真实令牌）
                String tempToken = accessKey + ":" + signature;
                result.put("token", tempToken);
                result.put("expiresAt", System.currentTimeMillis() / 1000 + 3600);
                logger.warn("未能获取多吉云临时上传令牌，使用模拟令牌");
            }
            
            return result;
        } catch (Exception e) {
            logger.error("生成上传令牌失败", e);
            throw new RuntimeException("生成上传令牌失败：" + e.getMessage());
        }
    }
    
    /**
     * 发送HTTP请求到多吉云API获取临时上传令牌
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
            
            logger.info("发送请求到多吉云: URL={}, 请求头={}, 请求体={}", 
                       httpPost.getURI(), authorization, requestBody);
            
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
                        // 成功获取令牌
                        JSONObject data = jsonResponse.getJSONObject("data");
                        Map<String, Object> result = new HashMap<>();
                        
                        // 获取Credentials信息
                        if (data.has("Credentials")) {
                            JSONObject credentials = data.getJSONObject("Credentials");
                            // 构造适合多吉云OSS上传的token格式
                            if (credentials.has("accessKeyId") && credentials.has("secretAccessKey") && credentials.has("sessionToken")) {
                                // 这里需要按照多吉云OSS上传API的要求组装token
                                String accessKeyId = credentials.getString("accessKeyId");
                                String secretAccessKey = credentials.getString("secretAccessKey");
                                String sessionToken = credentials.getString("sessionToken");
                                
                                // 将这三个参数组合成一个token，用于上传API
                                String combinedToken = String.format("%s:%s:%s", 
                                    accessKeyId, secretAccessKey, sessionToken);
                                result.put("token", combinedToken);
                                
                                logger.info("成功解析多吉云临时凭证: accessKeyId={}, secretKey={}...", 
                                    accessKeyId, secretAccessKey.substring(0, 5));
                            } else {
                                logger.warn("Credentials字段缺少必要的密钥信息");
                            }
                        } else {
                            logger.warn("API响应中缺少Credentials字段: {}", responseString);
                        }
                        
                        // 获取过期时间
                        if (data.has("ExpiredAt")) {
                            result.put("expires", data.getLong("ExpiredAt"));
                        } else {
                            // 默认1小时后过期
                            result.put("expires", System.currentTimeMillis() / 1000 + 3600);
                            logger.warn("API响应中缺少ExpiredAt字段，使用默认过期时间");
                        }
                        
                        // 额外记录存储桶信息
                        if (data.has("Buckets") && data.getJSONArray("Buckets").length() > 0) {
                            JSONObject bucket = data.getJSONArray("Buckets").getJSONObject(0);
                            if (bucket.has("s3EndpointHost")) {
                                String s3EndpointHost = bucket.getString("s3EndpointHost");
                                logger.info("获取到存储桶终端节点: {}", s3EndpointHost);
                                result.put("s3EndpointHost", s3EndpointHost);
                            }
                        }
                        
                        return result;
                    } else {
                        // API返回错误
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
}