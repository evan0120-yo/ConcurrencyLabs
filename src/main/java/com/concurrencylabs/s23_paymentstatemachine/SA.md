# SA.md — s23_paymentstatemachine（交易狀態機系統）

> 事實文件（SASD）· 結構化分析。描述「交易狀態機要做什麼」的**業務行為**，平台無關（P1..Pn 共用；各平台怎麼扛量在各自 SD）。
> 本體是**狀態機**：合法轉移只有幾條，其餘一律擋；用它消化「不可靠、會亂序重複」的外部訊息，全程不錯帳。

## 1. Context Diagram

```text
[商戶 / 用戶] --(建交易 / 查狀態: txnId)--> (交易狀態機) --(當前狀態)--> [商戶 / 用戶]

(交易狀態機) <--(callback / 查單結果 / 逾時)--> [第三方支付]
(交易狀態機) --(每個轉移對應記帳)-----------> [s25 ledger / common.account]
(交易狀態機) --(交易 / 狀態事件)------------> [PostgreSQL transaction]
```

## 2. DFD（精簡 box flow）

```text
[建交易] --(idemKey)--> 【CREATED】 --(發起付款)--> 【PENDING】 --> 【PAYING】
   ▼（收斂訊號，可能亂序 / 重複 / 遺失）
[消化訊號]
   ├─ callback 到達 → 依結果嘗試轉移
   ├─ 主動查單 → 我去問第三方現在到底怎樣
   └─ 逾時補償 → 太久沒結果走兜底（查單 / 標失敗）
   ▼
[狀態機守門]（只允許合法轉移）
   ├─ 合法 → 轉移 + 對應記帳（成功→入帳 / 退款→退回）
   └─ 非法 / 重複 → 忽略（不被帶錯）
```

## 3. Process Spec（行為基準，decision table）

```text
[合法轉移]（其餘一律拒）
├─ CREATED → PENDING → PAYING
├─ PAYING → SUCCESS（入帳）/ FAILED
└─ SUCCESS → REFUNDED（退款）

[冪等 / 亂序]（txnId + eventSeq）
├─ 重複訊號 → 忽略（同結果只處理一次）
├─ 過期訊號（狀態已更前）→ 忽略（不倒退）
└─ 亂序 → 以狀態機當前態判定是否可套用

[收斂三力]
├─ callback（不可靠：遲到 / 重送 / 遺失）
├─ 主動查單（callback 沒來就去問）
└─ 逾時補償（都沒結果 → 走兜底）

不變量：每個資金相關轉移必對應且只對應一次記帳（不錯帳、不重複入帳）。
```

## 4. State Transition

```text
【CREATED】──> 【PENDING】──> 【PAYING】──(成功)──> 【SUCCESS】──(退款)──> 【REFUNDED】
                                    └──(失敗/逾時)──> 【FAILED】
```

## 5. Data Dictionary（邏輯欄位；實體 DDL 在各平台 SD）

```text
transaction（交易）
├─ txnId(PK) / userId / amount / status（CREATED|PENDING|PAYING|SUCCESS|FAILED|REFUNDED）
├─ idemKey（唯一）/ channelRef
└─ createdAt / updatedAt

state_event（狀態訊號，去重 / 稽核）
├─ eventId(PK) / txnId / type（CALLBACK|QUERY|TIMEOUT）/ seq
└─ payload / receivedAt
```
