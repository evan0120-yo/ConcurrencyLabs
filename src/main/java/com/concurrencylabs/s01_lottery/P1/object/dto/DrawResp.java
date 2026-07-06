package com.concurrencylabs.s01_lottery.P1.object.dto;

import com.concurrencylabs.s01_lottery.P1.object.bo.DrawOutcome;
import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * 抽獎回應（邊界 DTO）。result = "WIN" | "LOSE"；LOSE 時 prize 為 null。
 */
@Data
@AllArgsConstructor
public class DrawResp {

    private String result;

    private Long drawId;

    private PrizeView prize;

    public static DrawResp of(Long drawId, DrawOutcome o) {
        return o.isWin()
                ? new DrawResp("WIN", drawId, new PrizeView(o.getPrizeId(), o.getPrizeName(), o.getPrizeLevel()))
                : new DrawResp("LOSE", drawId, null);
    }

    public static DrawResp win(Long drawId, Long prizeId, String name, Integer level) {
        return new DrawResp("WIN", drawId, new PrizeView(prizeId, name, level));
    }

    public static DrawResp lose(Long drawId) {
        return new DrawResp("LOSE", drawId, null);
    }
}
