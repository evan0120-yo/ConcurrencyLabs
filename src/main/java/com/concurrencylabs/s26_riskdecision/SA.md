# SA.md — s26_riskdecision（風控決策系統）

> 事實文件（SASD）· 結構化分析。描述「風控決策要做什麼」的**業務行為**，平台無關（P1..Pn 共用；各平台怎麼扛量在各自 SD）。
> 交易路徑上的守門員：**同步只放「快而確定」的硬規則（毫秒級 allow/deny）**，重運算的複雜模型挪到非同步事後補判，避免風控本身變成瓶頸。

## 1. Context Diagram

```text
[交易請求] --(txn 特徵: userId, amount, device, ip…)--> (風控) --(ALLOW / DENY，毫秒)--> [交易]

(風控) --(同步硬規則: 黑名單/額度/頻率)--> [util.redis / 規則庫]
(風控) --(風控事件)-----------------> [MQ]（非同步：複雜模型補判）
```

## 2. DFD（精簡 box flow）

```text
[交易進] --(特徵)-->
   ├─【同步路徑（毫秒）】
   │    [硬規則] 黑名單 / 額度 / 頻率 --> ALLOW / DENY
   │        └─ 快取化規則與名單，避免臨場慢查
   │
   └─【非同步路徑（事後）】
        發風控事件 → [MQ] → [複雜模型] 行為分析 / 關聯圖
             └─ 補判：標記可疑 / 更新名單 / 觸發二次驗證
```

## 3. Process Spec（行為基準，decision table）

```text
[同步硬規則]（必須毫秒級、確定）
├─ 命中黑名單 → DENY
├─ 超額度 / 超頻率 → DENY
└─ 皆過 → ALLOW（放行交易）

[同步路徑禁令]
└─ 只放「快而確定」的規則；任何重運算不得放在同步路徑（否則拖慢所有正常交易）

[非同步複雜模型]
├─ 事後行為分析 / 關聯圖 → 標記可疑、回饋名單、觸發二次驗證
└─ 允許延遲（事後補判），不阻塞當下交易

[取捨]
└─ 攔截力 vs 交易體驗：寧可同步先放行、非同步再補判，也不拖慢正常人
```

## 4. State Transition（一筆交易的風控標記）

```text
【同步 ALLOW】──(非同步補判可疑)──> 【FLAGGED】（標記 / 觸發二次驗證 / 更新名單）
【同步 DENY】──> 交易被擋
```

## 5. Data Dictionary（邏輯欄位；實體 DDL 在各平台 SD）

```text
risk_rule（硬規則）
├─ ruleId(PK) / type（BLACKLIST|LIMIT|FREQUENCY）/ params
└─ enabled

risk_event（風控事件，供事後模型 / 分析）
├─ eventId(PK) / txnId / decision（ALLOW|DENY）/ features
└─ createdAt / flagged（事後補判）
```
