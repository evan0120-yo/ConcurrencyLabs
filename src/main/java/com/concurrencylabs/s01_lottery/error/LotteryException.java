package com.concurrencylabs.s01_lottery.error;

import lombok.Getter;

/** 抽獎業務例外，攜帶 {@link ErrorCode}。 */
@Getter
public class LotteryException extends RuntimeException {

    private final ErrorCode errorCode;

    public LotteryException(ErrorCode errorCode) {
        super(errorCode.getDefaultMessage());
        this.errorCode = errorCode;
    }
}
