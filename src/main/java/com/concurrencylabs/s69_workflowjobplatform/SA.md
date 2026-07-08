# SA.md — s69_workflowjobplatform（Workflow / Job Platform）

> 事實文件（SASD）· 結構化分析。描述「Workflow 平台要做什麼」的**業務行為**，平台無關（P1..Pn 共用；各平台怎麼扛量在各自 SD）。
> s33 背景任務的進階：管理「有依賴的多步驟長流程（DAG）」的狀態，讓任一步失敗都能**精準從斷點續跑**而非全部重來。

## 1. Context Diagram

```text
[觸發方] --(啟動 workflow: dagId, input)--> (Workflow 平台) --(runId / 各步狀態)--> [觸發方 / 觀測]

(Workflow 平台) --(步驟執行)--> [worker]
(Workflow 平台) --(流程 / 步驟狀態)--> [PostgreSQL run / step]
```

## 2. DFD（精簡 box flow）

```text
[啟動] --(依 DAG 定義)--> [排程就緒步驟]（前置依賴皆完成的步驟）
   ▼
[執行步驟]（交 worker，冪等）
   ├─(成功)──> 標記 DONE → 喚醒後續依賴步驟
   ├─(失敗)──> 重試；重試耗盡 → 標 FAILED（可從此步續跑，不整個重來）
   └─(全 DAG 完成)──> workflow DONE
   ▼
[控制面]：暫停某流程 / 回放 / 查每步狀態
```

## 3. Process Spec（行為基準，decision table）

```text
[DAG 依賴調度]
└─ 一步的前置依賴全 DONE → 該步才就緒可執行；並行無依賴步驟

[狀態持久 / 續跑]（核心）
├─ 每步狀態持久化
└─ 失敗 → 從失敗步（或指定步）續跑，不重跑已成功步驟

[冪等]
└─ 步驟執行冪等（重試 / 續跑不重複副作用）

[可控性]
└─ 暫停 / 回放 / 隔離不同流程；長流程全程可觀測（卡在哪一步一目了然）
```

## 4. State Transition（一個 workflow run / step）

```text
run：【RUNNING】──(所有步 DONE)──> 【COMPLETED】/（有步終失敗）──> 【FAILED】──(修復後續跑)──> RUNNING
step：【PENDING】──(依賴就緒)──> 【RUNNING】──(成功)──> 【DONE】/（失敗耗盡）──> 【FAILED】
```

## 5. Data Dictionary（邏輯欄位；實體 DDL 在各平台 SD）

```text
workflow_run（流程實例）
├─ runId(PK) / dagId / input / status（RUNNING|COMPLETED|FAILED|PAUSED）
└─ startedAt / finishedAt

step_run（步驟實例）
├─ runId / stepId（複合）/ dependsOn[] / status（PENDING|RUNNING|DONE|FAILED）
├─ attempts / output
└─ updatedAt
```
