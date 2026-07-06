package com.concurrencylabs.s01_lottery.P1.usecase;

import com.concurrencylabs.common.account.AccountService;
import com.concurrencylabs.s01_lottery.P1.error.ErrorCode;
import com.concurrencylabs.s01_lottery.P1.error.LotteryException;
import com.concurrencylabs.s01_lottery.P1.model.LotteryActivity;
import com.concurrencylabs.s01_lottery.P1.model.LotteryDrawRecord;
import com.concurrencylabs.s01_lottery.P1.model.LotteryPrize;
import com.concurrencylabs.s01_lottery.P1.object.bo.DrawOutcome;
import com.concurrencylabs.s01_lottery.P1.object.dto.DrawReq;
import com.concurrencylabs.s01_lottery.P1.object.dto.DrawResp;
import com.concurrencylabs.s01_lottery.P1.service.DrawRecordService;
import com.concurrencylabs.s01_lottery.P1.service.LotteryActivityService;
import com.concurrencylabs.s01_lottery.P1.service.LotteryDrawService;
import com.concurrencylabs.s01_lottery.P1.service.LotteryGuardService;
import com.concurrencylabs.s01_lottery.P1.service.LotteryIdempotencyService;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 抽獎流程編排（mapping SA DFD / SD Sequence）：一個 // 註解 = 一行 service 呼叫。
 *
 * <p>不加 @Transactional：跨 Redis + DB 的 saga，靠補償 + 冪等，不是單一 ACID 交易
 * （見 DEVELOPMENT §四 @Transactional 邊界、§五 並發不變量）。
 */
@Service
public class DrawLotteryUsecase {

    private static final long POINTS_COST = 200L;

    private final LotteryGuardService guardService;
    private final LotteryActivityService activityService;
    private final AccountService accountService;
    private final LotteryDrawService drawService;
    private final DrawRecordService drawRecordService;
    private final LotteryIdempotencyService idempotencyService;

    public DrawLotteryUsecase(LotteryGuardService guardService,
                              LotteryActivityService activityService,
                              AccountService accountService,
                              LotteryDrawService drawService,
                              DrawRecordService drawRecordService,
                              LotteryIdempotencyService idempotencyService) {
        this.guardService = guardService;
        this.activityService = activityService;
        this.accountService = accountService;
        this.drawService = drawService;
        this.drawRecordService = drawRecordService;
        this.idempotencyService = idempotencyService;
    }

    public DrawResp draw(Long activityId, DrawReq req) {
        String idem = req.getIdempotencyKey();

        // 去重 claim
        LotteryIdempotencyService.Claim claim = idempotencyService.claim(idem);
        if (claim == LotteryIdempotencyService.Claim.DONE) {
            return replay(idem);
        }
        if (claim == LotteryIdempotencyService.Claim.PENDING) {
            throw new LotteryException(ErrorCode.PROCESSING);
        }

        try {
            // 驗活動
            LotteryActivity activity = guardService.checkActivity(activityId);
            // 取獎品設定
            List<LotteryPrize> prizes = activityService.listPrizes(activityId);
            // 扣 200 積分
            guardService.chargePoints(req.getUserId(), POINTS_COST);

            DrawOutcome outcome;
            Long drawId;
            try {
                // 抽獎
                outcome = drawService.draw(activity.getId(), prizes);
                // 記流水
                drawId = drawRecordService.record(activity.getId(), req.getUserId(), outcome, POINTS_COST, idem);
            } catch (RuntimeException systemFailure) {
                // 補償退款
                accountService.refundPoints(req.getUserId(), POINTS_COST);
                throw systemFailure;
            }

            // 標記完成
            idempotencyService.markDone(idem);
            return DrawResp.of(drawId, outcome);

        } catch (RuntimeException e) {
            idempotencyService.release(idem);
            throw e;
        }
    }

    /** 同一 idempotencyKey 重送 → 回放上次結果。 */
    private DrawResp replay(String idem) {
        LotteryDrawRecord rec = drawRecordService.findByIdempotencyKey(idem)
                .orElseThrow(() -> new LotteryException(ErrorCode.PROCESSING));
        if (rec.isWin()) {
            LotteryPrize p = activityService.findPrize(rec.getPrizeId());
            return (p != null)
                    ? DrawResp.win(rec.getId(), p.getId(), p.getName(), p.getLevel())
                    : DrawResp.win(rec.getId(), rec.getPrizeId(), null, null);
        }
        return DrawResp.lose(rec.getId());
    }
}
