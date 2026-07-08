# SA.md — s32_b2bmultitenantisolation（B2B 多租戶 SaaS 隔離）

> 事實文件（SASD）· 結構化分析。描述「多租戶隔離要做什麼」的**業務行為**，平台無關（P1..Pn 共用；各平台怎麼扛量在各自 SD）。
> 兩條底線：**資料不能串**（安全紅線）、**資源不能互相拖累**（吵鬧鄰居）。大到一定程度的租戶要獨立資源池 / 庫。

## 1. Context Diagram

```text
[多家企業請求] --(帶 tenantId)--> (多租戶平台) --(僅該租戶資料 / 受 quota 限)--> [該租戶]

(多租戶平台) --(強制 tenant 範圍查詢)--> [PostgreSQL（含 tenantId）]
(多租戶平台) --(per-tenant quota / 限流)--> [util.redis]
```

## 2. DFD（精簡 box flow）

```text
[請求] --(tenantId 來自 token / 路由)--> [租戶識別]
   ▼
[資料隔離]（所有查詢強制加 tenant 範圍）
   └─ 跨租戶存取 → 一律拒（安全紅線）
   ▼
[資源隔離]（per-tenant quota / 限流）
   ├─(未超額)──> 正常服務
   └─(hot tenant 暴衝超額)──> 限流 / 降級（保護鄰居）
   ▼
[分級]（大租戶）──> 路由到獨立資源池 / 獨立庫
```

## 3. Process Spec（行為基準，decision table）

```text
[資料隔離]（安全紅線，不可妥協）
├─ 每筆資料帶 tenantId；所有查詢強制 tenant 過濾
└─ 跨租戶讀 / 寫 → 一律拒（A 絕不可見 B）

[資源隔離 / 吵鬧鄰居]
├─ per-tenant quota / 限流：單一租戶不得吃光共用資源
└─ hot tenant 暴衝 → 限流 / 降級，不連累其他租戶

[租戶分級]
├─ 小 / 中租戶 → 共用池（省成本）
└─ 大租戶（hot tenant）→ 獨立資源池 / 獨立 DB / cell

不變量：任何時刻 資料零跨租戶洩漏；任一租戶的高負載不得使其他租戶劣化。
```

## 4. State Transition（租戶資源分級）

```text
【SHARED（共用池）】──(成長 / hot)──> 【DEDICATED_POOL】──(超大)──> 【DEDICATED_DB / CELL】
```

## 5. Data Dictionary（邏輯欄位；實體 DDL 在各平台 SD）

```text
tenant（租戶）
├─ tenantId(PK) / name / tier（SHARED|DEDICATED_POOL|DEDICATED_CELL）
├─ quota（QPS / 儲存 / 連線）
└─ createdAt

（所有業務資料表一律含 tenantId 欄位並建索引；查詢強制帶 tenant 條件）
```
