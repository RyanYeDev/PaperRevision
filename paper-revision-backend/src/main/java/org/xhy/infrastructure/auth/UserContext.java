package org.xhy.infrastructure.auth;

/** 用户上下文，使用ThreadLocal存储当前请求的用户信息 */
public class UserContext {

    private static final ThreadLocal<String> CURRENT_USER_ID = new ThreadLocal<>();

    /** 设置当前用户ID */
    public static void setCurrentUserId(String userId) {
        CURRENT_USER_ID.set(userId);
    }

    /** 获取当前用户ID */
    public static String getCurrentUserId() {
        return CURRENT_USER_ID.get();
    }

    /** 清除当前用户信息 */
    public static void clear() {
        CURRENT_USER_ID.remove();
    }
}
