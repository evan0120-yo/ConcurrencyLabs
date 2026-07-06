package com.concurrencylabs.s01_lottery.P1.service;

import com.concurrencylabs.s01_lottery.P1.model.LotteryPrize;
import com.concurrencylabs.s01_lottery.P1.object.bo.DrawOutcome;
import com.concurrencylabs.util.redis.RedisUtil;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 抽獎核心：權重挑桶 + Redis Lua 原子扣獎品（SA Process Spec：抽獎品）。
 *
 * <p>不超發靠 Lua「判空 + 扣減」一步原子，不用分散式鎖（DEVELOPMENT §五）。
 */
@Service
public class LotteryDrawService {

    /** 判空 + 扣減：回 1=扣成功、0=已空、-1=未載入。 */
    private static final RedisScript<Long> DECR_SCRIPT = new DefaultRedisScript<>(
            "local r = redis.call('GET', KEYS[1])\n"
                    + "if r == false then return -1 end\n"
                    + "if tonumber(r) > 0 then redis.call('DECR', KEYS[1]) return 1 end\n"
                    + "return 0",
            Long.class);

    private final RedisUtil redisUtil;

    public LotteryDrawService(RedisUtil redisUtil) {
        this.redisUtil = redisUtil;
    }

    /** 依權重挑一個獎品桶，對真實獎品做原子扣減；銘謝惠遠桶或扣不到 → LOSE。 */
    public DrawOutcome draw(Long activityId, List<LotteryPrize> prizes) {
        LotteryPrize bucket = pickByWeight(prizes);
        if (bucket == null || bucket.isThanks()) {
            return DrawOutcome.lose();
        }
        Long r = redisUtil.eval(DECR_SCRIPT, List.of(stockKey(activityId, bucket.getId())));
        return (r != null && r == 1L) ? DrawOutcome.win(bucket) : DrawOutcome.lose();
    }

    /** 活動開始時把 DB 的 total_qty 載入 Redis 餘量（供整合測試 / 初始化用）。 */
    public void loadStock(Long activityId, List<LotteryPrize> prizes) {
        for (LotteryPrize p : prizes) {
            if (!p.isThanks()) {
                redisUtil.set(stockKey(activityId, p.getId()), String.valueOf(p.getTotalQty()));
            }
        }
    }

    private LotteryPrize pickByWeight(List<LotteryPrize> prizes) {
        int total = prizes.stream().mapToInt(LotteryPrize::getWeight).sum();
        if (total <= 0) {
            return null;
        }
        int r = ThreadLocalRandom.current().nextInt(total);
        int acc = 0;
        for (LotteryPrize p : prizes) {
            acc += p.getWeight();
            if (r < acc) {
                return p;
            }
        }
        return prizes.get(prizes.size() - 1);
    }

    private static String stockKey(Long activityId, Long prizeId) {
        return "lottery:stock:" + activityId + ":" + prizeId;
    }
}
