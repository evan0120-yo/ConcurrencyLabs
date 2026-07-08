# SA.md — s19_promotionpricing（促銷價格計算）

> 事實文件（SASD）· 結構化分析。描述「算價要做什麼」的**業務行為**，平台無關（P1..Pn 共用；各平台怎麼扛量在各自 SD）。
> 特性：價格是**一串規則疊出來的結果**，讀爆（熱門商品反覆算）。熱門可快照、活動期要凍結一致（不能一刷一個價）。

## 1. Context Diagram

```text
[看價 / 結帳] --(算價: itemId, userId, context)--> (算價) --(最終價 + 明細)--> [呼叫方]

(算價) --(規則: 滿減/券/會員價/限時折)--> [PostgreSQL price_rule]
(算價) --(熱門商品價格快照)-----------> [util.redis]
```

## 2. DFD（精簡 box flow）

```text
[算價請求] --(itemId, userId, context)--> [查快照]
   │(命中且未過期)──> 直接回快照價（省重算）
   │(未命中)
   ▼
[疊規則計算]（依序：原價 → 滿減 → 券 → 會員價 → 限時折）
   ▼
[寫快照]（熱門商品：相同輸入 → 快取結果）
   ▼
回應 { finalPrice, 折扣明細 }
```

## 3. Process Spec（行為基準，decision table）

```text
[規則疊加]
└─ 依既定順序套用；每條規則明確可疊 / 互斥；最終價不可為負

[熱門快照]
├─ 相同輸入（itemId + 定價 context）→ 快取相同結果，命中直接回
└─ 規則變更 / 到期 → 主動失效快照

[活動期凍結]（一致性）
└─ 活動期間同一商品對同一情境的價格必須穩定：以凍結快照供價，避免規則刷新造成抖動

不變量：結帳當下取用的價格必須是「有效且一致」的（不能一刷一個價、不能用過期規則）。
```

## 4. State Transition（一份價格快照）

```text
（無）──(算出)──> 【ACTIVE】──(規則變/到期)──> 【STALE】──> 重算
       活動期：ACTIVE 被凍結至活動結束（FROZEN）
```

## 5. Data Dictionary（邏輯欄位；實體 DDL 在各平台 SD）

```text
price_rule（定價規則）
├─ ruleId(PK) / type（FULL_CUT|COUPON|MEMBER|FLASH）/ params
└─ priority / effectiveFrom / effectiveTo

price_snapshot（價格快照）
├─ itemId / contextKey（複合）/ finalPrice / breakdown
└─ status（ACTIVE|STALE|FROZEN）/ computedAt / expireAt
```
