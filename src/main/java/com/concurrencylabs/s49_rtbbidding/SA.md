# SA.md — s49_rtbbidding（RTB 即時競價系統）

> 事實文件（SASD）· 結構化分析。描述「RTB 競價要做什麼」的**業務行為**，平台無關（P1..Pn 共用；各平台怎麼扛量在各自 SD）。
> 天條：**硬性 timeout（~50ms）**，超時的回應直接作廢。逼你把重活預算好，臨場只留最輕量計算；寧可「夠好且來得及」，不要「最佳但太慢」。

## 1. Context Diagram

```text
[廣告交易所] --(競價請求: impressionId, 用戶/情境, deadline ~50ms)--> (RTB) --(出價 bid / 棄權)--> [廣告交易所]

(RTB) --(撈候選 / 素材)--> [預備好的候選廣告 / 快取]
```

## 2. DFD（精簡 box flow）

```text
[競價請求] --(帶 deadline)--> [硬 timeout 護欄]
   ▼
[撈候選]（開賣前已預備：定向匹配好的候選廣告 + 素材）
   ▼
[輕量計算出價]（只做來得及的輕算：預估 CTR × 出價策略）
   ├─(在 deadline 內算完)──> 回出價
   └─(來不及 / 無合適候選)──> 棄權（no-bid），不硬撐
```

## 3. Process Spec（行為基準，decision table）

```text
[硬 timeout]（核心天條）
├─ deadline 內回出價 → 有效
└─ 超時 → 回應作廢（等於棄權）；故臨場計算必須極輕

[重活預算]
└─ 定向 / 候選篩選 / 素材準備 一律預先做好；臨場不做重查詢 / 重算

[出價決策]
├─ 有合適候選 + 算得完 → 出價
└─ 無合適 / 來不及 → no-bid（乾脆棄權）

[取捨]
└─ 夠好且來得及 > 最佳但太慢
```

## 4. State Transition

```text
（單次競價無跨請求狀態機；一次請求 = 收到 → 輕算 → 出價 / 棄權 的極短生命週期）
```

## 5. Data Dictionary（邏輯欄位；實體 DDL 在各平台 SD）

```text
bid_request（競價請求，短生命）
├─ impressionId / userSegment / context / deadline
└─ result（BID+price | NO_BID）

campaign_candidate（預備候選，活在快取）
├─ campaignId / targeting / creativeRef / bidStrategy
```
