# SA.md — s12_providertransfercallback（遊戲商轉入轉出 / 回調結算）

> 事實文件（SASD）· 結構化分析。描述「跨系統轉帳要做什麼」的**業務行為**，平台無關（P1..Pn 共用；各平台怎麼扛量在各自 SD）。
> 特性：**真錢跨系統搬動**，靠不可靠 callback 收斂，冪等與補償是命脈；過程可短暫不一致，結果必須最終一致。

## 1. Context Diagram

```text
[玩家] --(轉入/轉出: userId, providerId, amount, transferNo)--> (轉帳) --(受理 / 結果)--> [玩家]

(轉帳) --(扣加平台錢包)------> [common.account]
(轉帳) --(呼叫入帳 / 收 callback)--> [外部遊戲商]
(轉帳) --(轉帳單狀態)--------> [PostgreSQL transfer]
```

## 2. DFD（精簡 box flow）

```text
[玩家] --(轉帳請求 transferNo)--> [去重] --(transferNo 唯一)--> [PostgreSQL transfer]
   │(已存在)──> 回上次結果
   │(首次)
   ▼
[扣款側先動]（轉出：扣平台錢包；轉入：扣遊戲商側，平台待入帳）
   ▼
[呼叫外部入帳] --(providerId, amount)--> [外部遊戲商]
   ▼
[等 callback]（不可靠：可能遲到 / 重送 / 遺失）
   ├─(成功 callback，冪等)──> 確認入帳、轉帳單 SUCCESS、兩邊對平
   ├─(重送 callback)──> 冪等：同 transferNo 只處理一次
   └─(逾時無 callback)──> [主動補償]：查詢對方最終狀態 → 補入帳 或 退款回滾
```

## 3. Process Spec（行為基準，decision table）

```text
[去重]（transferNo）
├─ 首次 → 正常受理
└─ 已存在 → 回上次結果，不重複扣 / 加

[callback 冪等]（callbackId / transferNo）
├─ 首次 → 依結果收斂轉帳單
└─ 重送 → 忽略，不重複加錢

[補償對帳]（callback 逾時 / 遺失）
└─ 主動向對方查詢最終狀態：
   ├─ 對方已入帳 → 平台補記 SUCCESS
   └─ 對方未入帳 → 回滾扣款、轉帳單 FAILED（真錢退回）

[最終一致]（不變量）
└─ 收斂後：平台側金額變動 = 遊戲商側金額變動；絕不重複加、不憑空少
```

## 4. State Transition（一張轉帳單）

```text
【INIT】──(扣款+呼叫外部)──> 【PENDING】（等 callback）
                                ├─(成功 callback / 補償查到已入帳)──> 【SUCCESS】
                                └─(逾時且對方未入帳)──> 【FAILED】（退款回滾）
```

## 5. Data Dictionary（邏輯欄位；實體 DDL 在各平台 SD）

```text
transfer（轉帳單）
├─ transferId(PK) / userId / providerId
├─ direction（IN|OUT）/ amount
├─ status（INIT|PENDING|SUCCESS|FAILED）
├─ transferNo（唯一，去重）/ callbackId（冪等）
└─ createdAt / settledAt

（餘額增減走 common.account，以 transferId 為冪等單號）
```
