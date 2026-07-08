# SA.md — s48_exchangeassetwallet（交易所資產錢包）

> 事實文件（SASD）· 結構化分析。描述「資產錢包要做什麼」的**業務行為**，平台無關（P1..Pn 共用；各平台怎麼扛量在各自 SD）。
> 交易所地基：管理每個使用者每種資產的**可用 / 凍結**兩態，被下單 / 撮合 / 充提 / 活動全部依賴。要在「成交事件雨」下每筆正確，並與非核心流量硬隔離。

## 1. Context Diagram

```text
[下單 s42 / 撮合 s41 / 充提 s46,s47] --(資產操作: userId, asset, delta, bizNo)--> (資產錢包) --(成功 / 拒絕)--> [呼叫方]

(資產錢包) --(可用/凍結變更 + append 流水)--> [PostgreSQL asset / asset_ledger]
```

## 2. DFD（精簡 box flow）

```text
[資產操作] --(bizNo)--> [去重] --(bizNo 唯一)-->
   │(已處理)──> 回上次結果
   │(首次)
   ▼
[原子變更兩態]（同一交易內完成）
   ├─ 下單凍結：可用 -X、凍結 +X（可用不足 → 拒）
   ├─ 成交扣減：凍結 -X、對手方可用 +Y
   ├─ 撤單解凍：凍結 -X、可用 +X
   └─ 充值 / 提幣：可用 +/-X
   ▼
[append 流水] --> 回 { 成功, 可用, 凍結 }
```

## 3. Process Spec（行為基準，decision table）

```text
[去重 / 冪等]（bizNo）
├─ 首次 → 執行；已存在 → 回上次結果（絕不重複變更）

[兩態不變量]（核心）
├─ 可用 >= 0、凍結 >= 0（任何操作不得使其為負）
├─ 下單：可用 -X / 凍結 +X（總額不變）；可用不足即拒
├─ 成交：凍結 -X（實扣）/ 對手方可用 +Y
└─ 撤單：凍結 -X / 可用 +X（總額不變）

[餘額 vs 流水]
└─ (可用 + 凍結) 的每次變動 = 對應流水；兩者同交易一致、可追溯

[熱點]
└─ 活躍帳戶 / 熱門幣種高頻讀寫 → 分片 / 分桶（見各平台 SD）

[硬隔離]
└─ 資產核心與行情 / 活動 / 通知等非核心流量資源隔離，不可被拖垮
```

## 4. State Transition

```text
（資產帳 append-only 流水；可用/凍結為結果快照。凍結子狀態：FROZEN ──成交──> DEBITED / ──撤單──> RELEASED）
```

## 5. Data Dictionary（邏輯欄位；實體 DDL 在各平台 SD）

```text
asset（資產餘額，結果）
├─ userId + asset（複合 PK）/ available / frozen
└─ updatedAt

asset_ledger（資產流水，append-only 真相）
├─ entryId(PK) / userId / asset / delta / bucket（AVAILABLE|FROZEN）
├─ type（ORDER_FREEZE|TRADE|CANCEL|DEPOSIT|WITHDRAW）
├─ bizNo（唯一，去重）/ createdAt
```
