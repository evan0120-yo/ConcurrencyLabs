package com.concurrencylabs.s01_lottery.P1.repository;

import com.concurrencylabs.s01_lottery.P1.model.LotteryPrize;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface LotteryPrizeRepository extends JpaRepository<LotteryPrize, Long> {

    List<LotteryPrize> findByActivityId(Long activityId);
}
