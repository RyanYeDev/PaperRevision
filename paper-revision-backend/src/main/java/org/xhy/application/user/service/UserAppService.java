package org.xhy.application.user.service;

import org.springframework.stereotype.Service;
import org.xhy.application.user.assembler.UserAssembler;
import org.xhy.application.user.dto.UserDTO;
import org.xhy.domain.user.model.UserEntity;
import org.xhy.domain.user.service.UserDomainService;
import org.xhy.infrastructure.utils.JwtUtils;

/** 用户应用服务 */
@Service
public class UserAppService {

    private final UserDomainService userDomainService;
    private final JwtUtils jwtUtils;

    public UserAppService(UserDomainService userDomainService, JwtUtils jwtUtils) {
        this.userDomainService = userDomainService;
        this.jwtUtils = jwtUtils;
    }

    /** 邮箱注册 */
    public UserDTO register(String email, String nickname, String password) {
        UserEntity user = userDomainService.registerByEmail(email, nickname, password);
        return UserAssembler.toDTO(user);
    }

    /** 邮箱登录，返回JWT Token */
    public String login(String email, String password) {
        UserEntity user = userDomainService.loginByEmail(email, password);
        return jwtUtils.generateToken(user.getId(), user.getEmail());
    }

    /** 获取当前用户信息 */
    public UserDTO getCurrentUser(String userId) {
        UserEntity user = userDomainService.getUserById(userId);
        return UserAssembler.toDTO(user);
    }
}
