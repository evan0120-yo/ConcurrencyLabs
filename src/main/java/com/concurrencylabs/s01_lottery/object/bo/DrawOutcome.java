package com.concurrencylabs.s01_lottery.object.bo;

import com.concurrencylabs.s01_lottery.model.LotteryPrize;
import lombok.AllArgsConstructor;
import lombok.Getter;

/** 抽獎內部結果（BO，內部載體）。LOSE 時 prize 相關欄位為 null。 */
@Getter
@AllArgsConstructor
public class DrawOutcome {

    private final boolean win;

    private final Long prizeId;

    private final String prizeName;

    private final Integer prizeLevel;

    public static DrawOutcome win(LotteryPrize prize) {
        return new DrawOutcome(true, prize.getId(), prize.getName(), prize.getLevel());
    }

    public static DrawOutcome lose() {
        return new DrawOutcome(false, null, null, null);
    }
}
