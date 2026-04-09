package com.exam.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.exam.common.BusinessException;
import com.exam.dto.LoginRequest;
import com.exam.dto.LoginResponse;
import com.exam.dto.RegisterRequest;
import com.exam.entity.User;
import com.exam.mapper.UserMapper;
import com.exam.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;

import java.nio.charset.StandardCharsets;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserMapper userMapper;
    private final JwtUtil jwtUtil;

    public LoginResponse login(LoginRequest request) {
        User user = userMapper.selectOne(
                new LambdaQueryWrapper<User>().eq(User::getUsername, request.getUsername())
        );
        
        if (user == null) {
            throw new BusinessException("Invalid username or password");
        }
        
        String encodedPassword = encodePassword(request.getPassword());
        if (!encodedPassword.equals(user.getPassword())) {
            throw new BusinessException("Invalid username or password");
        }
        
        String token = jwtUtil.generateToken(user.getId(), user.getUsername(), user.getRole());
        log.info("User logged in: {}", user.getUsername());
        
        return new LoginResponse(token, user.getId(), user.getUsername(), user.getNickname(), user.getRole());
    }

    public void register(RegisterRequest request) {
        User existing = userMapper.selectOne(
                new LambdaQueryWrapper<User>().eq(User::getUsername, request.getUsername())
        );
        
        if (existing != null) {
            throw new BusinessException("Username already exists");
        }
        
        User user = new User();
        user.setUsername(request.getUsername());
        user.setPassword(encodePassword(request.getPassword()));
        user.setNickname(request.getNickname() != null ? request.getNickname() : request.getUsername());
        user.setRole("STUDENT");
        
        userMapper.insert(user);
        log.info("New user registered: {}", user.getUsername());
    }

    public User getUserById(Long id) {
        return userMapper.selectById(id);
    }

    private String encodePassword(String password) {
        return DigestUtils.md5DigestAsHex((password + "exam_salt").getBytes(StandardCharsets.UTF_8));
    }
}
