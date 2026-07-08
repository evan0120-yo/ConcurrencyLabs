# SA.md — s72_dispatchmatching（外送 / 派單 / 供需匹配）

> 事實文件（SASD）· 結構化分析。描述「派單要做什麼」的**業務行為**，平台無關（P1..Pn 共用；各平台怎麼扛量在各自 SD）。
> 與 s09 同構的雙邊匹配，但多了**地理供需不均**：熱區（用餐尖峰）訂單爆炸 / 司機不足，要獨立調度；ETA / 定價尖峰可近似。

## 1. Context Diagram

```text
[訂單] --(派單: orderId, pickupLoc)--> (派單) --(配對司機 / 排隊)--> [顧客 / 司機]
[司機] --(位置 / 狀態上報)-----------> (派單) --(可派池更新)--> [調度池]

(派單) --(附近司機 / 匹配)--> [util.redis geo 池]
(派單) --(訂單 / 指派)------> [PostgreSQL order / assignment]
```

## 2. DFD（精簡 box flow）

```text
[訂單進] --(pickupLoc)--> [附近找候選司機]（geo 池）
   ▼
[匹配打分]（距離 / 順路 / 司機負載 / ETA 綜合）
   ├─(熱區·尖峰供不應求)──> 獨立調度 / 排隊等待
   └─(冷區)──> 放寬範圍配，別讓人等太久
   ▼
[指派]（一單指派一司機；防同一司機被兩單搶）
   ▼
（司機接受 / 拒絕 → 重派）
```

## 3. Process Spec（行為基準，decision table）

```text
[雙邊匹配]
└─ 訂單找司機 / 司機找訂單；一張單指派一位司機（防重複指派）

[匹配目標]
└─ 綜合 距離 / 順路 / 司機負載 / ETA；兼顧顧客、司機、平台效率

[地理供需不均]（核心）
├─ 熱區尖峰（訂單爆、司機少）→ 獨立調度池 / 排隊
└─ 冷區（司機少、單稀）→ 放寬配對範圍，減少等待

[近似]（放寬）
└─ ETA / 定價尖峰可用近似值求快

[司機位置流]
└─ 司機持續上報位置 / 狀態，更新可派池
```

## 4. State Transition（一張派單）

```text
【PENDING】──(匹配到)──> 【ASSIGNED】──(司機接受)──> 【ACCEPTED】──> 【PICKED/DELIVERING】
       ├──(司機拒絕/逾時)──> 重派（回 PENDING）
       └──(熱區無車)──> 【QUEUED】（排隊等待）
```

## 5. Data Dictionary（邏輯欄位；實體 DDL 在各平台 SD）

```text
dispatch_order（派單）
├─ orderId(PK) / pickupLoc / dropoffLoc / status（PENDING|ASSIGNED|ACCEPTED|QUEUED|...）
└─ assignedDriverId / createdAt

driver（司機，活在 geo 池）
├─ driverId(PK) / loc / status（IDLE|BUSY|OFFLINE）/ load
```
