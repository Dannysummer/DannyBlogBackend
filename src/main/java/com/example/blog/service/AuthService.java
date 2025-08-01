package com.example.blog.service;

import com.example.blog.dto.*;
import com.example.blog.entity.User;
// import com.example.blog.model.VerificationCode;
import com.example.blog.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Value;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.example.blog.util.JwtUtil;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.time.LocalDateTime;
import com.example.blog.enums.UserStatus;

@Service
public class AuthService {
    private static final Logger logger = LoggerFactory.getLogger(AuthService.class);
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private PasswordEncoder passwordEncoder;
    
    @Autowired
    private EmailService emailService;
    
    @Autowired
    private JwtUtil jwtUtil;
    
    @Value("${app.email.verification.enabled:false}")  // 从配置文件读取，默认false
    private boolean emailVerificationEnabled;
    
    private Map<String, String> emailVerificationCodes = new HashMap<>();
    // 记录邮箱最后请求时间
    private Map<String, Long> lastEmailRequestTime = new HashMap<>();
    // 限制时间间隔（60秒）
    private static final long EMAIL_REQUEST_INTERVAL = 60 * 1000;
    
    // private Map<String, VerificationCode> loginVerificationCodes = new HashMap<>();
    
    /**
     * 用户登录
     */
    public ApiResponse<?> login(LoginRequest loginRequest) {
        try {
            User user = userRepository.findByUsername(loginRequest.getUsername());
            
            // 用户不存在
            if (user == null) {
                return ApiResponse.error("用户名或密码错误");
            }
            
            // 检查用户状态
            if (user.getStatus() == UserStatus.BANNED) {
                logger.warn("用户已被封禁，禁止登录: username={}", loginRequest.getUsername());
                return ApiResponse.error("您的账号已被封禁，请联系管理员");
            }
            
            if (user.getStatus() == UserStatus.LOCKED) {
                logger.warn("用户已被锁定，禁止登录: username={}", loginRequest.getUsername());
                return ApiResponse.error("您的账号已被锁定，请联系管理员解锁");
            }
            
            if (user.getStatus() == UserStatus.DELETED) {
                logger.warn("用户已注销，禁止登录: username={}", loginRequest.getUsername());
                return ApiResponse.error("该账号已被注销");
            }
            
            if (user.getStatus() == UserStatus.PENDING) {
                logger.warn("用户待审核，禁止登录: username={}", loginRequest.getUsername());
                return ApiResponse.error("您的账号正在审核中，请耐心等待");
            }
            
            // 验证密码
            if (!passwordEncoder.matches(loginRequest.getPassword(), user.getPassword())) {
                return ApiResponse.error("用户名或密码错误");
            }
            
            // 生成JWT令牌
            String token = jwtUtil.generateToken(user);
            
            // 更新最后登录时间
            user.setLastLoginTime(LocalDateTime.now());
            userRepository.save(user);
            
            // 构建用户信息
            Map<String, Object> userInfo = new HashMap<>();
            userInfo.put("id", user.getId());
            userInfo.put("username", user.getUsername());
            userInfo.put("email", user.getEmail());
            userInfo.put("role", user.getRole());
            userInfo.put("avatar", user.getAvatar());
            userInfo.put("token", token);
            userInfo.put("status", user.getStatus());
            
            return ApiResponse.success(userInfo);
        } catch (Exception e) {
            logger.error("用户登录失败", e);
            return ApiResponse.error("登录失败：" + e.getMessage());
        }
    }
    
    public ApiResponse<?> register(RegisterRequest request) {
        // 验证码校验
        String storedCode = emailVerificationCodes.get(request.getEmail());
        if (storedCode == null || !storedCode.equals(request.getEmailVerifyCode())) {
            return ApiResponse.error("验证码错误或已过期");
        }
        
        // 打印接收到的注册请求数据
        logger.info("收到注册请求: username={}, email={}", request.getUsername(), request.getEmail());
        
        // 验证用户名是否已存在
        if (userRepository.existsByUsername(request.getUsername())) {
            logger.warn("注册失败: 用户名已存在 username={}", request.getUsername());
            return ApiResponse.error("您的用户名已经存在啦，这样不符合您的高贵身份丫！");
        }
        
        // 验证邮箱是否已存在
        if (userRepository.existsByEmail(request.getEmail())) {
            logger.warn("注册失败: 邮箱已被注册 email={}", request.getEmail());
            return ApiResponse.error("大人您的邮箱已经注册过啦，仔细回忆一下叭。");
        }
        
        try {
            User user = new User();
            user.setUsername(request.getUsername());
            user.setPassword(passwordEncoder.encode(request.getPassword()));
            user.setEmail(request.getEmail());
            
            // 计算用户排名
            Long userCount = userRepository.count();
            user.setRank(userCount + 1);
            
            User savedUser = userRepository.save(user);
            logger.info("用户注册成功: id={}, username={}, rank={}", savedUser.getId(), savedUser.getUsername(), savedUser.getRank());
            
            // 注册成功后删除验证码
            emailVerificationCodes.remove(request.getEmail());
            
            // 生成token
            String token = generateToken(savedUser);
            
            ApiResponse<User> response = ApiResponse.success(savedUser);
            response.setToken(token);  // 设置token
            return response;
            
        } catch (Exception e) {
            logger.error("用户注册失败: ", e);
            return ApiResponse.error("注册失败：" + e.getMessage());
        }
    }
    
    public ApiResponse<?> sendEmailCode(String email) {
        return sendEmailCode(email, null);
    }

    public ApiResponse<?> sendEmailCode(String email, String type) {
        try {
            // 检查请求频率
            Long lastRequestTime = lastEmailRequestTime.get(email);
            long currentTime = System.currentTimeMillis();
            
            if (lastRequestTime != null) {
                long timeDiff = currentTime - lastRequestTime;
                if (timeDiff < EMAIL_REQUEST_INTERVAL) {
                    long waitSeconds = (EMAIL_REQUEST_INTERVAL - timeDiff) / 1000;
                    return ApiResponse.error(String.format("请等待 %d 秒后再试", waitSeconds));
                }
            }
            
            // 更新最后请求时间
            lastEmailRequestTime.put(email, currentTime);
            
            // 生成验证码
            String code = generateVerificationCode();
            // 保存验证码
            emailVerificationCodes.put(email, code);
            // 发送验证码
            emailService.sendVerificationCode(email, code, type);
            
            // 只保留一条日志，记录完整信息
            logger.info("验证码发送成功: email={}, code={}, type={}", email, code, type);
            
            return ApiResponse.success("验证码发送成功");
        } catch (Exception e) {
            logger.error("发送验证码失败: email={}, type={}", email, type, e);
            return ApiResponse.error("发送验证码失败：" + e.getMessage());
        }
    }
    
    public ApiResponse<?> resetPassword(String email, String code) {
        try {
            // 验证邮箱是否存在
            User user = userRepository.findByEmail(email);
            if (user == null) {
                return ApiResponse.error("邮箱未注册");
            }
            
            // 验证验证码
            String storedCode = emailVerificationCodes.get(email);
            if (storedCode == null || !storedCode.equals(code)) {
                return ApiResponse.error("验证码错误或已过期");
            }
            
            // 生成新密码
            String newPassword = generateRandomPassword();
            user.setPassword(passwordEncoder.encode(newPassword));
            userRepository.save(user);
            
            // 发送新密码邮件
            emailService.sendNewPassword(email, newPassword);
            
            // 清除验证码
            emailVerificationCodes.remove(email);
            
            return ApiResponse.success("新密码已发送到您的邮箱");
        } catch (Exception e) {
            logger.error("重置密码失败: ", e);
            return ApiResponse.error("重置密码失败：" + e.getMessage());
        }
    }
    
    public ApiResponse<?> verifyEmailCode(String email, String code) {
        try {
            String storedCode = emailVerificationCodes.get(email);
            if (storedCode == null) {
                return ApiResponse.error("验证码不存在或已过期");
            }
            
            if (!storedCode.equals(code)) {
                return ApiResponse.error("验证码错误");
            }
            
            return ApiResponse.success("验证成功");
        } catch (Exception e) {
            logger.error("验证邮箱验证码失败: ", e);
            return ApiResponse.error("验证失败：" + e.getMessage());
        }
    }
    
    private String generateVerificationCode() {
        // 生成6位数字验证码
        return String.format("%06d", (int)(Math.random() * 1000000));
    }
    
    private String generateRandomPassword() {
        // 定义字符集
        String upperCase = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
        String lowerCase = "abcdefghijklmnopqrstuvwxyz";
        String numbers = "0123456789";
        String specialChars = "!@#$%^&*()_+-=[]{}|;:,.<>?";
        
        StringBuilder password = new StringBuilder();
        Random random = new Random();
        
        // 确保密码包含至少一个大写字母
        password.append(upperCase.charAt(random.nextInt(upperCase.length())));
        // 确保密码包含至少一个小写字母
        password.append(lowerCase.charAt(random.nextInt(lowerCase.length())));
        // 确保密码包含至少一个数字
        password.append(numbers.charAt(random.nextInt(numbers.length())));
        // 确保密码包含至少一个特殊字符
        password.append(specialChars.charAt(random.nextInt(specialChars.length())));
        
        // 添加额外的8个随机字符
        String allChars = upperCase + lowerCase + numbers + specialChars;
        for (int i = 0; i < 8; i++) {
            password.append(allChars.charAt(random.nextInt(allChars.length())));
        }
        
        // 打乱密码顺序
        char[] passwordArray = password.toString().toCharArray();
        for (int i = passwordArray.length - 1; i > 0; i--) {
            int j = random.nextInt(i + 1);
            char temp = passwordArray[i];
            passwordArray[i] = passwordArray[j];
            passwordArray[j] = temp;
        }
        
        return new String(passwordArray);
    }
    
    private String generateToken(User user) {
        return jwtUtil.generateToken(user);
    }
    
    public ApiResponse<?> updateAvatar(String username, String avatarUrl) {
        try {
            // 查找用户
            User user = userRepository.findByUsername(username);
            if (user == null) {
                return ApiResponse.error("用户不存在");
            }
            
            // 更新头像URL
            user.setAvatar(avatarUrl);
            userRepository.save(user);
            
            logger.info("用户头像更新成功: username={}, avatarUrl={}", username, avatarUrl);
            return ApiResponse.success("头像更新成功");
        } catch (Exception e) {
            logger.error("更新头像失败: ", e);
            return ApiResponse.error("更新头像失败：" + e.getMessage());
        }
    }
} 