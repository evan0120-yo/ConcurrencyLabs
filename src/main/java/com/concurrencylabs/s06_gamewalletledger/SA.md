# SA.md — s06_gamewalletledger（玩家錢包與帳本）

> 事實文件（SASD）· 結構化分析。描述「錢包帳本要做什麼」的**業務行為**，平台無關（P1..Pn 共用；各平台怎麼扛量在各自 SD）。
> 這是全平台的錢地基：下注 / 派彩 / 儲值 / 提現全打這裡。核心是**正確性**（不重不漏、餘額與流水永遠一致）。

## 1. Context Diagram

```text
[呼叫方: 下注/派彩/儲值/提現] --(扣加款: userId, amount, reason, bizNo)--> (錢包帳本) --(成功+新餘額 / 拒絕+原因)--> [呼叫方]

(錢包帳本) --(餘額增減 + append 流水)--> [PostgreSQL balance / ledger]
```

## 2. DFD（精簡 box flow）

```text
[呼叫方] --(扣/加款請求 bizNo)--> [去重] --(bizNo 唯一)--> [PostgreSQL ledger]
   │(已處理)──> 回上次結果（同 bizNo 不重複扣加）
   │(首次)
   ▼
[判定]
   ├─(扣款且餘額不足)──> 拒絕（餘額不足），餘額不動
   │(可執行)
   ▼
[原子變更餘額 + 記流水]（同一交易：餘額 ±amount、append 一筆 ledger）
   ▼
回應 { 成功, 新餘額 }
```

## 3. Process Spec（行為基準，decision table）

```text
[去重]（bizNo：一筆業務唯一單號）
├─ 首次      → 正常執行
└─ 已存在    → 回上次結果，不重複扣 / 加（冪等底線）

[扣款]
├─ 餘額 >= amount → 原子扣，記流水（-amount）
└─ 餘額 <  amount → 拒絕（餘額不足），餘額與流水都不動

[加款]
└─ 一律原子加，記流水（+amount）

[餘額 vs 流水 一致性]（不變量）
└─ 任一時刻：餘額 = 初始 + Σ 流水金額；兩者必須在同一交易內同進同退

[凍結 / 解凍]（供下注等用）
├─ 凍結：可用餘額 -= amount、凍結額 += amount（總額不變）
└─ 解凍 / 實扣：凍結額 -= amount（實扣再減總額）
```

## 4. State Transition（一筆帳目）

```text
（無）──(受理 bizNo)──> 【已入帳】 append-only，只增不改
      凍結金額另有子狀態：FROZEN ──(實扣)──> DEBITED / ──(解凍)──> RELEASED
```

## 5. Data Dictionary（邏輯欄位；實體 DDL 在各平台 SD）

```text
balance（餘額，當下結果）
├─ userId(PK) / available（可用）/ frozen（凍結）
└─ updatedAt

ledger（流水，歷史真相，append-only）
├─ ledgerId(PK) / userId / delta（±金額）/ reason（下注/派彩/儲值/提現…）
├─ bizNo（唯一，去重）/ balanceAfter（記帳後餘額，供對帳）
└─ createdAt
```
