package com.concurrencylabs.s01_lottery.P1.repository;

import com.concurrencylabs.s01_lottery.P1.model.LotteryActivity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LotteryActivityRepository extends JpaRepository<LotteryActivity, Long> {
}
