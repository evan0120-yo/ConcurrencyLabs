package com.concurrencylabs.s01_lottery.P1.service;

import com.concurrencylabs.s01_lottery.P1.model.LotteryActivity;
import com.concurrencylabs.s01_lottery.P1.model.LotteryPrize;
import com.concurrencylabs.s01_lottery.P1.repository.LotteryActivityRepository;
import com.concurrencylabs.s01_lottery.P1.repository.LotteryPrizeRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

/**
 * 活動 / 獎品設定的純讀取（驗證邏輯移至 {@link LotteryGuardService}）。
 * 單筆讀取不加 @Transactional（Spring Data 已內建；見 DEVELOPMENT §四 @Transactional 邊界）。
 */
@Service
public class LotteryActivityService {

    private final LotteryActivityRepository activityRepository;
    private final LotteryPrizeRepository prizeRepository;

    public LotteryActivityService(LotteryActivityRepository activityRepository,
                                  LotteryPrizeRepository prizeRepository) {
        this.activityRepository = activityRepository;
        this.prizeRepository = prizeRepository;
    }

    public Optional<LotteryActivity> findActivity(Long activityId) {
        return activityRepository.findById(activityId);
    }

    public List<LotteryPrize> listPrizes(Long activityId) {
        return prizeRepository.findByActivityId(activityId);
    }

    public LotteryPrize findPrize(Long prizeId) {
        return prizeRepository.findById(prizeId).orElse(null);
    }
}
