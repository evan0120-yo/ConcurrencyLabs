# SA.md — s05_payoutsettlement（派彩結算系統）

> 事實文件（SASD）· 結構化分析。描述「派彩結算要做什麼」的**業務行為**，平台無關（P1..Pn 共用；各平台怎麼扛量在各自 SD）。

## 1. Context Diagram

```text
[開獎事件] --(roundId, 開獎結果)--> (派彩結算) --(每位中獎者到帳 / 結算完成)--> [玩家錢包]

(派彩結算) --(撈該局注、標記已派)----> [PostgreSQL bet / payout]
(派彩結算) --(加錢入帳、冪等單號)-----> [common.account]
```

## 2. DFD（精簡 box flow）

```text
[開獎事件] --(roundId)--> [撈該局所有注] --(status=ACCEPTED)--> [PostgreSQL bet]
   ▼
[逐筆判定中獎] --(依開獎結果算應派金額)-->
   │(未中)──> 標記 SETTLED_LOSE（不派）
   │(中獎)
   ▼
[冪等派彩] --(payout 單 SET idem NX：同注同局只派一次)--> [PostgreSQL payout]
   │(已派過)──> 跳過（重跑安全）
   │(首次)
   ▼
[加錢入帳] --(以 payoutId 當冪等單號加錢)--> [common.account]
   ▼
[標記 bet=SETTLED_WIN] --> 該局全部處理完 → 局結算完成
```

## 3. Process Spec（行為基準，decision table）

```text
[判定中獎]
├─ 注符合開獎結果 → 算應派金額（依賠率），走派彩
└─ 不符合         → 標記未中，不派

[冪等派彩]（key = roundId + betId）
├─ 首次（payout 單建立成功） → 加錢入帳
└─ 已存在（重跑 / 重送）      → 跳過，不重複加錢

[加錢入帳]
└─ 以 payoutId 當 account 的冪等單號 → 同單號重入只加一次

[對帳]
└─ 該局派彩總額 = Σ 中獎注應派；不符 → 標記異常、停止並告警（絕不錯帳）

[重跑補償]
└─ 結算中斷後可整局重跑：已派的跳過、未派的補上（at-least-once 投遞 + 冪等 = exactly-once 效果）
```

## 4. State Transition（一張派彩單 / 一注）

```text
注：ACCEPTED ──(中,派彩完成)──> SETTLED_WIN
            └──(未中)────────> SETTLED_LOSE

派彩單：(無) ──(建立)──> PENDING ──(加錢成功)──> PAID
```

## 5. Data Dictionary（邏輯欄位；實體 DDL 在各平台 SD）

```text
payout（派彩單）
├─ payoutId(PK) / roundId / betId / userId
├─ amount / status（PENDING|PAID）
├─ idemKey（roundId+betId 唯一）
└─ createdAt / paidAt

（bet 見 s04；餘額增減走 common.account，以 payoutId 為冪等單號）
```
