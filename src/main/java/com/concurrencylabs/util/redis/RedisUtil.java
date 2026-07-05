package com.concurrencylabs.util.redis;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.List;

/**
 * 跨情境共用的 Redis 原子操作薄封裝（util/redis）。
 *
 * <p>只做「Redis 存取封裝」，不含任何情境業務規則。原子性由 Redis 保證
 * （單執行緒 / Lua 內序列化），本工具不自行加鎖；多台 server 共享同一 Redis，
 * 原子性天生跨 server（見 DEVELOPMENT §五）。
 */
@Component
public class RedisUtil {

    private final StringRedisTemplate redis;

    public RedisUtil(StringRedisTemplate redis) {
        this.redis = redis;
    }

    /** {@code SET key val NX EX ttl}。回傳是否搶到（true = 首次），用於 idempotency claim。 */
    public boolean setIfAbsent(String key, String value, Duration ttl) {
        Boolean ok = redis.opsForValue().setIfAbsent(key, value, ttl);
        return Boolean.TRUE.equals(ok);
    }

    public String get(String key) {
        return redis.opsForValue().get(key);
    }

    public void set(String key, String value, Duration ttl) {
        redis.opsForValue().set(key, value, ttl);
    }

    /** 設值（無 TTL），用於載入獎品餘量等長駐 key。 */
    public void set(String key, String value) {
        redis.opsForValue().set(key, value);
    }

    public boolean delete(String key) {
        return Boolean.TRUE.equals(redis.delete(key));
    }

    public Long incr(String key) {
        return redis.opsForValue().increment(key);
    }

    public Long decr(String key) {
        return redis.opsForValue().decrement(key);
    }

    /** 執行 Lua 腳本（複合原子操作，如判空 + 扣減）。 */
    public <T> T eval(RedisScript<T> script, List<String> keys, Object... args) {
        return redis.execute(script, keys, args);
    }
}
