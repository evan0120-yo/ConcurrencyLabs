package com.concurrencylabs.s01_lottery.P1.object.dto;

import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 抽獎請求（邊界 DTO）。無 auth：userId 直接傳入（labs 簡化，見 SD §5）。
 * idempotencyKey：同一次邏輯抽獎重送帶同一個，保證只發生一次。
 */
@Data
@NoArgsConstructor
public class DrawReq {

    private String userId;

    private String idempotencyKey;
}
