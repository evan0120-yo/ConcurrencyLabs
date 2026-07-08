# SA.md — s03_jackpotpool（彩金獎池累積系統）

> 事實文件（SASD）· 結構化分析。描述「獎池累積要做什麼」的**業務行為**，平台無關（P1..Pn 共用；各平台怎麼扛量在各自 SD）。
> 特性：同一個數字**高頻累加 + 高頻圍觀讀**；顯示可近似，中獎結算必須強一致（真錢）。

## 1. Context Diagram

```text
[下注抽成事件] --(累加: poolId, amount)--> (獎池) --(累加成功)--> 
[玩家看池]     --(poolId)-----------------> (獎池) --(池值·近似)--> [玩家]
[中獎]         --(poolId, winnerId)-------> (獎池) --(結算派彩·精準)--> [玩家錢包]

(獎池) --(counter 累加 / 讀)--> [util.redis]
(獎池) --(池值快照 / 結算流水)--> [PostgreSQL]
```

## 2. DFD（精簡 box flow）

```text
【累加（寫爆）】[下注抽成] --(amount)--> [原子累加 counter] --> [util.redis]

【圍觀（讀爆）】[看池] --> 讀 counter（近似、可稍舊）

【中獎（強一致冷路徑）】
[中獎] --> [結算派彩]（精準取當前池值 → 派給中獎者，走 s06/s25 記帳）
   ▼
[池歸零] --> 重新累積
```

## 3. Process Spec（行為基準，decision table）

```text
[累加]
└─ 每筆抽成原子累加到池 counter（高頻寫）

[顯示]（放寬）
└─ 讀池允許近似 / 稍舊（不涉及錢，換取扛住圍觀讀爆）

[中獎結算]（強一致，真錢）
├─ 精準鎖定當前池值 → 派彩給中獎者（冪等、不可錯不可重複派）
└─ 派彩完成 → 池歸零，重新累積

不變量：顯示可近似，但「結算派出的金額」必須精準且只發生一次。
```

## 4. State Transition（一個獎池）

```text
【ACCUMULATING（累積中）】──(中獎)──> 【SETTLING（結算派彩，強一致）】──> 【RESET（歸零重累積）】
```

## 5. Data Dictionary（邏輯欄位；實體 DDL 在各平台 SD）

```text
jackpot_pool（獎池）
├─ poolId(PK) / currentValue（活在 Redis counter，快照落 DB）
└─ status（ACCUMULATING|SETTLING）

settlement（結算流水）
├─ settlementId(PK) / poolId / winnerId / amount
├─ idemKey（冪等，同次中獎只派一次）
└─ settledAt
```
