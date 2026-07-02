package com.paperrevision.interfaces.api.portal.auth;

import org.springframework.validation.annotation.Validated;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;
import com.paperrevision.application.user.dto.UserDTO;
import com.paperrevision.application.user.service.UserAppService;
import com.paperrevision.infrastructure.auth.UserContext;
import com.paperrevision.infrastructure.utils.JwtUtils;
import com.paperrevision.interfaces.api.common.Result;
import com.paperrevision.interfaces.dto.user.request.LoginRequest;
import com.paperrevision.interfaces.dto.user.request.RegisterRequest;
import com.paperrevision.interfaces.dto.user.response.LoginResponse;

/** 认证控制器 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UserAppService userAppService;
    private final JwtUtils jwtUtils;

    public AuthController(UserAppService userAppService, JwtUtils jwtUtils) {
        this.userAppService = userAppService;
        this.jwtUtils = jwtUtils;
    }

    /** 用户注册 */
    @PostMapping("/register")
    public Result<UserDTO> register(@RequestBody @Validated RegisterRequest request) {
        UserDTO user = userAppService.register(request.getEmail(), request.getNickname(), request.getPassword());
        return Result.success("注册成功", user);
    }

    /** 用户登录 */
    @PostMapping("/login")
    public Result<LoginResponse> login(@RequestBody @Validated LoginRequest request) {
        String token = userAppService.login(request.getEmail(), request.getPassword());
        UserDTO user = userAppService.getCurrentUser(jwtUtils.getUserIdFromToken(token));
        return Result.success(new LoginResponse(token, user));
    }

    /** 获取当前用户信息 */
    @GetMapping("/me")
    public Result<UserDTO> getCurrentUser(HttpServletRequest request) {
        String token = extractToken(request);
        if (token == null || !jwtUtils.validateToken(token)) {
            return Result.unauthorized("未登录或Token已过期");
        }
        String userId = jwtUtils.getUserIdFromToken(token);
        UserDTO user = userAppService.getCurrentUser(userId);
        return Result.success(user);
    }

    private String extractToken(HttpServletRequest request) {
        String bearer = request.getHeader("Authorization");
        if (bearer != null && bearer.startsWith("Bearer ")) {
            return bearer.substring(7);
        }
        return null;
    }
}
