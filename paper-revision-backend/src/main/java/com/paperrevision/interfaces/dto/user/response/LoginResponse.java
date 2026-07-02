package com.paperrevision.interfaces.dto.user.response;

import com.paperrevision.application.user.dto.UserDTO;

/** 登录响应 */
public class LoginResponse {

    private String token;
    private UserDTO user;

    public LoginResponse(String token, UserDTO user) {
        this.token = token;
        this.user = user;
    }

    public String getToken() { return token; }
    public void setToken(String token) { this.token = token; }
    public UserDTO getUser() { return user; }
    public void setUser(UserDTO user) { this.user = user; }
}
