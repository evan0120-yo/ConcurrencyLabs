# SA.md — s25_ledger（Ledger 帳本系統）

> 事實文件（SASD）· 結構化分析。描述「帳本要做什麼」的**業務行為**，平台無關（P1..Pn 共用；各平台怎麼扛量在各自 SD）。
> 全金流的正確性根基：**只增不改**，餘額是結果、流水是真相。要同時做到不可竄改、可對帳、扛得住熱帳戶，且不被非核心流量拖垮。

## 1. Context Diagram

```text
[金流事件: 付款/退款/手續費/結算] --(記帳: accountId, delta, bizNo)--> (Ledger) --(成功 / 拒絕)--> [呼叫方]
[對帳 / 稽核]                     --(查流水 / 餘額)-----------------> (Ledger) --(流水明細 / 餘額)--> [呼叫方]

(Ledger) --(append 流水 + 餘額)--> [PostgreSQL ledger_entry / balance_snapshot]
```

## 2. DFD（精簡 box flow）

```text
[記帳請求] --(bizNo)--> [去重] --(bizNo 唯一)-->
   │(已記)──> 回上次結果（不重複記）
   │(首次)
   ▼
[append 流水]（只增不改，記 delta + balanceAfter）
   ▼
[更新餘額]（餘額 = 前餘額 + delta；可為快照）
   ▼
回應 { 成功, 新餘額 }

【對帳讀】攤開流水 → 逐筆可追溯 → 餘額 = Σ流水（可核對）
```

## 3. Process Spec（行為基準，decision table）

```text
[記帳冪等]（bizNo）
├─ 首次 → append 流水 + 更新餘額
└─ 已存在 → 回上次結果（不可重複記）

[append-only]（核心不變量）
├─ 流水只能新增，不可修改 / 刪除
└─ 更正只能用「反向沖銷」再記一筆（保留完整歷史）

[餘額 vs 流水]
└─ 任一時刻：餘額 = 初始 + Σ流水delta；兩者同交易一致；流水為權威真相

[熱帳戶]
└─ 平台 / 商戶主帳戶高頻記帳 → 需分片 / 分桶承接（見各平台 SD）

[隔離]
└─ 記帳（核心）與 對帳/報表（讀）分流；帳本不可被非核心流量拖垮
```

## 4. State Transition

```text
（帳目 append-only，無可變狀態；更正走反向沖銷分錄，不改原分錄）
```

## 5. Data Dictionary（邏輯欄位；實體 DDL 在各平台 SD）

```text
ledger_entry（流水，append-only 真相）
├─ entryId(PK) / accountId / delta（±）/ reason
├─ bizNo（唯一，去重）/ balanceAfter（記帳後餘額，供對帳）
└─ createdAt

balance_snapshot（餘額快照，結果）
├─ accountId(PK) / balance / lastEntryId
└─ updatedAt
```
