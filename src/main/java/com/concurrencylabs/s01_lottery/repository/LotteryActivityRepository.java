package com.concurrencylabs.s01_lottery.repository;

import com.concurrencylabs.s01_lottery.model.LotteryActivity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LotteryActivityRepository extends JpaRepository<LotteryActivity, Long> {
}
