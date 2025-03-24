package com.example.blog.service;

import com.example.blog.dto.ApiResponse;
import com.example.blog.dto.UserDto;
import com.example.blog.entity.User;
import com.example.blog.enums.UserStatus;
import com.example.blog.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class UserService {
    
    private static final Logger logger = LoggerFactory.getLogger(UserService.class);
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private PasswordEncoder passwordEncoder;
    
    /**
     * 获取用户列表（分页）
     */
    public ApiResponse<Map<String, Object>> getUserList(int page, int size, String username, UserStatus status) {
        try {
            Pageable pageable = PageRequest.of(page, size, Sort.by("createTime").descending());
            
            Page<User> userPage;
            if (username != null && !username.isEmpty() && status != null) {
                // 按用户名和状态筛选
                userPage = userRepository.findByUsernameContainingAndStatus(username, status, pageable);
            } else if (username != null && !username.isEmpty()) {
                // 仅按用户名筛选
                userPage = userRepository.findByUsernameContaining(username, pageable);
            } else if (status != null) {
                // 仅按状态筛选
                userPage = userRepository.findByStatus(status, pageable);
            } else {
                // 不筛选
                userPage = userRepository.findAll(pageable);
            }
            
            // 转换为DTO
            List<UserDto> userDtos = userPage.getContent().stream()
                    .map(this::convertToDto)
                    .collect(Collectors.toList());
            
            Map<String, Object> result = new HashMap<>();
            result.put("users", userDtos);
            result.put("totalItems", userPage.getTotalElements());
            result.put("totalPages", userPage.getTotalPages());
            result.put("currentPage", userPage.getNumber());
            
            return ApiResponse.success(result);
        } catch (Exception e) {
            logger.error("获取用户列表失败", e);
            return ApiResponse.error("获取用户列表失败：" + e.getMessage());
        }
    }
    
    /**
     * 获取用户详情
     */
    public ApiResponse<UserDto> getUserDetail(Long id) {
        try {
            Optional<User> userOpt = userRepository.findById(id);
            if (userOpt.isPresent()) {
                UserDto userDto = convertToDto(userOpt.get());
                return ApiResponse.success(userDto);
            } else {
                return ApiResponse.error("用户不存在：ID=" + id);
            }
        } catch (Exception e) {
            logger.error("获取用户详情失败", e);
            return ApiResponse.error("获取用户详情失败：" + e.getMessage());
        }
    }
    
    /**
     * 创建用户
     */
    @Transactional
    public ApiResponse<UserDto> createUser(UserDto userDto, String operator) {
        try {
            // 检查用户名和邮箱是否已存在
            if (userRepository.findByUsername(userDto.getUsername()) != null) {
                return ApiResponse.error("用户名已存在：" + userDto.getUsername());
            }
            
            if (userRepository.findByEmail(userDto.getEmail()) != null) {
                return ApiResponse.error("邮箱已存在：" + userDto.getEmail());
            }
            
            User user = new User();
            user.setUsername(userDto.getUsername());
            user.setEmail(userDto.getEmail());
            user.setRole(userDto.getRole());
            user.setPhoneNumber(userDto.getPhoneNumber());
            user.setRealName(userDto.getRealName());
            user.setRemark(userDto.getRemark());
            user.setUpdatedBy(operator);
            
            // 设置状态
            if (userDto.getStatus() != null) {
                user.setStatus(userDto.getStatus());
            } else {
                user.setStatus(UserStatus.NORMAL);
            }
            
            // 设置初始密码
            if (userDto.getPassword() != null && !userDto.getPassword().isEmpty()) {
                user.setPassword(passwordEncoder.encode(userDto.getPassword()));
            } else {
                // 默认随机密码
                String randomPassword = generateRandomPassword();
                user.setPassword(passwordEncoder.encode(randomPassword));
                userDto.setPassword(randomPassword); // 返回明文密码给前端显示
            }
            
            // 保存用户
            User savedUser = userRepository.save(user);
            UserDto savedUserDto = convertToDto(savedUser);
            
            logger.info("用户创建成功：username={}, operator={}", user.getUsername(), operator);
            return ApiResponse.success(savedUserDto);
        } catch (Exception e) {
            logger.error("创建用户失败", e);
            return ApiResponse.error("创建用户失败：" + e.getMessage());
        }
    }
    
    /**
     * 更新用户
     */
    @Transactional
    public ApiResponse<UserDto> updateUser(Long id, UserDto userDto, String operator) {
        try {
            Optional<User> userOpt = userRepository.findById(id);
            if (!userOpt.isPresent()) {
                return ApiResponse.error("用户不存在：ID=" + id);
            }
            
            User user = userOpt.get();
            
            // 检查邮箱是否被其他用户使用
            if (userDto.getEmail() != null && !userDto.getEmail().equals(user.getEmail())) {
                User existingUser = userRepository.findByEmail(userDto.getEmail());
                if (existingUser != null && !existingUser.getId().equals(id)) {
                    return ApiResponse.error("邮箱已被其他用户使用：" + userDto.getEmail());
                }
                user.setEmail(userDto.getEmail());
            }
            
            // 更新其他字段
            if (userDto.getRole() != null) {
                user.setRole(userDto.getRole());
            }
            
            if (userDto.getStatus() != null) {
                user.setStatus(userDto.getStatus());
            }
            
            if (userDto.getPhoneNumber() != null) {
                user.setPhoneNumber(userDto.getPhoneNumber());
            }
            
            if (userDto.getRealName() != null) {
                user.setRealName(userDto.getRealName());
            }
            
            if (userDto.getRemark() != null) {
                user.setRemark(userDto.getRemark());
            }
            
            if (userDto.getAvatar() != null) {
                user.setAvatar(userDto.getAvatar());
            }
            
            // 如果需要修改密码
            if (userDto.getPassword() != null && !userDto.getPassword().isEmpty()) {
                user.setPassword(passwordEncoder.encode(userDto.getPassword()));
            }
            
            user.setUpdatedBy(operator);
            
            // 保存更新
            User updatedUser = userRepository.save(user);
            UserDto updatedUserDto = convertToDto(updatedUser);
            
            logger.info("用户更新成功：username={}, operator={}", user.getUsername(), operator);
            return ApiResponse.success(updatedUserDto);
        } catch (Exception e) {
            logger.error("更新用户失败", e);
            return ApiResponse.error("更新用户失败：" + e.getMessage());
        }
    }
    
    /**
     * 删除用户（逻辑删除，修改状态为DELETED）
     */
    @Transactional
    public ApiResponse<?> deleteUser(Long id, String operator) {
        try {
            Optional<User> userOpt = userRepository.findById(id);
            if (!userOpt.isPresent()) {
                return ApiResponse.error("用户不存在：ID=" + id);
            }
            
            User user = userOpt.get();
            user.setStatus(UserStatus.DELETED);
            user.setUpdatedBy(operator);
            user.setUpdateTime(LocalDateTime.now());
            
            userRepository.save(user);
            
            logger.info("用户删除成功：username={}, operator={}", user.getUsername(), operator);
            return ApiResponse.success("用户删除成功");
        } catch (Exception e) {
            logger.error("删除用户失败", e);
            return ApiResponse.error("删除用户失败：" + e.getMessage());
        }
    }
    
    /**
     * 修改用户状态
     */
    @Transactional
    public ApiResponse<?> changeUserStatus(Long id, UserStatus status, String operator) {
        try {
            Optional<User> userOpt = userRepository.findById(id);
            if (!userOpt.isPresent()) {
                return ApiResponse.error("用户不存在：ID=" + id);
            }
            
            User user = userOpt.get();
            user.setStatus(status);
            user.setUpdatedBy(operator);
            user.setUpdateTime(LocalDateTime.now());
            
            userRepository.save(user);
            
            logger.info("用户状态修改成功：username={}, status={}, operator={}", 
                    user.getUsername(), status, operator);
            return ApiResponse.success("用户状态修改成功");
        } catch (Exception e) {
            logger.error("修改用户状态失败", e);
            return ApiResponse.error("修改用户状态失败：" + e.getMessage());
        }
    }
    
    /**
     * 重置用户密码
     */
    @Transactional
    public ApiResponse<String> resetUserPassword(Long id, String operator) {
        try {
            Optional<User> userOpt = userRepository.findById(id);
            if (!userOpt.isPresent()) {
                return ApiResponse.error("用户不存在：ID=" + id);
            }
            
            User user = userOpt.get();
            
            // 生成随机密码
            String randomPassword = generateRandomPassword();
            user.setPassword(passwordEncoder.encode(randomPassword));
            user.setUpdatedBy(operator);
            user.setUpdateTime(LocalDateTime.now());
            
            userRepository.save(user);
            
            logger.info("用户密码重置成功：username={}, operator={}", user.getUsername(), operator);
            return ApiResponse.success(randomPassword);
        } catch (Exception e) {
            logger.error("重置用户密码失败", e);
            return ApiResponse.error("重置用户密码失败：" + e.getMessage());
        }
    }
    
    /**
     * 生成随机密码
     */
    private String generateRandomPassword() {
        // 生成8位随机密码
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789!@#$%^&*()";
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 8; i++) {
            int index = (int) (Math.random() * chars.length());
            sb.append(chars.charAt(index));
        }
        return sb.toString();
    }
    
    /**
     * 将User实体转换为UserDto
     */
    private UserDto convertToDto(User user) {
        UserDto dto = new UserDto();
        dto.setId(user.getId());
        dto.setUsername(user.getUsername());
        dto.setEmail(user.getEmail());
        dto.setRole(user.getRole());
        dto.setAvatar(user.getAvatar());
        dto.setStatus(user.getStatus());
        dto.setPhoneNumber(user.getPhoneNumber());
        dto.setRealName(user.getRealName());
        dto.setRemark(user.getRemark());
        dto.setCreateTime(user.getCreateTime());
        dto.setUpdateTime(user.getUpdateTime());
        dto.setLastLoginTime(user.getLastLoginTime());
        dto.setUpdatedBy(user.getUpdatedBy());
        return dto;
    }
} 