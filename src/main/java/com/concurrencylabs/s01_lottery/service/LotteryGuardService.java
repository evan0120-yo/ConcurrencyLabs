package com.concurrencylabs.s01_lottery.service;

import com.concurrencylabs.common.account.AccountService;
import com.concurrencylabs.common.account.DeductResult;
import com.concurrencylabs.s01_lottery.enums.ActivityStatus;
import com.concurrencylabs.s01_lottery.error.ErrorCode;
import com.concurrencylabs.s01_lottery.error.LotteryException;
import com.concurrencylabs.s01_lottery.model.LotteryActivity;
import org.springframework.stereotype.Service;

import java.time.Instant;

/**
 * 抽獎前置守門：把「檢查不合法就 throw」的守門邏輯集中在此，
 * 讓 Usecase 每個守門只呈現一行（見 DEVELOPMENT §六）。
 */
@Service
public class LotteryGuardService {

    private final LotteryActivityService activityService;
    private final AccountService accountService;

    public LotteryGuardService(LotteryActivityService activityService, AccountService accountService) {
        this.activityService = activityService;
        this.accountService = accountService;
    }

    /** 驗活動可抽：未存在 / 未開始 / 已結束一律 throw。 */
    public LotteryActivity checkActivity(Long activityId) {
        LotteryActivity a = activityService.findActivity(activityId)
                .orElseThrow(() -> new LotteryException(ErrorCode.ACTIVITY_NOT_FOUND));
        Instant now = Instant.now();
        if (a.getStatus() == ActivityStatus.ENDED || now.isAfter(a.getEndAt())) {
            throw new LotteryException(ErrorCode.ACTIVITY_ENDED);
        }
        if (a.getStatus() == ActivityStatus.NOT_STARTED || now.isBefore(a.getStartAt())) {
            throw new LotteryException(ErrorCode.ACTIVITY_NOT_STARTED);
        }
        return a;
    }

    /** 扣積分（原子）；不足即 throw，並把 account 的結果翻成 lottery 錯誤碼。 */
    public void chargePoints(String userId, long cost) {
        if (accountService.deductPoints(userId, cost) == DeductResult.INSUFFICIENT) {
            throw new LotteryException(ErrorCode.INSUFFICIENT_POINTS);
        }
    }
}
