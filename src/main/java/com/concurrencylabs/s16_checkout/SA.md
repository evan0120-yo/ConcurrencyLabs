# SA.md — s16_checkout（Checkout 下單鏈路）

> 事實文件（SASD）· 結構化分析。描述「結帳鏈路要做什麼」的**業務行為**，平台無關（P1..Pn 共用；各平台怎麼扛量在各自 SD）。
> 特性：**編排者**，串多系統成一條龍。核心（算價/扣庫存/建單/付款）不能錯，周邊（積分/推薦/通知）可降級；任一步錯要回滾或補償，不留半殘訂單。

## 1. Context Diagram

```text
[使用者] --(結帳: userId, cart, idemKey)--> (Checkout 編排) --(ORDER_OK+orderId / FAIL)--> [使用者]

(Checkout) --(算價)--------> [s19 促銷價格]
(Checkout) --(扣庫存/回補)--> [s15 庫存中心]
(Checkout) --(付款)--------> [s06 錢包 / 支付]
(Checkout) --(建單)--------> [PostgreSQL order]
(Checkout) --(周邊: 積分/推薦/通知)--> [MQ]（非同步、可降級）
```

## 2. DFD（精簡 box flow）

```text
[結帳] --(idemKey)--> [去重] --(SET NX)--> 已存在回上次結果
   │(首次)
   ▼
[算價] --(s19)--> 得應付金額
   ▼
[扣庫存] --(s15, orderNo=idemKey)-->
   │(不足)──> 回 FAIL（未扣任何錢）
   │(成功)
   ▼
[建單 status=CREATED] --> [付款]
   │(付款失敗)──> [回補庫存] + 取消單（補償）──> 回 FAIL
   │(付款成功)──> 訂單 PAID
   ▼
[核心完成，回 ORDER_OK] ──> 發周邊事件到 [MQ]（積分/推薦/通知，非同步；失敗只補償不擋成單）
```

## 3. Process Spec（行為基準，decision table）

```text
[去重]（idemKey）：首次執行；已存在回上次結果（同一結帳不重複下單）

[核心步驟]（算價 → 扣庫存 → 建單 → 付款）必須全成：
├─ 全成 → 訂單 PAID，回 ORDER_OK
└─ 任一失敗 → 依 saga 補償回滾：解扣庫存、退款（若已扣）、訂單 CANCELLED；不留半殘單

[周邊步驟]（積分/推薦/通知）
└─ 一律非同步、可失敗降級；失敗走補償重試，絕不阻擋 / 回滾核心成單

不變量：錢、庫存、訂單三者最終對得起來（成單 ⇒ 已扣庫存且已收款）。
```

## 4. State Transition（一張訂單）

```text
（無）──(建單)──> 【CREATED】──(付款成功)──> 【PAID】
                        └──(付款失敗/逾時)──> 【CANCELLED】（回補庫存 + 退款）
```

## 5. Data Dictionary（邏輯欄位；實體 DDL 在各平台 SD）

```text
order（訂單）
├─ orderId(PK) / userId / items[] / amount
├─ status（CREATED|PAID|CANCELLED）/ idemKey（唯一）
└─ createdAt / paidAt

（庫存走 s15、金流走 s06/支付、周邊事件走 MQ）
```
