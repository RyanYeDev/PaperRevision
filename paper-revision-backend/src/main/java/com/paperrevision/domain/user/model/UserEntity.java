package com.paperrevision.domain.user.model;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.paperrevision.infrastructure.entity.BaseEntity;
import com.paperrevision.infrastructure.exception.BusinessException;

/** 用户实体 */
@TableName("users")
public class UserEntity extends BaseEntity {

    @TableId(type = IdType.ASSIGN_UUID)
    private String id;
    private String nickname;
    private String email;
    private String phone;
    private String password;
    private String salt;
    private String avatarUrl;
    private Boolean isAdmin;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getNickname() { return nickname; }
    public void setNickname(String nickname) { this.nickname = nickname; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    public String getSalt() { return salt; }
    public void setSalt(String salt) { this.salt = salt; }
    public String getAvatarUrl() { return avatarUrl; }
    public void setAvatarUrl(String avatarUrl) { this.avatarUrl = avatarUrl; }
    public Boolean getIsAdmin() { return isAdmin; }
    public void setIsAdmin(Boolean isAdmin) { this.isAdmin = isAdmin; }

    public boolean isAdmin() {
        return Boolean.TRUE.equals(isAdmin);
    }

    public void valid() {
        if (StringUtils.isEmpty(email) && StringUtils.isEmpty(phone)) {
            throw new BusinessException("必须使用邮箱或手机号注册");
        }
    }

    /** 创建新用户 */
    public static UserEntity create(String email, String nickname, String encodedPassword, String salt) {
        UserEntity user = new UserEntity();
        user.setEmail(email);
        user.setNickname(nickname);
        user.setPassword(encodedPassword);
        user.setSalt(salt);
        user.setIsAdmin(false);
        return user;
    }
}
