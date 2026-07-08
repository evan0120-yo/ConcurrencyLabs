# SA.md — s68_circuitbreakerdegradation（熔斷 / 降級平台）

> 事實文件（SASD）· 結構化分析。描述「熔斷降級要做什麼」的**業務行為**，平台無關（P1..Pn 共用；各平台怎麼扛量在各自 SD）。
> 故障會沿依賴鏈蔓延成雪崩（慢 → 資源耗盡 → 自己也掛 → 蔓延）。靠**熔斷「快速失敗」把故障關在局部**，並用降級守住核心。

## 1. Context Diagram

```text
[服務 A] --(呼叫下游 B)--> (熔斷降級) --(正常結果 / 快速失敗 → 降級)--> [服務 A]

(熔斷降級) --(下游健康度 / 熔斷狀態)--> [util.redis / 本地狀態]
```

## 2. DFD（精簡 box flow）

```text
[呼叫下游 B] --> [熔斷器判定]（per 依賴 B）
   ├─(OPEN 熔斷中)──> 直接快速失敗 → [降級]（不打 B、不卡執行緒）
   ├─(CLOSED / HALF_OPEN)
   │     ▼
   │  [呼叫 B]（隔離池 bulkhead 限制併發）
   │     ├─(正常)──> 記成功、回結果
   │     └─(慢 / 失敗)──> 記失敗；失敗率超標 → 熔斷 OPEN → 走降級
   └─(HALF_OPEN 探測成功)──> 恢復 CLOSED
```

## 3. Process Spec（行為基準，decision table）

```text
[熔斷器]（per 依賴）
├─ CLOSED：失敗率 < 門檻 → 正常放行
├─ OPEN：失敗率 >= 門檻 → 快速失敗、走降級，冷卻一段時間
└─ HALF_OPEN：冷卻後放少量探測 → 成功回 CLOSED、失敗回 OPEN

[快速失敗]（防雪崩核心）
└─ 下游不行時立即失敗，不讓請求卡著等 → 保住自身執行緒 / 資源

[隔離 bulkhead]
└─ 每個依賴獨立資源池；一個依賴的問題不吃光呼叫方全部資源

[降級]
└─ 事先定義「壞了給什麼」：快取值 / 預設值 / 精簡結果，保住核心功能
```

## 4. State Transition（一個依賴的熔斷器）

```text
【CLOSED】──(失敗率超標)──> 【OPEN（快速失敗+降級）】──(冷卻)──> 【HALF_OPEN】
     ▲──────────────(探測成功)──────────────┘   （探測失敗 → 回 OPEN）
```

## 5. Data Dictionary（邏輯欄位；實體 DDL 在各平台 SD）

```text
circuit_state（熔斷狀態，per 依賴）
├─ target(PK) / state（CLOSED|OPEN|HALF_OPEN）/ failRate / openedAt

fallback_policy（降級策略）
├─ target / type（CACHE|DEFAULT|SIMPLIFIED）/ config
```
