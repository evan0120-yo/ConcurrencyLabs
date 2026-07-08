# SA.md — s17_productdetailtraffic（商品詳情頁爆流量）

> 事實文件（SASD）· 結構化分析。描述「商品詳情頁要做什麼」的**業務行為**，平台無關（P1..Pn 共用；各平台怎麼扛量在各自 SD）。
> 特性：**幾乎純讀爆**。頁面分兩種資料——靜態（圖文/價格，很少變，可重度快取）與動態（庫存，一直變，顯示可近似）。

## 1. Context Diagram

```text
[使用者] --(看商品頁: itemId)--> (商品詳情) --(圖文+價格 + 庫存近似)--> [使用者]

(商品詳情) --(靜態內容快取)----> [util.redis / 多層 cache]
(商品詳情) --(庫存近似值)------> [util.redis 計數]
(商品詳情) --(源資料)---------> [PostgreSQL product]
```

## 2. DFD（精簡 box flow）

```text
[看頁請求] --(itemId)--> [拆兩路組頁]
   ├─【靜態：圖文 / 價格】
   │    └─ 走快取（cache 命中直接回；未命中回源填 cache）─ 變動少、TTL 較長
   └─【動態：庫存數字】
        └─ 走近似計數（「剩不多 / 售罄」等分級），可稍舊
   ▼
組合回應（靜態 + 動態近似）
```

## 3. Process Spec（行為基準，decision table）

```text
[靜態內容]
├─ cache 命中 → 直接回
├─ cache 未命中 → 回源 PostgreSQL 填 cache（防擊穿：單飛 / 短鎖）
└─ 變動（改價/改圖）→ 主動失效 / 更新 cache

[庫存顯示]
├─ 允許近似：以分級（充足 / 剩不多 / 售罄）或稍舊數字顯示
└─ 精確扣減不在此情境（在 s15）；此處只做「看」

不變量：靜態與動態分開處理——庫存高頻變動不得拖累整頁快取效益。
邊界：熱點 key（爆款）需防快取擊穿 / 雪崩（單飛回源、TTL 抖動、必要時 local cache）。
```

## 4. State Transition

```text
（本情境為讀取型，無業務狀態機；只有 cache 的 命中 / 未命中 / 失效 生命週期）
```

## 5. Data Dictionary（邏輯欄位；實體 DDL 在各平台 SD）

```text
product（商品源資料）
├─ itemId(PK) / title / images / desc / price
└─ updatedAt

（庫存近似值活在 util.redis；快取層存渲染好的靜態片段 / 商品 DTO）
```
