# SA.md — s33_backgroundjob（背景任務平台）

> 事實文件（SASD）· 結構化分析。描述「背景任務平台要做什麼」的**業務行為**，平台無關（P1..Pn 共用；各平台怎麼扛量在各自 SD）。
> 通用底座：把耗時工作非同步化，worker 可水平擴展；要在大量任務湧入時仍**可控（不雪崩）、可觀測、可人工介入**（限流 / 暫停 / 重跑）。

## 1. Context Diagram

```text
[業務方] --(丟 job: type, payload, opts)--> (背景任務平台) --(jobId 受理)--> [業務方]
[worker] --(撿 job / 回報結果)-----------> (背景任務平台)

(背景任務平台) --(佇列 / 狀態)--> [MQ / PostgreSQL job]
```

## 2. DFD（精簡 box flow）

```text
[提交 job] --> [入佇列]（依 type / 優先級）--> [MQ]
   ▼
[worker 撿取]（水平擴展；受限流閥門控制）
   ├─(成功)──> 標記 DONE
   ├─(失敗)──> 重試（退避，見 s35）
   └─(重試耗盡)──> 進死信（DLQ），待人工 / 補償
   ▼
[營運控制面]：限流（調 worker 併發）/ 暫停某類 type / 重跑 DLQ
```

## 3. Process Spec（行為基準，decision table）

```text
[解耦]
└─ 提交即受理回 jobId，業務不等執行完成

[執行]
├─ 成功 → DONE
├─ 失敗 → 重試（有限次 + 退避）
└─ 耗盡 → DEAD（進 DLQ）

[可控性]（核心價值）
├─ 限流：可調每類 type 的 worker 併發，避免下游被打垮
├─ 暫停：可暫停某類 job（維護 / 故障時）
└─ 重跑：DLQ / 指定 job 可重新投遞

[冪等]
└─ 同 jobId 重試 / 重跑不可產生重複副作用（執行需冪等）
```

## 4. State Transition（一個 job）

```text
【QUEUED】──(撿取)──> 【RUNNING】──(成功)──> 【DONE】
                            ├──(失敗·可重試)──> 【RETRYING】──> QUEUED
                            └──(重試耗盡)──> 【DEAD（DLQ）】──(重跑)──> QUEUED
```

## 5. Data Dictionary（邏輯欄位；實體 DDL 在各平台 SD）

```text
job（任務）
├─ jobId(PK) / type / payload / priority
├─ status（QUEUED|RUNNING|RETRYING|DONE|DEAD）/ attempts / maxAttempts
└─ createdAt / updatedAt

job_type_control（營運控制）
├─ type(PK) / paused / concurrencyLimit
```
