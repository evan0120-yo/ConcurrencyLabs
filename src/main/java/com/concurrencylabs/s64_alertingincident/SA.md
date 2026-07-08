# SA.md — s64_alertingincident（Alerting / Incident 通知）

> 事實文件（SASD）· 結構化分析。描述「告警要做什麼」的**業務行為**，平台無關（P1..Pn 共用；各平台怎麼扛量在各自 SD）。
> 告警的價值在「可行動」不在「量多」。最需要告警的大事故時刻，也是告警最容易變成「風暴淹沒重點」的時刻——**去重 / 聚合 / 抑制**是關鍵。

## 1. Context Diagram

```text
[指標 / 事件] --(觸發規則)--> (告警) --(收斂後的可行動告警)--> [值班人 / 通知渠道]

(告警) --(規則評估 / 告警狀態)--> [PostgreSQL alert / incident]
(告警) --(派送 / 重送)--------> [通知渠道: 電話/IM/簡訊]
```

## 2. DFD（精簡 box flow）

```text
[指標/事件] --> [規則評估]（門檻 / 持續時間）
   │(觸發)
   ▼
[去重]（相同告警重複觸發 → 併為一條，不重送）
   ▼
[聚合 / 關聯]（一個根因炸出的海量子告警 → 收斂成「機房 X 故障」一則）
   ▼
[抑制]（依賴關係：上游掛了，壓掉必然連帶的下游告警）
   ▼
[派送]（依值班表 / 升級策略；未 ACK 升級重送）
```

## 3. Process Spec（行為基準，decision table）

```text
[規則評估]
└─ 指標 / 事件符合規則（門檻 + 持續）→ 觸發告警

[去重]
└─ 同一告警重複觸發 → 只保一條、更新計數，不重複轟炸

[聚合 / 抑制]（核心：防風暴）
├─ 聚合：同根因的大量子告警 → 收斂成少數根因告警
└─ 抑制：已知依賴（上游故障）→ 壓掉必然連帶的下游告警

[不漏]（不變量）
└─ 收斂但不得把「真正重要 / 獨立的告警」壓掉；根因與關鍵告警必達

[派送 / 升級]
└─ 依值班表派送；未 ACK → 升級 / 重送
```

## 4. State Transition（一個告警 / 事故）

```text
【FIRING】──(值班 ACK)──> 【ACKNOWLEDGED】──(恢復)──> 【RESOLVED】
   （多個 FIRING 告警可聚合為一個 incident 根因）
```

## 5. Data Dictionary（邏輯欄位；實體 DDL 在各平台 SD）

```text
alert（告警）
├─ alertId(PK) / ruleId / fingerprint（去重鍵）/ severity
├─ status（FIRING|ACKNOWLEDGED|RESOLVED）/ count
└─ firstAt / lastAt

incident（事故，聚合多告警）
├─ incidentId(PK) / rootCause / alertIds[] / status
```
