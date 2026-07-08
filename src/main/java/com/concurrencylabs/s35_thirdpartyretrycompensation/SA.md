# SA.md — s35_thirdpartyretrycompensation（第三方 API 重試補償）

> 事實文件（SASD）· 結構化分析。描述「重試補償要做什麼」的**業務行為**，平台無關（P1..Pn 共用；各平台怎麼扛量在各自 SD）。
> 特性：重試本意提高成功率，但**沒節制的重試會演變成雪崩**把對方與自己一起打垮。退避 + 死信 + 限速是命脈。

## 1. Context Diagram

```text
[業務方] --(委託呼叫外部: target, payload, idemKey)--> (重試補償) --(最終成功 / 進死信)--> [業務方]

(重試補償) --(呼叫)----> [外部 API]
(重試補償) --(重試佇列 / 死信)--> [MQ / DLQ]
```

## 2. DFD（精簡 box flow）

```text
[委託呼叫] --> [呼叫外部 API]（帶 idemKey）
   ├─(成功)──> 完成
   └─(失敗 / 逾時)
       ▼
   [排重試]（指數退避：1s → 2s → 4s …，加抖動）
       ├─(重試中成功)──> 完成
       └─(達重試上限仍失敗)──> [死信 DLQ]（人工 / 補償）
   ▼
[全域限速 / 熔斷]（對方大當機時，壓低整體重送速率，避免雪崩）
```

## 3. Process Spec（行為基準，decision table）

```text
[退避重試]（不可立刻 / 全部一起重送）
├─ 指數退避 + 抖動：給對方喘息、避免同刻重送共振
└─ 重試次數有上限

[防雪崩]（核心不變量）
├─ 對方 5xx / 逾時率飆高 → 熔斷 / 全域降速（別火上加油）
└─ 大批待重送 → 限速平滑釋放，不一次全轟

[死信兜底]
└─ 重試耗盡 → 進 DLQ，轉人工 / 補償；不無限重送

[冪等]
└─ 帶 idemKey：重送不得造成對方重複副作用（對方需支援冪等）
```

## 4. State Transition（一個外呼任務）

```text
【PENDING】──(失敗)──> 【RETRYING（退避中）】──(成功)──> 【DONE】
                              └──(耗盡)──> 【DEAD（DLQ）】──(補償/人工)──> 重放 / 結案
        （熔斷開時：暫緩所有重試，冷卻後再放）
```

## 5. Data Dictionary（邏輯欄位；實體 DDL 在各平台 SD）

```text
outbound_call（外呼任務）
├─ callId(PK) / target / payload / idemKey
├─ status（PENDING|RETRYING|DONE|DEAD）/ attempts / nextRetryAt
└─ createdAt

circuit_state（對 target 的熔斷，per target）
├─ target(PK) / state（CLOSED|OPEN|HALF_OPEN）/ failRate
```
