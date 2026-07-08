# Object Storage Metadata 系統 · s70_objectstoragemetadata

> 一句話定位：管理海量檔案的 metadata（誰的、多大、在哪），list 操作要限流、大 bucket 要拆分。

---

## Block 1：這個情境同時符合哪些分類

- 業務 domain：Cloud / 平台治理
- 壓力模型：讀爆型（metadata 查詢）、多租戶型（bucket / tenant 分片）

---

## Block 2：為什麼被列上這些

- **讀爆型** → 檔案 metadata 被高頻查詢（開檔前先查）。
- **多租戶型** → 按 bucket / tenant 分片，避免單一大 bucket 拖垮。

---

## Block 3：User Story + 痛點分析（不涉及技術）

### User Story

```text
海量檔案，每個檔有一筆 metadata（大小 / owner / 路徑 / 權限）
   讀檔前 ──> 先查 metadata
   ▼
[Metadata 系統]
   ├─ 一般查詢（查單一檔）→ 高頻但輕
   └─ list 操作（列出某 bucket 全部檔）→ 大 bucket 有上億筆 → 極重

────────── 有人對一個上億檔的 bucket 下 list ──────────

平台想要：list 這種重操作要限流，別讓一個大 bucket 拖垮 metadata 服務
```

### 分段解說

- Metadata 本身不大，但「筆數」極多。
- 單檔查詢輕，但「list 整個大 bucket」是重操作。
- 大 bucket 的 index 要拆分，list 要限流。

### 痛點分析

- **讀爆還寫爆？** 讀爆為主。
- **隱藏地雷** → list 大 bucket 是重操作。
- **要不要分片？** 大 bucket index 要拆分。
- **真正的痛**：單檔查詢很輕，但「list 上億筆的大 bucket」這種重操作若不限流 / 分片，會把整個 metadata 服務拖垮。

---

## 容量平台演進地圖（P1 → P3）

> 起點固定假設 QPS 3,000+。每跳一個平台 = 一次架構改造（一組必須一起改的大方塊）。
> s70 讀爆、list 大 bucket 重操作要限流分片,放寬 → 共 3 個平台。

```text
【P1 · 基線】約 3,000 QPS
├─ 定義：單檔 metadata 查詢 + list
├─ 大方塊：metadata 查詢 · list
└─ 撐不住的訊號 → list 上億筆大 bucket 重操作拖垮服務

【P2 · list 限流分頁 + 大 bucket index 分片】約 10,000 QPS
├─ 定義：單檔查快取,list 一律分頁+限流,大 bucket index 分片
├─ 大方塊（綁一起）
│   ├─ 單檔查詢快取(輕高頻)
│   ├─ list 分頁(游標)+限流(防拖垮)
│   └─ 大 bucket index 分片
├─ 為何綁：list 重操作不拖垮 = 分頁 + 限流 + index 分片,一起做
└─ 撐不住的訊號 → 檔數再放大、按 bucket/tenant 熱點

【P3 · metadata 分片 + 多層快取 + 租戶隔離】約 50,000+ QPS
├─ 定義：metadata 按 bucket/tenant 分片,查詢多層快取,租戶隔離
├─ 大方塊：metadata 分片 · 多層快取 · 租戶隔離
└─ 為何綁：規模化 metadata = 分片 + 快取 + 租戶隔離,一起做
```
