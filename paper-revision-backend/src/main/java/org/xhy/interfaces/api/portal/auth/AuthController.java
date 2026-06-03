package org.xhy.interfaces.api.portal.auth;

import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.xhy.application.user.assembler.UserAssembler;
import org.xhy.application.user.dto.UserDTO;
import org.xhy.application.user.service.UserAppService;
import org.xhy.infrastructure.auth.UserContext;
import org.xhy.infrastructure.utils.JwtUtils;
import org.xhy.interfaces.api.common.Result;
import org.xhy.interfaces.dto.user.request.LoginRequest;
import org.xhy.interfaces.dto.user.request.RegisterRequest;
import org.xhy.interfaces.dto.user.response.LoginResponse;

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
    public Result<UserDTO> getCurrentUser() {
        String userId = UserContext.getCurrentUserId();
        UserDTO user = userAppService.getCurrentUser(userId);
        return Result.success(user);
    }
}
