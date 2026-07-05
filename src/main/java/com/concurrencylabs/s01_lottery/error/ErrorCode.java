package com.concurrencylabs.s01_lottery.error;

import lombok.Getter;

/** 抽獎錯誤碼（見 SD §5）。 */
@Getter
public enum ErrorCode {

    ACTIVITY_NOT_FOUND(404, "40401", "活動不存在"),
    ACTIVITY_NOT_STARTED(409, "40901", "活動未開始"),
    ACTIVITY_ENDED(409, "40902", "活動已結束"),
    INSUFFICIENT_POINTS(402, "40201", "積分不足"),
    PROCESSING(409, "40903", "抽獎處理中，請稍後重試");

    private final int httpStatus;
    private final String code;
    private final String defaultMessage;

    ErrorCode(int httpStatus, String code, String defaultMessage) {
        this.httpStatus = httpStatus;
        this.code = code;
        this.defaultMessage = defaultMessage;
    }
}
