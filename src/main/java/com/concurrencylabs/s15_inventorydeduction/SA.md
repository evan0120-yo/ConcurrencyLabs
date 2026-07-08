# SA.md — s15_inventorydeduction（庫存扣減中心）

> 事實文件（SASD）· 結構化分析。描述「庫存扣減要做什麼」的**業務行為**，平台無關（P1..Pn 共用；各平台怎麼扛量在各自 SD）。
> 特性：多來源併發扣同一商品，**不超賣、不扣負**，且「扣了但單沒成」要能可靠**回補**。是被秒殺 / 下單共用的底層元件。

## 1. Context Diagram

```text
[下單來源: s13/s16/…] --(扣: itemId, qty, orderNo)--> (庫存中心) --(OK / INSUFFICIENT)--> [來源]
[下單來源]           --(回補: itemId, qty, orderNo)--> (庫存中心) --(REFUNDED)---------> [來源]

(庫存中心) --(原子扣減 / 回補)--> [util.redis]
(庫存中心) --(庫存 / 扣減流水)--> [PostgreSQL]
```

## 2. DFD（精簡 box flow）

```text
【扣減】
[扣減請求] --(orderNo)--> [去重] --(同 orderNo 已扣?)-->
   │(已扣)──> 回上次結果（冪等）
   │(首次)
   ▼
[原子條件扣減] --(可用 >= qty 則扣)--> [util.redis]
   ├─(不夠)──> 回 INSUFFICIENT（不扣負、不超賣）
   └─(夠) ──> 記扣減流水 ──> 回 OK

【回補】
[回補請求] --(orderNo)--> [依 orderNo 找該筆扣減]
   ├─(已回補)──> 冪等，忽略
   └─(未回補)──> 原子加回 qty，標記已回補 ──> 回 REFUNDED
```

## 3. Process Spec（行為基準，decision table）

```text
[扣減冪等]（orderNo）
├─ 首次 → 執行扣減
└─ 已存在 → 回上次結果（不重複扣）

[扣減判定]
├─ 可用庫存 >= qty → 原子扣，記流水
└─ 可用庫存 <  qty → INSUFFICIENT（絕不扣成負數 / 超賣）

[回補冪等]（orderNo）
├─ 該筆已扣且未回補 → 加回 qty，標記已回補
└─ 已回補 / 無此扣減 → 忽略（同一單只回補一次）

不變量：任一時刻 可用庫存 = 初始 − Σ有效扣減 + Σ回補；不可為負。
```

## 4. State Transition（一筆扣減）

```text
（無）──(扣減)──> 【DEDUCTED】──(下單失敗/取消)──> 【REFUNDED】
                          └──(下單成功)──> 【CONFIRMED】（不再回補）
```

## 5. Data Dictionary（邏輯欄位；實體 DDL 在各平台 SD）

```text
stock（庫存，當下結果）
├─ itemId(PK) / available
└─ updatedAt

stock_op（扣減 / 回補流水）
├─ opId(PK) / itemId / qty / orderNo（去重 / 回補配對）
├─ type（DEDUCT|REFUND）/ status（DEDUCTED|REFUNDED|CONFIRMED）
└─ createdAt
```
