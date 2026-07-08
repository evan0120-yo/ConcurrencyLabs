# SA.md — s34_delayjob（延遲任務系統）

> 事實文件（SASD）· 結構化分析。描述「延遲任務要做什麼」的**業務行為**，平台無關（P1..Pn 共用；各平台怎麼扛量在各自 SD）。
> 特性：任務「到某時間點才執行」；難點在**到期洪峰**（同一時刻大批到期）要打散，且到期時**狀態可能已變**，要先校驗再執行。

## 1. Context Diagram

```text
[業務方] --(排延遲任務: runAt, type, payload)--> (延遲任務) --(taskId)--> [業務方]
[業務方] --(取消: taskId)--------------------> (延遲任務) --(cancelled)--> [業務方]

(延遲任務) --(到期觸發 → 校驗 → 執行)--> [業務回呼 / MQ]
(延遲任務) --(排程 / 狀態)-----------> [util.redis / PostgreSQL]
```

## 2. DFD（精簡 box flow）

```text
[排任務] --(runAt)--> [入延遲結構]（時間輪 / 有序集合，依到期時間）
   ▼（時間到）
[到期掃描]（打散：分桶 / 抖動，避免同刻集中爆）
   ▼
[狀態校驗]（到期時重新確認條件）
   ├─(條件已不成立: 例 已付款)──> 取消執行
   └─(條件仍成立: 未付款)──> 執行（如自動取消訂單）
   ▼
[執行]（冪等）──> 標記 DONE
```

## 3. Process Spec（行為基準，decision table）

```text
[時間精度]
└─ 在 runAt 附近觸發（不早於、不過度晚）；容忍小抖動

[到期打散]（核心痛點）
└─ 同一時刻大批到期 → 分桶 / 加隨機抖動平滑，避免到期洪峰壓垮

[狀態校驗]（到期時）
├─ 條件已變（已付款 / 已取消）→ 不執行
└─ 條件仍成立 → 執行

[冪等 / 取消]
├─ 同 taskId 重複觸發只執行一次
└─ 到期前可取消（付款成功 → 撤銷「自動取消」延遲任務）
```

## 4. State Transition（一個延遲任務）

```text
【SCHEDULED】──(到期·條件成立)──> 【FIRED】──> 【DONE】
        ├──(到期·條件已變)──> 【SKIPPED】
        └──(提前取消)──> 【CANCELLED】
```

## 5. Data Dictionary（邏輯欄位；實體 DDL 在各平台 SD）

```text
delay_task（延遲任務）
├─ taskId(PK) / type / payload / runAt
├─ status（SCHEDULED|FIRED|DONE|SKIPPED|CANCELLED）
├─ checkCondition（到期校驗依據，如 orderId）
└─ createdAt
```
