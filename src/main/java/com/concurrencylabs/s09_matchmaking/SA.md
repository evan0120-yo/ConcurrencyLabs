# SA.md — s09_matchmaking（遊戲房間配對系統）

> 事實文件（SASD）· 結構化分析。描述「配對要做什麼」的**業務行為**，平台無關（P1..Pn 共用；各平台怎麼扛量在各自 SD）。
> 特性：**供需匹配 + 等待可接受**（連線保持），不涉及錢。

## 1. Context Diagram

```text
[玩家] --(開始配對: userId, mode, region, rank)--> (配對) --(MATCHED+roomId / QUEUED / CANCELLED)--> [玩家]

(配對) --(進池 / 湊人 / 取排隊)--> [util.redis 配對池]
(配對) --(開房、對局記錄)-------> [PostgreSQL room]
```

## 2. DFD（精簡 box flow）

```text
[玩家] --(開始配對)--> [進配對池] --(依 mode/region/rank 建票)--> [util.redis]
   ▼
[嘗試湊人] --(池內找條件相符者)-->
   │(湊滿門檻)──> [開房] --(建 room, 拉人進場)--> [PostgreSQL] ──> 回 MATCHED
   │(湊不滿)
   ▼
[排隊等待]（連線保持）
   ├─(有新玩家進池)──> 回到[嘗試湊人]
   ├─(等待逾時)──> 放寬配對條件 或 回 TIMEOUT
   └─(玩家取消)──> 移出池，回 CANCELLED
```

## 3. Process Spec（行為基準，decision table）

```text
[建票 / 進池]
└─ 一位玩家一張配對票（同人重複開始 → 覆蓋 / 冪等，不建兩張）

[湊人條件]
├─ 同 mode 必要；region / rank 在容忍範圍內 → 可配
└─ 湊滿人數門檻 → 開房；否則續留池中

[等待]
├─ 逾時未滿 → 放寬條件（擴大 rank 容忍）續配，或回 TIMEOUT
└─ 玩家主動取消 → 立即移出池

[開房]
└─ 一張票只能被開進一間房（防同一玩家被兩房同時搶）
```

## 4. State Transition（一張配對票）

```text
【QUEUED】──(湊滿)──> 【MATCHED】──> 進房
      ├─(逾時)──> 【TIMEOUT】
      └─(取消)──> 【CANCELLED】
```

## 5. Data Dictionary（邏輯欄位；實體 DDL 在各平台 SD）

```text
match_ticket（配對票，主要活在 Redis 池）
├─ ticketId(PK) / userId / mode / region / rank
├─ status（QUEUED|MATCHED|TIMEOUT|CANCELLED）
└─ enqueuedAt

room（房）
├─ roomId(PK) / mode / players[] / status（FORMING|READY）
└─ createdAt
```
