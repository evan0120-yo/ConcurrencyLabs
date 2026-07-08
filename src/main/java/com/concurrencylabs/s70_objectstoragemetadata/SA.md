# SA.md — s70_objectstoragemetadata（Object Storage Metadata 系統）

> 事實文件（SASD）· 結構化分析。描述「Metadata 系統要做什麼」的**業務行為**，平台無關（P1..Pn 共用；各平台怎麼扛量在各自 SD）。
> Metadata 本身不大，但「筆數」極多。單檔查詢很輕，但「list 上億筆的大 bucket」是重操作——不限流 / 分片會把整個服務拖垮。

## 1. Context Diagram

```text
[讀檔前 / 客戶端] --(查 metadata: key / list bucket)--> (Metadata) --(檔資訊 / 列表)--> [呼叫方]

(Metadata) --(metadata 讀寫)--> [PostgreSQL / KV（含 bucket/tenant 分片）]
```

## 2. DFD（精簡 box flow）

```text
[請求]
   ├─【單檔查詢】--(key)--> 直查 metadata（高頻但輕）
   └─【list 操作】--(bucket)-->
        ├─(小 bucket)──> 正常列出
        └─(大 bucket 上億筆)──> 限流 + 分頁（游標）+ 走分片 index
   ▼
回應（單檔資訊 / 分頁列表）
```

## 3. Process Spec（行為基準，decision table）

```text
[單檔查詢]（輕、高頻）
└─ 依 key 直查；讀爆靠快取 / 索引

[list 操作]（重、隱藏地雷）
├─ 一律分頁（游標 / continuation token），不一次回全量
├─ 大 bucket → 限流（防單一 list 拖垮服務）
└─ 大 bucket 的 index 分片，list 走分片並行 / 有序合併

[分片 / 多租戶]
└─ 按 bucket / tenant 分片；單一大 bucket 不得吃光共用資源

不變量：list 重操作必受限流 + 分頁保護，絕不允許一次掃全量的 list 打垮服務。
```

## 4. State Transition

```text
（metadata CRUD，無複雜業務狀態機；物件 metadata 的 建立 / 更新 / 刪除 生命週期）
```

## 5. Data Dictionary（邏輯欄位；實體 DDL 在各平台 SD）

```text
object_meta（物件 metadata）
├─ key(PK) / bucket / tenant / size / owner / path / contentType
├─ createdAt / updatedAt
└─（按 bucket / tenant 分片；list 依 key 前綴 + 游標分頁）
```
