# SA.md — s38_etlsync（ETL / 大量資料同步）

> 事實文件（SASD）· 結構化分析。描述「ETL 同步要做什麼」的**業務行為**，平台無關（P1..Pn 共用；各平台怎麼扛量在各自 SD）。
> 特性：大批量搬運 + 加工，批次性強；用**緩衝當蓄水池削峰**，在「正確（不重不漏、可重跑）」與「不把目標端寫爆」之間平衡。

## 1. Context Diagram

```text
[排程 / 觸發] --(同步: source, target, 範圍)--> (ETL) --(入庫完成 / 進度)--> [排程]

(ETL) --(抽取)--> [來源系統]
(ETL) --(緩衝削峰)--> [MQ / 緩衝]
(ETL) --(清洗後寫入)--> [目標庫 / 數倉]
```

## 2. DFD（精簡 box flow）

```text
[抽取] --(依 watermark 增量 / 全量)--> [來源]
   ▼
[清洗 / 轉換 / 去重]（欄位映射、格式化、依 key 去重）
   ▼
[緩衝蓄水池]（削掉來源尖峰，控制寫入目標的速率）--> [MQ]
   ▼
[分批寫入目標]（受控批量，不壓垮目標端）
   ▼
[推進 watermark]（記錄已同步位置，供增量 / 重跑）
```

## 3. Process Spec（行為基準，decision table）

```text
[抽取]
├─ 增量：依 watermark（時間戳 / 自增位點）只抽新資料
└─ 全量：初始化 / 補跑

[去重 / 冪等]（核心不變量）
└─ 依業務 key 去重；重跑同一批不得產生重複資料（upsert / 唯一鍵）

[削峰]
└─ 來源突然吐一大批（補跑 / 初始化）→ 緩衝蓄水池平滑，分批受控寫入目標

[正確性]
├─ 不重（去重 / 冪等）
├─ 不漏（watermark 不跳過；失敗批可重跑）
└─ 可批次延遲（多數 ETL 允許）
```

## 4. State Transition（一個同步批次）

```text
【PENDING】──(開始)──> 【EXTRACTING】──> 【LOADING】──(完成)──> 【DONE（推進 watermark）】
                                              └──(失敗)──> 【FAILED】──(重跑·冪等)──> EXTRACTING
```

## 5. Data Dictionary（邏輯欄位；實體 DDL 在各平台 SD）

```text
etl_batch（同步批次）
├─ batchId(PK) / source / target / mode（INCR|FULL）
├─ watermarkFrom / watermarkTo / status
└─ startedAt / finishedAt / rowCount
```
