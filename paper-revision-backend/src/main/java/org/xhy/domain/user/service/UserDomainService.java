package org.xhy.domain.user.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.xhy.domain.user.model.UserEntity;
import org.xhy.domain.user.repository.UserRepository;
import org.xhy.infrastructure.exception.BusinessException;
import org.xhy.infrastructure.exception.EntityNotFoundException;
import org.xhy.infrastructure.utils.PasswordUtils;

/** 用户领域服务 */
@Service
public class UserDomainService {

    private static final Logger logger = LoggerFactory.getLogger(UserDomainService.class);

    private final UserRepository userRepository;
    private final PasswordUtils passwordUtils;

    public UserDomainService(UserRepository userRepository, PasswordUtils passwordUtils) {
        this.userRepository = userRepository;
        this.passwordUtils = passwordUtils;
    }

    /** 邮箱注册 */
    public UserEntity registerByEmail(String email, String nickname, String rawPassword) {
        // 检查邮箱是否已注册
        LambdaQueryWrapper<UserEntity> wrapper = Wrappers.<UserEntity>lambdaQuery()
                .eq(UserEntity::getEmail, email);
        UserEntity existing = userRepository.selectOne(wrapper);
        if (existing != null) {
            throw new BusinessException("该邮箱已被注册");
        }

        String salt = passwordUtils.generateSalt();
        String encodedPassword = passwordUtils.hashPassword(rawPassword, salt);

        UserEntity user = UserEntity.create(email, nickname, encodedPassword, salt);
        user.valid();
        userRepository.checkInsert(user);
        logger.info("用户注册成功: {}", email);
        return user;
    }

    /** 邮箱+密码登录 */
    public UserEntity loginByEmail(String email, String rawPassword) {
        LambdaQueryWrapper<UserEntity> wrapper = Wrappers.<UserEntity>lambdaQuery()
                .eq(UserEntity::getEmail, email);
        UserEntity user = userRepository.selectOne(wrapper);
        if (user == null) {
            throw new BusinessException("邮箱或密码错误");
        }

        if (!passwordUtils.verifyPassword(rawPassword, user.getSalt(), user.getPassword())) {
            throw new BusinessException("邮箱或密码错误");
        }

        return user;
    }

    /** 根据ID获取用户 */
    public UserEntity getUserById(String userId) {
        UserEntity user = userRepository.selectById(userId);
        if (user == null) {
            throw new EntityNotFoundException("用户", userId);
        }
        return user;
    }

    /** 根据邮箱查找用户 */
    public UserEntity findByEmail(String email) {
        LambdaQueryWrapper<UserEntity> wrapper = Wrappers.<UserEntity>lambdaQuery()
                .eq(UserEntity::getEmail, email);
        return userRepository.selectOne(wrapper);
    }
}
