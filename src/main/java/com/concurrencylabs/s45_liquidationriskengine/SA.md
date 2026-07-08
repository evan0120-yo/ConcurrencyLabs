# SA.md — s45_liquidationriskengine（強平與風控引擎）

> 事實文件（SASD）· 結構化分析。描述「強平引擎要做什麼」的**業務行為**，平台無關（P1..Pn 共用；各平台怎麼扛量在各自 SD）。
> 特性：**價格事件驅動、極低延遲**；暴動時大量倉位同時逼近強平線——最需要算力的時刻正是所有倉位一起危險的時刻。風控算力必須優先、獨立。

## 1. Context Diagram

```text
[價格事件] --(symbol, price tick)--> (強平引擎) --(跌破維持保證金 → 觸發強平)--> [s41 撮合 / s48 錢包]

(強平引擎) --(倉位 / 保證金)--> [PostgreSQL position]
```

## 2. DFD（精簡 box flow）

```text
[價格每一跳] --(symbol, price)--> [重算受影響倉位]
   ▼
[逐倉風險判定]（保證金率 = 保證金 / 倉位價值）
   ├─(> 維持保證金率)──> 安全，略過
   └─(<= 維持保證金率)──> 【觸發強制平倉】
        ▼
   [市價平倉]（送撮合）+ 凍結/損益結算（走 s48）
        └─(平不掉，穿倉)──> 走風險準備金 / 自動減倉（ADL）
```

## 3. Process Spec（行為基準，decision table）

```text
[價格驅動]
└─ 價格每跳動即重算相關倉位風險（不是定時輪詢，是事件觸發）

[強平判定]
├─ 保證金率 > 維持保證金率 → 安全
└─ 保證金率 <= 維持保證金率 → 強制平倉（boundary-equal 視為觸發）

[暴動洪峰]（核心）
└─ 同一 symbol 大量倉位同時逼近強平線 → 引擎須在洪峰下即時算完
   → 風控算力獨立、優先於一般功能（不與別的流量搶）

[穿倉兜底]
└─ 市價平不掉（流動性不足）→ 風險準備金 / ADL，避免平台承損擴大

不變量：該平的倉位必須被即時、只一次地平掉；損益與資產變動對得起帳。
```

## 4. State Transition（一個倉位）

```text
【SAFE】──(價格惡化)──> 【AT_RISK】──(跌破維持)──> 【LIQUIDATING】──> 【CLOSED】
   ▲──────(價格回升 / 補保證金)──────┘
```

## 5. Data Dictionary（邏輯欄位；實體 DDL 在各平台 SD）

```text
position（倉位）
├─ positionId(PK) / userId / symbol / side / size / entryPrice
├─ margin / maintenanceMarginRate / status（SAFE|AT_RISK|LIQUIDATING|CLOSED）
└─ updatedAt

liquidation（強平記錄）
├─ liqId(PK) / positionId / triggerPrice / closedQty / pnl / ts
```
