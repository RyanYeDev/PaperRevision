package com.paperrevision.infrastructure.exception;

/** 参数校验异常 */
public class ParamValidationException extends BusinessException {

    public ParamValidationException(String message) {
        super(message);
    }

    public ParamValidationException(String field, String message) {
        super("参数校验失败 - " + field + ": " + message);
    }
}
