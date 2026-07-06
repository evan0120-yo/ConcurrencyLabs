package com.concurrencylabs.s01_lottery.P1.service;

import com.concurrencylabs.s01_lottery.P1.model.LotteryDrawRecord;
import com.concurrencylabs.s01_lottery.P1.object.bo.DrawOutcome;
import com.concurrencylabs.s01_lottery.P1.repository.LotteryDrawRecordRepository;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Optional;

/**
 * 抽獎流水（每抽一筆，含未中獎）。
 * 單筆 save / find 不加 @Transactional（Spring Data 已內建）。
 */
@Service
public class DrawRecordService {

    private final LotteryDrawRecordRepository repository;

    public DrawRecordService(LotteryDrawRecordRepository repository) {
        this.repository = repository;
    }

    public Long record(Long activityId, String userId, DrawOutcome outcome,
                       long pointsCost, String idempotencyKey) {
        LotteryDrawRecord r = new LotteryDrawRecord();
        r.setActivityId(activityId);
        r.setUserId(userId);
        r.setPrizeId(outcome.isWin() ? outcome.getPrizeId() : null);
        r.setWin(outcome.isWin());
        r.setPointsCost(pointsCost);
        r.setDrawAt(Instant.now());
        r.setIdempotencyKey(idempotencyKey);
        return repository.save(r).getId();
    }

    public Optional<LotteryDrawRecord> findByIdempotencyKey(String idempotencyKey) {
        return repository.findByIdempotencyKey(idempotencyKey);
    }
}
