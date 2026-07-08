# SA.md — s24_paymentchannelrouting（多支付通道路由）

> 事實文件（SASD）· 結構化分析。描述「通道路由要做什麼」的**業務行為**，平台無關（P1..Pn 共用；各平台怎麼扛量在各自 SD）。
> 特性：在一堆會抽風的外部通道上，動態選「當下最可能成功」的那條，壞掉能立刻切換。業務簡單、重點是防禦與路由。

## 1. Context Diagram

```text
[付款請求] --(amount, 幣別, 國家, 商戶)--> (通道路由) --(選定通道 → 發起付款)--> [多個支付通道]
                                              │
                                              └--(通道健康度回饋)--> 路由決策
```

## 2. DFD（精簡 box flow）

```text
[付款] --(context)--> [候選通道篩選]（支援該幣別/國家/商戶）
   ▼
[路由排序]（依 即時成功率 / 成本 / 健康度）
   ▼
[呼叫首選通道]
   ├─(成功)──> 回結果、更新該通道健康度(好)
   ├─(慢 / 失敗)──> 更新健康度(差) → [切備援通道重試]
   └─(通道熔斷中)──> 直接跳過該通道
   ▼
（皆失敗）──> 回付款失敗（明確錯誤，不硬拖）
```

## 3. Process Spec（行為基準，decision table）

```text
[候選篩選]
└─ 只保留支援該 幣別 / 國家 / 商戶 的通道

[路由排序]
└─ 依即時 成功率 + 成本 + 健康度 綜合排序，挑首選

[健康度 / 熔斷]（per channel）
├─ 成功率驟降 / 連續失敗 → 標 DEGRADED / DOWN，暫時避開
└─ 冷卻後小流量探測 → 恢復則回 HEALTHY

[fallback]
└─ 首選失敗 → 依序切備援重試（重試有上限，避免雪崩）

[隔離 / 限流]
└─ 熱門通道限流；不同 國家 / 幣別 / 商戶 資源隔離，互不拖累
```

## 4. State Transition（一個通道的健康度）

```text
【HEALTHY】──(成功率降/連失敗)──> 【DEGRADED】──(持續惡化)──> 【DOWN】
     ▲                                                        │
     └──────────────(探測恢復)──────────────────────────────────┘
```

## 5. Data Dictionary（邏輯欄位；實體 DDL 在各平台 SD）

```text
channel（支付通道）
├─ channelId(PK) / supportedCurrencies / supportedCountries
├─ cost / successRate / health（HEALTHY|DEGRADED|DOWN）
└─ updatedAt

route_log（路由記錄，供觀測 / 調參）
├─ logId / txnRef / chosenChannel / fallbackChain[] / result
└─ createdAt
```
