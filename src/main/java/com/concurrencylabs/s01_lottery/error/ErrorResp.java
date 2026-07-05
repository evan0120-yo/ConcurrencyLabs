package com.concurrencylabs.s01_lottery.error;

import lombok.AllArgsConstructor;
import lombok.Data;

/** 錯誤回應 body（邊界 DTO；依規範用 Lombok class，不用 record）。 */
@Data
@AllArgsConstructor
public class ErrorResp {

    private String code;

    private String message;
}
