# SA.md — s42_ordercancelentry（下單 / 撤單入口）

> 事實文件（SASD）· 結構化分析。描述「下單入口要做什麼」的**業務行為**，平台無關（P1..Pn 共用；各平台怎麼扛量在各自 SD）。
> 撮合前的守門員：驗證 → 風控 → 凍結資產 → 送撮合。要快（別成撮合前瓶頸），資產凍結 / 解凍不能錯；**爆量時優先保撮合**，查詢讓路。

## 1. Context Diagram

```text
[使用者] --(下單/撤單: symbol, side, price, qty, clientOrderId)--> (下單入口) --(送撮合 / 拒絕)--> [使用者]

(下單入口) --(風控)--------> [s26 風控]
(下單入口) --(凍結/解凍資產)--> [s48 資產錢包]
(下單入口) --(送單 / 撤單)--> [s41 撮合]
```

## 2. DFD（精簡 box flow）

```text
【下單】
[下單] --(clientOrderId 去重)--> [驗證]（參數 / 交易對狀態）
   ▼
[風控]（額度 / 頻率 / 黑名單，毫秒）
   │(擋)──> 拒絕
   ▼
[凍結資產]（買凍 quote、賣凍 base）--> [s48]
   │(不足)──> 拒絕（餘額不足）
   ▼
[送撮合]（進 s41）──> ACCEPTED

【撤單】
[撤單] --> [送撮合撤單] --(撤成功)--> [解凍剩餘資產]
```

## 3. Process Spec（行為基準，decision table）

```text
[去重]（clientOrderId）：首次受理；重送回上次結果

[前置檢查]
├─ 驗證失敗 / 交易對停用 → 拒絕
└─ 風控擋 → 拒絕

[凍結 / 解凍]（資產正確性）
├─ 下單 → 凍結對應資產（買凍計價幣、賣凍標的幣）；不足即拒
├─ 撤單 / 未成交 → 解凍剩餘
└─ 成交 → 由結算把凍結轉實扣（見 s48）

[爆量取捨]（核心）
└─ 入口爆量 → 優先保撮合送單路徑；下單與查詢隔離，查詢可降級 / 讓路

不變量：任一時刻 凍結資產 = Σ未成交掛單應凍；撤單 / 成交後必正確解凍 / 轉扣。
```

## 4. State Transition（一張入口訂單意圖）

```text
【RECEIVED】──(檢查+凍結通過)──> 【SENT_TO_MATCH】──(撮合回報)──> 成交/掛單/撤單
        └──(驗證/風控/餘額不足)──> 【REJECTED】
```

## 5. Data Dictionary（邏輯欄位；實體 DDL 在各平台 SD）

```text
order_intent（下單意圖 / 入口記錄）
├─ clientOrderId(PK, 去重) / userId / symbol / side / price / qty
├─ frozenAsset / frozenAmount
├─ status（RECEIVED|SENT_TO_MATCH|REJECTED）
└─ createdAt
```
