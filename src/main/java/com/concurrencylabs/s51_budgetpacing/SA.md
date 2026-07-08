# SA.md — s51_budgetpacing（廣告預算 pacing 系統）

> 事實文件（SASD）· 結構化分析。描述「預算 pacing 要做什麼」的**業務行為**，平台無關（P1..Pn 共用；各平台怎麼扛量在各自 SD）。
> 特性：即時感知「花了多少」控投放快慢；每筆精算會打爆花費計數器，故用**近似扣減求快、事後對帳修正**（最終一致、不超花）。

## 1. Context Diagram

```text
[投放 / 競價] --(花費事件: campaignId, cost)--> (pacing) --(投放快慢決策: 放行/放慢/停)--> [投放]

(pacing) --(近似花費累加)--> [util.redis counter]
(pacing) --(對帳修正)------> [計費真相 / PostgreSQL]
```

## 2. DFD（精簡 box flow）

```text
[花費事件] --(cost)--> [近似累加花費 counter]（求快，不逐筆精算）
   ▼
[pacing 判定]（已花 vs 日預算 + 時間進度）
   ├─(進度正常)──> 正常投放
   ├─(燒太快 / 逼近預算)──> 放慢投放 / 降頻
   └─(達預算)──> 暫停投放
   ▼
（事後）[對帳] 用計費真相修正近似誤差 → 校正累計花費
```

## 3. Process Spec（行為基準，decision table）

```text
[近似扣減]（求快）
└─ 花費用近似 counter 即時累加；不逐筆精算（否則熱點被打爆、拖慢競價）

[pacing 決策]
├─ 花費進度 <= 時間進度 → 正常投放
├─ 花費超前 → 放慢 / 降頻（平順花一天）
└─ 達日預算 → 暫停

[最終一致]（核心）
└─ 近似有誤差 → 事後對帳用計費真相修正；目標「不超花」
   容忍即時值短暫不精確，最終帳要對得平

不變量：即時求快用近似，但最終花費以對帳為準、不得超出預算（超花無人買單）。
```

## 4. State Transition（一個活動的 pacing 狀態）

```text
【NORMAL】──(燒太快)──> 【SLOWING（放慢）】──(達預算)──> 【CAPPED（暫停）】
   ▲────────(對帳修正 / 新一日重置)────────┘
```

## 5. Data Dictionary（邏輯欄位；實體 DDL 在各平台 SD）

```text
campaign_spend（活動花費）
├─ campaignId(PK) / dailyBudget / spentApprox（近似，活在 Redis）
├─ spentReconciled（對帳後）/ pacingState（NORMAL|SLOWING|CAPPED）
└─ date
```
