# SA.md — s11_gameproviderapi（第三方遊戲商 API 串接）

> 事實文件（SASD）· 結構化分析。描述「provider 串接要做什麼」的**業務行為**，平台無關（P1..Pn 共用；各平台怎麼扛量在各自 SD）。
> 特性：**依賴不可控外部系統**，重點是「隔離 + 熔斷 + 重試 / fallback」，業務邏輯反而簡單。

## 1. Context Diagram

```text
[玩家 / 平台] --(玩某遊戲商遊戲)--> (provider 串接) --(呼叫)--> [外部遊戲商 API]
                                        │
                                        └--(正常結果 / 熔斷 fallback)--> [玩家 / 平台]
```

## 2. DFD（精簡 box flow）

```text
[請求] --(providerId, payload)--> [熔斷器判定]
   │(OPEN 熔斷中)──> 直接走 fallback（不打外部）
   │(CLOSED / HALF_OPEN)
   ▼
[呼叫外部 API]（帶 timeout、per-provider 隔離池 bulkhead）
   ├─(正常)──> 記成功、回結果
   ├─(逾時 / 失敗)──> [有限次重試 + 退避]
   │        └─(重試仍失敗)──> 累計失敗；達門檻 → 熔斷 OPEN → 走 fallback
   └─(HALF_OPEN 探測成功)──> 熔斷 CLOSED 恢復
```

## 3. Process Spec（行為基準，decision table）

```text
[Timeout]
└─ 每次外部呼叫設硬 timeout，逾時即視為失敗（不無限等）

[重試]
├─ 失敗可重試至上限 N 次，指數退避
└─ 重試不得雪崩：受隔離池 + 熔斷保護

[熔斷器]（per provider）
├─ CLOSED：失敗率 < 門檻 → 正常放行
├─ OPEN：失敗率 >= 門檻 → 快速失敗、走 fallback，冷卻一段時間
└─ HALF_OPEN：冷卻後放少量探測 → 成功則 CLOSED、失敗則回 OPEN

[隔離 bulkhead]
└─ 每個 provider 用獨立資源池；一家慢 / 掛不得吃光他家的資源

[fallback]
└─ 熔斷 / 失敗時回退：快取結果 / 預設值 / 明確錯誤（不硬拖）
```

## 4. State Transition（一個 provider 的熔斷器）

```text
【CLOSED】──(失敗率超標)──> 【OPEN】──(冷卻到期)──> 【HALF_OPEN】
     ▲                                                  │
     └───────────(探測成功)──────────────────────────────┘
                 （探測失敗 → 回 OPEN）
```

## 5. Data Dictionary（邏輯欄位；實體 DDL 在各平台 SD）

```text
provider_call（呼叫記錄，主要供觀測）
├─ callId(PK) / providerId / status（OK|TIMEOUT|FAIL|FALLBACK）/ retries
└─ latencyMs / createdAt

circuit_state（熔斷狀態，per provider）
├─ providerId(PK) / state（CLOSED|OPEN|HALF_OPEN）
└─ failCount / openedAt
```
