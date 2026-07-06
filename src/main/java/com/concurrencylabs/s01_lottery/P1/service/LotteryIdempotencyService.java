package com.concurrencylabs.s01_lottery.P1.service;

import com.concurrencylabs.util.redis.RedisUtil;
import org.springframework.stereotype.Service;

import java.time.Duration;

/**
 * 抽獎冪等控制（SA Process Spec：去重）。用 Redis SET NX 做「一次性 claim」，
 * 不是可 acquire/release 的完整分散式鎖（見 DEVELOPMENT §五）。
 */
@Service
public class LotteryIdempotencyService {

    /** claim 結果。 */
    public enum Claim {
        /** 首次，本請求負責執行。 */
        FIRST,
        /** 已完成，應回放上次結果。 */
        DONE,
        /** 別的請求執行中（同 key 尚未完成）。 */
        PENDING
    }

    private static final String PENDING_VAL = "PENDING";
    private static final String DONE_VAL = "DONE";
    private static final Duration CLAIM_TTL = Duration.ofSeconds(60);
    private static final Duration DONE_TTL = Duration.ofHours(1);

    private final RedisUtil redisUtil;

    public LotteryIdempotencyService(RedisUtil redisUtil) {
        this.redisUtil = redisUtil;
    }

    public Claim claim(String idempotencyKey) {
        String key = key(idempotencyKey);
        if (redisUtil.setIfAbsent(key, PENDING_VAL, CLAIM_TTL)) {
            return Claim.FIRST;
        }
        return DONE_VAL.equals(redisUtil.get(key)) ? Claim.DONE : Claim.PENDING;
    }

    /** 成功完成後標記，之後同 key 走回放。 */
    public void markDone(String idempotencyKey) {
        redisUtil.set(key(idempotencyKey), DONE_VAL, DONE_TTL);
    }

    /** 拒絕 / 系統失敗時釋放 claim，允許之後重試。 */
    public void release(String idempotencyKey) {
        redisUtil.delete(key(idempotencyKey));
    }

    private static String key(String idempotencyKey) {
        return "lottery:idem:" + idempotencyKey;
    }
}
