package com.exam.controller;

import com.exam.common.Result;
import com.exam.dto.LoginRequest;
import com.exam.dto.LoginResponse;
import com.exam.dto.RegisterRequest;
import com.exam.entity.User;
import com.exam.service.AuthService;
import com.exam.util.UserContext;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    public Result<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        return Result.success(authService.login(request));
    }

    @PostMapping("/register")
    public Result<Void> register(@Valid @RequestBody RegisterRequest request) {
        authService.register(request);
        return Result.success();
    }

    @GetMapping("/info")
    public Result<User> info() {
        User user = authService.getUserById(UserContext.getUserId());
        user.setPassword(null); // Hide password
        return Result.success(user);
    }
}
