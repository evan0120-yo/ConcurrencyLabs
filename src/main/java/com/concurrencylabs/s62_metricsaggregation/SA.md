# SA.md — s62_metricsaggregation（Metrics Aggregation 系統）

> 事實文件（SASD）· 結構化分析。描述「Metrics 收集要做什麼」的**業務行為**，平台無關（P1..Pn 共用；各平台怎麼扛量在各自 SD）。
> 隱藏地雷：真正撐爆時序庫的常常不是流量，而是**標籤基數（cardinality）失控**。控制 cardinality + 預聚合降採樣 + 冷存是這題的獨門課題。

## 1. Context Diagram

```text
[各服務] --(指標: name, labels, value, ts)--> (Metrics) --(寫時序庫)--> [時序儲存]
[Dashboard] --(查詢: 指標, 時間範圍, 維度)--> (Metrics) --(聚合結果)--> [Dashboard]
```

## 2. DFD（精簡 box flow）

```text
[指標寫入] --(name + labels)--> [cardinality 控制]
   ├─(標籤組合過多 / 高基數標籤如 userId)──> 拒絕 / 丟棄 / 降維（保護時序庫）
   │(合法)
   ▼
[寫時序庫]
   ├─ 秒級原始 → 短期保留
   └─ 長期 → 降採樣（1 分 / 1 小時）+ 冷存
   ▼
[Dashboard 查詢] → 走預聚合 / 降採樣層（讀爆）
```

## 3. Process Spec（行為基準，decision table）

```text
[cardinality 控制]（核心地雷）
├─ 標籤組合數在可控範圍 → 正常寫入
└─ 高基數標籤（userId / requestId 等）→ 禁止 / 降維 / 告警
   （一個高基數標籤能讓時間序列數量暴增、撐爆庫）

[降採樣 / 冷存]
├─ 秒級原始 → 短期
└─ 長期 → 降採樣（不需秒級精度）+ 冷存省成本

[讀]
└─ Dashboard 查詢走預聚合層；長區間走降採樣資料
```

## 4. State Transition

```text
（時序資料，無業務狀態機；資料的 原始 → 降採樣 → 冷存 生命週期）
```

## 5. Data Dictionary（邏輯欄位；實體 DDL 在各平台 SD）

```text
metric_series（時間序列）
├─ metricName / labels（受 cardinality 限制）/ ts / value
└─ resolution（RAW|1m|1h）/ tier（HOT|COLD）

cardinality_guard（基數守門）
├─ metricName / maxSeries / blockedLabels[]
```
