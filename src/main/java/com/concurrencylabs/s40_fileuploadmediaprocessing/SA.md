# SA.md — s40_fileuploadmediaprocessing（檔案上傳 / 圖片影片處理）

> 事實文件（SASD）· 結構化分析。描述「上傳與媒體處理要做什麼」的**業務行為**，平台無關（P1..Pn 共用；各平台怎麼扛量在各自 SD）。
> 特性：**先安全收下、再慢慢加工**。上傳直傳儲存（不中轉、可續傳）；後處理輕重分級（縮圖優先、重轉碼排隊），別讓重活塞住輕活。

## 1. Context Diagram

```text
[使用者] --(要上傳: fileMeta)--> (上傳/媒體) --(pre-signed 直傳位址)--> [使用者]
[使用者] --(直傳大檔)-----------------------------------------------> [object storage]
[storage] --(上傳完成通知)--> (上傳/媒體) --(啟動後處理 pipeline)--> [worker]

(上傳/媒體) --(媒體 / 任務狀態)--> [PostgreSQL media / process_task]
```

## 2. DFD（精簡 box flow）

```text
[要上傳] --> [發 pre-signed 位址]（直傳 storage，不走後端中轉；大檔分段續傳）
   ▼（storage 通知上傳完成）
[建媒體記錄] --> [啟動後處理 pipeline]
   ▼
[掃毒]（先行；不安全直接拒 / 隔離）
   ▼
[分級後處理]
   ├─【縮圖】輕、快 → 優先佇列
   └─【多解析度轉碼(如 4K)】重、慢 → 排隊分級（低優先）
   ▼
[全部完成] --> media = READY
```

## 3. Process Spec（行為基準，decision table）

```text
[上傳]
├─ 直傳 object storage（pre-signed），後端不中轉（省頻寬 / 連線）
└─ 大檔分段 / 續傳（易斷，要能接續）

[掃毒先行]
├─ 安全 → 進後處理
└─ 不安全 → 拒絕 / 隔離，不對外提供

[後處理分級]（核心不變量）
├─ 縮圖等輕活 → 高優先，快出
└─ 重轉碼 → 低優先排隊；不得塞住輕活
   → 輕重用不同佇列 / worker 池

[可靠性]
└─ 各後處理步驟非同步、可重試；失敗不影響已完成步驟
```

## 4. State Transition（一個媒體檔）

```text
【UPLOADED】──> 【SCANNING】──(安全)──> 【PROCESSING（縮圖/轉碼）】──(全完成)──> 【READY】
                      └──(不安全)──> 【REJECTED】
```

## 5. Data Dictionary（邏輯欄位；實體 DDL 在各平台 SD）

```text
media（媒體檔）
├─ mediaId(PK) / userId / storageUrl / type（IMAGE|VIDEO）
├─ status（UPLOADED|SCANNING|PROCESSING|READY|REJECTED）
└─ createdAt

process_task（後處理任務）
├─ taskId(PK) / mediaId / kind（SCAN|THUMBNAIL|TRANSCODE）/ priority
├─ status（PENDING|RUNNING|DONE|FAILED）/ outputUrl
└─ createdAt
```
