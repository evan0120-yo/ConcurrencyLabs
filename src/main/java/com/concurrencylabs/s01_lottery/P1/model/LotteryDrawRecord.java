package com.concurrencylabs.s01_lottery.P1.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

/** 抽獎流水（lottery_draw_record）。每抽一筆，含未中獎；idempotencyKey 唯一。 */
@Entity
@Table(name = "lottery_draw_record")
@Getter
@Setter
@NoArgsConstructor
public class LotteryDrawRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "activity_id", nullable = false)
    private Long activityId;

    @Column(name = "user_id", nullable = false)
    private String userId;

    /** 中獎獎品；LOSE 時為 null（= 銘謝惠遠）。 */
    @Column(name = "prize_id")
    private Long prizeId;

    @Column(name = "is_win", nullable = false)
    private boolean win;

    @Column(name = "points_cost", nullable = false)
    private long pointsCost;

    @Column(name = "draw_at", nullable = false)
    private Instant drawAt;

    @Column(name = "idempotency_key", nullable = false, unique = true)
    private String idempotencyKey;
}
