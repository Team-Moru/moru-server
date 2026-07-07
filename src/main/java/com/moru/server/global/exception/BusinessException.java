package com.moru.server.global.exception;

import com.moru.server.global.response.code.BaseCode;
import lombok.Getter;

@Getter
public class BusinessException extends RuntimeException {
    private final BaseCode baseCode;

    public BusinessException(BaseCode baseCode) {
        super(baseCode.getMessage());
        this.baseCode = baseCode;
    }
}
