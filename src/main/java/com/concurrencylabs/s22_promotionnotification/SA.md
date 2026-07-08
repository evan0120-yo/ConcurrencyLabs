# SA.md — s22_promotionnotification（大促通知推播）

> 事實文件（SASD）· 結構化分析。描述「行銷群發要做什麼」的**業務行為**，平台無關（P1..Pn 共用；各平台怎麼扛量在各自 SD）。
> 特性：**巨量 fan-out、可延遲可丟可分批**；危險在於行銷洪流不能擠掉少量高優先的交易通知——隔離與配速是關鍵。

## 1. Context Diagram

```text
[活動觸發] --(群發: 受眾, 模板, 渠道)--> (通知) --(分渠道分批發送)--> [用戶: 推播/簡訊/站內信]

(通知) --(任務 / 發送記錄)--> [PostgreSQL notify_task]
(通知) --(非同步分批)------> [MQ]
(通知) --(配速呼叫)--------> [外部供應商: 推播 / 簡訊]
```

## 2. DFD（精簡 box flow）

```text
[活動觸發群發] --(受眾清單, 模板)--> [建通知任務]
   ▼
[fan-out 分批] --(拆成多批收件人)--> [MQ]（依優先級分流）
   ├─【行銷佇列】低優先：可延遲 / 可丟 / 分批
   └─【交易佇列】高優先：訂單/付款通知，優先發、不被行銷淹沒
   ▼
[worker 消費發送] --(依供應商速率上限配速)--> [外部供應商]
   ├─(成功)──> 記已送
   └─(失敗)──> 有限次重試（行銷可最終放棄）
```

## 3. Process Spec（行為基準，decision table）

```text
[優先級隔離]（核心不變量）
├─ 交易通知（付款/訂單）→ 高優先獨立佇列 / 資源，優先送達
└─ 行銷通知 → 低優先，不得佔用 / 拖垮交易通知通道

[行銷語意放寬]
├─ 可延遲：晚幾分鐘可接受
├─ 可分批：拆批平滑洪峰
└─ 可丟：重試上限後放棄不影響業務

[供應商配速]
└─ 依各供應商速率上限令牌桶配速，避免被限流 / 封鎖

[去重]
└─ 同一 taskId + 收件人只發一次（避免重送轟炸）
```

## 4. State Transition（一個通知任務）

```text
【PENDING】──(fan-out 入佇列)──> 【SENDING】──(全批處理完)──> 【DONE】
                                        └─(部分失敗且重試耗盡)──> 【PARTIAL】（行銷可接受）
```

## 5. Data Dictionary（邏輯欄位；實體 DDL 在各平台 SD）

```text
notify_task（通知任務）
├─ taskId(PK) / channel（PUSH|SMS|INBOX）/ priority（TXN|MARKETING）
├─ audienceRef / templateId / status（PENDING|SENDING|DONE|PARTIAL）
└─ createdAt

notify_record（發送記錄，去重 / 對帳）
├─ taskId / userId / status（SENT|FAILED）
└─ sentAt
```
