# SA.md — s65_multitenantobservability（多租戶 Observability 隔離）

> 事實文件（SASD）· 結構化分析。描述「多租戶可觀測平台要做什麼」的**業務行為**，平台無關（P1..Pn 共用；各平台怎麼扛量在各自 SD）。
> 又是「吵鬧鄰居」——大客戶的重查詢（掃 30 天全量）不能拖垮小客戶（查最近 1 小時）。查詢資源與資料量都要按租戶隔離。

## 1. Context Diagram

```text
[多客戶: A/B/大客戶C] --(存/查 log/metrics/trace: tenantId)--> (Observability 平台) --(僅該租戶資料 / 受 quota)--> [客戶]

(平台) --(強制 tenant 範圍查詢)--> [log/metrics/trace 儲存（含 tenantId）]
(平台) --(per-tenant 查詢 quota)--> [查詢資源池]
```

## 2. DFD（精簡 box flow）

```text
[請求] --(tenantId)--> [租戶識別]
   ▼
[資料隔離]（所有查詢 / 寫入強制帶 tenant 範圍）
   └─ 跨租戶存取 → 一律拒
   ▼
[查詢資源隔離]（per-tenant 查詢 quota / 併發限制）
   ├─(小客戶輕查詢)──> 正常
   └─(大客戶掃全量重查詢)──> 獨立資源 / 限流，不吃光共用查詢池
   ▼
[分級]（大客戶）──> 獨立 shard / cell
```

## 3. Process Spec（行為基準，decision table）

```text
[資料隔離]（安全紅線）
├─ 每筆 log/metric/trace 帶 tenantId；查詢 / 寫入強制 tenant 過濾
└─ 跨租戶不可見（A 查不到 B）

[查詢資源隔離]（核心：吵鬧鄰居）
├─ per-tenant 查詢 quota / 併發限制
└─ 大客戶重查詢（掃 30 天全量）→ 獨立資源 / 限流，不拖垮小客戶

[租戶分級]
├─ 小 / 中租戶 → 共用 shard
└─ 大客戶（資料多、查詢重）→ 獨立 shard / cell

不變量：資料零跨租戶洩漏；任一租戶的重查詢不得使其他租戶查詢劣化。
```

## 4. State Transition（租戶資源分級）

```text
【SHARED】──(資料量/查詢量成長)──> 【DEDICATED_SHARD】──(超大)──> 【DEDICATED_CELL】
```

## 5. Data Dictionary（邏輯欄位；實體 DDL 在各平台 SD）

```text
tenant（租戶）
├─ tenantId(PK) / tier（SHARED|DEDICATED_SHARD|DEDICATED_CELL）
├─ queryQuota / storageQuota
└─（所有觀測資料含 tenantId 並建索引；查詢強制帶 tenant）
```
