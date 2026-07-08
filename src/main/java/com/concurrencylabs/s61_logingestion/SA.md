# SA.md — s61_logingestion（Log Ingestion 系統）

> 事實文件（SASD）· 結構化分析。描述「Log 收集要做什麼」的**業務行為**，平台無關（P1..Pn 共用；各平台怎麼扛量在各自 SD）。
> 寫爆經典（與 IoT 心跳同家族）：緩衝削峰、按租戶分區、冷熱分層。服務出事時 log 暴增——最需要被收的時刻也最容易寫爆，**hot tenant 限流是保命關鍵**。

## 1. Context Diagram

```text
[服務 / agent] --(log: tenant, service, level, msg, ts)--> (Log 收集) --(ack)--> [agent]

(Log 收集) --(緩衝削峰)--> [MQ]
(Log 收集) --(近期熱存 / 舊冷存)--> [熱儲存 / 冷儲存]
```

## 2. DFD（精簡 box flow）

```text
[log 湧入] --> [快速收下 + 緩衝削峰] --> [MQ]
   ▼
[按租戶分區]（tenant 隔離；hot tenant 限流 / 抽樣）
   ▼
[冷熱分層寫入]
   ├─ 近期 log → 熱儲存（常查）
   └─ 舊 log → 冷儲存（少查、便宜）
```

## 3. Process Spec（行為基準，decision table）

```text
[接收]
└─ 快速收下 + 緩衝削峰；不同步處理

[放寬]
├─ 大量 log 可抽樣 / 限流（保統計價值即可）
└─ 可延遲入庫

[租戶分區 / hot tenant]（核心）
├─ 按 tenant 分區，資源隔離
└─ 某服務 bug → log 暴增 100 倍 → 對該 hot tenant 限流，不拖垮全平台

[冷熱分層]
└─ 近期熱、歷史冷；分層存放省成本
```

## 4. State Transition

```text
（ingestion 型，無業務狀態機；log 的 收下 → 緩衝 → 分區 → 冷熱落地 流程）
```

## 5. Data Dictionary（邏輯欄位；實體 DDL 在各平台 SD）

```text
log_entry（日誌）
├─ tenant / service / level（INFO|WARN|ERROR）/ msg / ts
└─（近期熱儲存 / 歷史冷儲存分層）

tenant_quota（租戶限流）
├─ tenant(PK) / ingestRateLimit / hot（是否暴量）
```
