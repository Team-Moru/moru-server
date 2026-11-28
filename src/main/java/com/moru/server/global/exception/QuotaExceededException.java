package com.moru.server.global.exception;

import com.moru.server.global.response.code.status.ErrorStatus;
import lombok.Getter;

@Getter
public class QuotaExceededException extends RuntimeException {
    private final ErrorStatus error;
    private final long retryAfter;

    public QuotaExceededException(ErrorStatus error, long retryAfter) {
        super(error.getCode());
        this.error = error;
        this.retryAfter = retryAfter;
    }
}
