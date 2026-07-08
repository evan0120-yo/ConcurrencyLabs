# SA.md — s08_gametaskreward（遊戲任務獎勵系統）

> 事實文件（SASD）· 結構化分析。描述「任務獎勵要做什麼」的**業務行為**，平台無關（P1..Pn 共用；各平台怎麼扛量在各自 SD）。
> 特性：發獎**可與主遊戲解耦、可延遲**，但必須**不多發、不漏發**（冪等是核心）。

## 1. Context Diagram

```text
[遊戲進度事件] --(userId, taskId, 進度)--> (任務獎勵) --(達成則發獎)--> [玩家背包 / 錢包]
[玩家領獎]     --(userId, taskId)------> (任務獎勵) --(GRANTED / 已領 / 未達成)--> [玩家]

(任務獎勵) --(任務進度 / 領取狀態)--> [PostgreSQL task_progress / grant]
(任務獎勵) --(發道具 / 發幣)--------> [背包 or common.account]
```

## 2. DFD（精簡 box flow）

```text
[進度事件] --(taskId, 進度)--> [更新進度] --(達成條件?)--> [PostgreSQL task_progress]
   │(未達成)──> 只記進度
   │(達成)
   ▼
[冪等發獎] --(grant 單 SET idem NX：同 user 同 task 只發一次)--> [PostgreSQL grant]
   │(已發過)──> 跳過（防重領）
   │(首次)
   ▼
[實際發放] --(道具進背包 / 幣進 common.account)--> 標記 GRANTED
```

## 3. Process Spec（行為基準，decision table）

```text
[判定達成]
├─ 進度滿足任務條件 → 進入發獎
└─ 未滿足           → 只累積進度，不發

[冪等發獎]（key = userId + taskId）
├─ 首次（grant 單建立成功） → 實際發放
└─ 已存在（重送 / 重跑）     → 跳過，不重複發（防重領底線）

[實際發放]
├─ 道具 → 進背包
└─ 幣   → 以 grantId 當 common.account 冪等單號加款

[不漏發]
└─ 發放失敗可重跑：grant 單在、GRANTED 未完成 → 補發（at-least-once + 冪等）
```

## 4. State Transition（一個任務對一位玩家）

```text
【未達成】──(進度滿足)──> 【已達成/待發】──(發放成功)──> 【GRANTED 已領】
                                    └─(發放失敗)──> 重試補發（仍冪等，不重複）
```

## 5. Data Dictionary（邏輯欄位；實體 DDL 在各平台 SD）

```text
task_progress（任務進度）
├─ userId / taskId（複合 PK）/ progress / achievedAt
└─ status（IN_PROGRESS|ACHIEVED）

grant（發獎單）
├─ grantId(PK) / userId / taskId / rewardType（ITEM|COIN）/ rewardAmount
├─ idemKey（userId+taskId 唯一）/ status（PENDING|GRANTED）
└─ createdAt / grantedAt
```
