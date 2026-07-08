# SA.md — s36_reportexport（批次匯出 / 大量報表任務）

> 事實文件（SASD）· 結構化分析。描述「大匯出要做什麼」的**業務行為**，平台無關（P1..Pn 共用；各平台怎麼扛量在各自 SD）。
> 特性：大匯出天生慢，改**任務化非同步**產生、產完通知下載；掃大量資料要**分批 / 走副本**，絕不拖垮線上 OLTP 主庫。

## 1. Context Diagram

```text
[使用者] --(匯出: 條件, 格式)--> (報表匯出) --(受理 taskId → 產完通知可下載)--> [使用者]

(報表匯出) --(分批掃資料·走副本)--> [read replica / 數倉]
(報表匯出) --(產出檔案)---------> [object storage]
(報表匯出) --(任務狀態)---------> [PostgreSQL export_task]
```

## 2. DFD（精簡 box flow）

```text
[匯出請求] --> [建匯出任務]（受理即回 taskId，不同步產）
   ▼
[背景產檔]
   ├─ 分批查詢（游標 / 分頁，控每批量）
   ├─ 走讀副本 / 數倉（不打線上主庫）
   └─ 逐批拼檔 → 寫 object storage
   ▼
[產完] --> 標記 DONE + 通知使用者（可下載）
   └─(失敗)──> 重試 / 標記 FAILED
```

## 3. Process Spec（行為基準，decision table）

```text
[非同步任務化]
└─ 匯出即受理回 taskId，背景慢慢產；不阻塞請求、不卡瀏覽器

[資料源隔離]（核心不變量）
├─ 掃資料一律走讀副本 / 數倉，禁打線上 OLTP 主庫
└─ 分批（游標 / 分頁）讀取，控制每批量，避免一次吸乾

[產出 / 通知]
├─ 檔案落 object storage，給有時效的下載連結
└─ 產完通知；失敗可重試 / 明確標 FAILED

[併發控制]
└─ 月底集中匯出 → 限制同時進行的匯出任務數，避免資源被吃光
```

## 4. State Transition（一個匯出任務）

```text
【PENDING】──(開始產)──> 【RUNNING】──(完成)──> 【DONE（可下載）】
                              └──(失敗)──> 【FAILED】──(重試)──> RUNNING
```

## 5. Data Dictionary（邏輯欄位；實體 DDL 在各平台 SD）

```text
export_task（匯出任務）
├─ taskId(PK) / userId / criteria / format（CSV|XLSX）
├─ status（PENDING|RUNNING|DONE|FAILED）/ fileUrl / rowCount
└─ createdAt / finishedAt
```
