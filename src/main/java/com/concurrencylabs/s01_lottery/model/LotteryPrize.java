package com.concurrencylabs.s01_lottery.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 獎品設定（lottery_prize，master）。即時餘量在 Redis，不在此表。
 *
 * <p>{@code thanks=true} 代表「銘謝惠遠」桶：被權重抽中即 LOSE，不做庫存扣減。
 */
@Entity
@Table(name = "lottery_prize")
@Getter
@Setter
@NoArgsConstructor
public class LotteryPrize {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "activity_id", nullable = false)
    private Long activityId;

    @Column(nullable = false)
    private String name;

    private Integer level;

    @Column(name = "total_qty", nullable = false)
    private long totalQty;

    /** 中獎權重（相對機率）。 */
    @Column(nullable = false)
    private int weight;

    /** 是否為「銘謝惠遠」桶。 */
    @Column(name = "is_thanks", nullable = false)
    private boolean thanks;
}
