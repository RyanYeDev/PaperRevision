package com.paperrevision.infrastructure.entity;

/** 操作者枚举，用于区分用户操作和管理员操作 */
public enum Operator {

    USER, ADMIN;

    public boolean needCheckUserId() {
        return this == Operator.USER;
    }
}
