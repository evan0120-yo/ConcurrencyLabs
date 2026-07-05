package com.concurrencylabs.s01_lottery.error;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/** 把 {@link LotteryException} 映射成 HTTP 狀態碼 + errorCode。 */
@RestControllerAdvice(basePackages = "com.concurrencylabs.s01_lottery")
public class LotteryExceptionHandler {

    @ExceptionHandler(LotteryException.class)
    public ResponseEntity<ErrorResp> handle(LotteryException e) {
        ErrorCode c = e.getErrorCode();
        return ResponseEntity.status(c.getHttpStatus())
                .body(new ErrorResp(c.getCode(), e.getMessage()));
    }
}
