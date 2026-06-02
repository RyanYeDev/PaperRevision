package org.xhy.infrastructure.exception;

/** 实体未找到异常 */
public class EntityNotFoundException extends BusinessException {

    public EntityNotFoundException(String message) {
        super(message);
    }

    public EntityNotFoundException(String entityName, String id) {
        super(entityName + "不存在: " + id);
    }
}
