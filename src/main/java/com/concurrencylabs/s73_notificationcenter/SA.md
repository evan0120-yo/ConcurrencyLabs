# SA.md — s73_notificationcenter（大型 Notification Center）

> 事實文件（SASD）· 結構化分析。描述「通知中樞要做什麼」的**業務行為**，平台無關（P1..Pn 共用；各平台怎麼扛量在各自 SD）。
> 全平台通知的匯流排。核心是**優先級 + 渠道配速**：保證少量高優先的交易通知，永遠不被巨量低優先的行銷通知淹沒。

## 1. Context Diagram

```text
[各業務: 付款/提幣/促銷/公告] --(通知: userId, type, priority, channels)--> (通知中樞) --(分渠道送)--> [用戶]

(通知中樞) --(分優先級佇列)--> [MQ]
(通知中樞) --(配速呼叫)------> [渠道供應商: 推播/簡訊/Email/站內信]
```

## 2. DFD（精簡 box flow）

```text
[各業務通知匯入] --> [優先級分道]
   ├─【交易通知】付款/安全 → 高優先佇列（必達、要快）
   └─【行銷通知】促銷/推薦 → 低優先佇列（可延遲、可丟）
   ▼
[分渠道投遞]（App 推播 / 簡訊 / Email / 站內信）
   ▼
[供應商配速 + 重試]（各渠道供應商有速率上限）
   ├─(成功)──> DELIVERED
   └─(失敗)──> 重試（行銷可最終放棄；交易更積極重試）
```

## 3. Process Spec（行為基準，decision table）

```text
[優先級隔離]（核心不變量）
├─ 交易通知（付款/安全）→ 高優先獨立佇列 / 資源，必達、優先送
└─ 行銷通知 → 低優先，不得佔用 / 淹沒交易通知通道

[行銷語意放寬]
└─ 可延遲、可分批、可丟（重試上限後放棄不影響業務）

[渠道配速]
└─ 各渠道供應商速率上限 → 令牌桶配速，避免被限流 / 封鎖

[去重]
└─ 同一通知（bizNo + user + channel）只發一次
```

## 4. State Transition（一則通知）

```text
【PENDING】──(投遞)──> 【SENDING】──(成功)──> 【DELIVERED】
                            └──(失敗·重試耗盡)──> 【FAILED】（交易→告警 / 行銷→放棄）
```

## 5. Data Dictionary（邏輯欄位；實體 DDL 在各平台 SD）

```text
notification（通知）
├─ notifyId(PK) / userId / bizType / priority（TXN|MARKETING）
├─ channels[]（PUSH|SMS|EMAIL|INBOX）/ status（PENDING|SENDING|DELIVERED|FAILED）
├─ dedupKey（bizNo+user+channel 去重）
└─ createdAt
```
