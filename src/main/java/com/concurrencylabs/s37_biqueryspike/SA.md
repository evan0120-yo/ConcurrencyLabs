# SA.md — s37_biqueryspike（BI / 後台查詢爆炸）

> 事實文件（SASD）· 結構化分析。描述「BI 查詢要做什麼」的**業務行為**，平台無關（P1..Pn 共用；各平台怎麼扛量在各自 SD）。
> 特性：**重聚合 OLAP 讀**（掃大量列、GROUP BY），與線上交易 OLTP 型態完全不同；報表可接受稍舊，核心是**不與線上交易搶同一個庫**。

## 1. Context Diagram

```text
[營運 / PM] --(BI 查詢: 維度, 篩選, 時間範圍)--> (BI 查詢) --(聚合報表)--> [營運 / PM]

(BI 查詢) --(重聚合掃描，走分析源)--> [read replica / 數倉 OLAP]
```

## 2. DFD（精簡 box flow）

```text
[BI 查詢] --(聚合條件)--> [查分析源]
   ├─(命中預聚合 / 快取)──> 直接回
   │(未命中)
   ▼
[重聚合計算]（GROUP BY / 掃大量列）── 走 OLAP 副本 / 數倉，禁打 OLTP 主庫
   ▼
回應報表（允許稍舊）
```

## 3. Process Spec（行為基準，decision table）

```text
[OLAP / OLTP 分離]（核心不變量）
└─ 分析查詢一律走讀副本 / 數倉，絕不打支撐線上交易的 OLTP 主庫

[重聚合]
├─ 熱門報表 → 預聚合 / 物化視圖，命中直接回
└─ 臨時查詢 → 走數倉重算（可較慢）

[一致性]
└─ 報表可接受稍舊資料（分析源同步有延遲）；不要求與線上強一致

[併發]
└─ 開會前集中拉報表 → 限制並發重查詢數 / 資源隔離，避免互相拖垮
```

## 4. State Transition

```text
（讀取分析型，無業務狀態機；只有預聚合 / 物化視圖的 生成 / 刷新 週期）
```

## 5. Data Dictionary（邏輯欄位；實體 DDL 在各平台 SD）

```text
report_query（查詢定義）
├─ queryId(PK) / dimensions / filters / timeRange
└─ 走 OLAP 源

aggregate_mv（預聚合 / 物化視圖）
├─ mvId(PK) / metric / groupBy / refreshedAt
```
