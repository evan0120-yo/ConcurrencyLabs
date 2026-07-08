# SA.md — s74_datalakeingestion（Data Lake Ingestion / 冷熱資料分層）

> 事實文件（SASD）· 結構化分析。描述「資料湖灌入要做什麼」的**業務行為**，平台無關（P1..Pn 共用；各平台怎麼扛量在各自 SD）。
> 資料湖不只是「灌進來」——要處理 **schema 演進**與**小檔合併**，否則存得下卻查不動。寫爆為主；熱資料即時查、冷資料延遲入湖 + 便宜存。

## 1. Context Diagram

```text
[各方: 交易/事件/log/第三方] --(資料批 / 流)--> (資料湖 Ingestion) --(存 + 處理)--> [資料湖 / 數倉]

(Ingestion) --(schema 註冊 / 演進)--> [schema registry]
(Ingestion) --(小檔合併 / 冷熱分層)--> [object storage / 表格式]
```

## 2. DFD（精簡 box flow）

```text
[資料灌入] --(批 / 串流)--> [schema 演進處理]
   ├─ 對照 schema registry：新欄位 → 向前 / 向後相容套用
   └─ 不相容 → 隔離 / 告警（不讓壞 schema 汙染）
   ▼
[寫入資料湖]（串流會產生大量小檔）
   ▼
[compaction 小檔合併]（定期把小檔合併成大檔，否則查詢被拖垮）
   ▼
[冷熱分層]（近期熱 → 即時查層 / 歷史冷 → 便宜冷存）
```

## 3. Process Spec（行為基準，decision table）

```text
[schema 演進]（核心）
├─ 上游加欄位 → 向前 / 向後相容（歷史資料仍讀得到，新欄位有預設）
└─ 不相容變更 → 隔離 / 版本化，不讓歷史資料失效

[小檔合併 compaction]（隱藏地雷）
└─ 串流一直寫小檔 → 太多小檔查詢極慢 → 定期 compaction 合併大檔

[冷熱分層]
├─ 熱資料 → 即時查層
└─ 冷資料 → 延遲入湖 + 冷存便宜

[寫爆]
└─ 各方匯集寫入巨量 → 緩衝 + 分區承接

不變量：schema 變更不得使歷史資料失效；小檔須被 compaction，避免存得下卻查不動。
```

## 4. State Transition（一批資料 / 一個表分區）

```text
【INGESTED（小檔）】──(compaction)──> 【COMPACTED（大檔，好查）】──(老化)──> 【COLD（冷存）】
```

## 5. Data Dictionary（邏輯欄位；實體 DDL 在各平台 SD）

```text
dataset（資料集 / 表）
├─ name(PK) / schemaVersion / partitionScheme
└─ tier（HOT|COLD）

data_file（資料檔）
├─ path(PK) / dataset / partition / size / rowCount
└─ compacted（是否已合併）/ createdAt
```
