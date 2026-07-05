package com.concurrencylabs.s01_lottery.repository;

import com.concurrencylabs.s01_lottery.model.LotteryDrawRecord;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface LotteryDrawRecordRepository extends JpaRepository<LotteryDrawRecord, Long> {

    Optional<LotteryDrawRecord> findByIdempotencyKey(String idempotencyKey);
}
