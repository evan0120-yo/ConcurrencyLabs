# SA.md — s14_couponclaim（優惠券搶券系統）

> 事實文件（SASD）· 結構化分析。描述「搶券要做什麼」的**業務行為**，平台無關（P1..Pn 共用；各平台怎麼扛量在各自 SD）。
> 特性：極端尖峰下同時守住**總量不超發**與**單人不超領**兩個維度。

## 1. Context Diagram

```text
[用戶] --(搶券: userId, batchId, idemKey)--> (搶券) --(CLAIMED+couponId / SOLD_OUT / ALREADY / NOT_ELIGIBLE)--> [用戶]

(搶券) --(原子扣券量 / 單人防重領)--> [util.redis]
(搶券) --(發出的券)--------------> [PostgreSQL coupon]
```

## 2. DFD（精簡 box flow）

```text
[用戶] --(搶券 idemKey)--> [去重] --(SET NX)--> [util.redis]
   │(已存在)──> 回上次結果
   │(首次)
   ▼
[判資格] --(新客/會員券資格,開搶前預熱)-->
   │(不符)──> 回 NOT_ELIGIBLE
   │(符合)
   ▼
[單人防重領] --(user 已領 set：本 batch 是否領過)--> [util.redis]
   │(已領)──> 回 ALREADY（單人限領）
   │(未領)
   ▼
[原子扣券量] --(餘量>0 則扣1)--> [util.redis]
   │(領完)──> 回 SOLD_OUT（不超發）
   │(扣成功)
   ▼
[發券入帳] --> [PostgreSQL] ──> 回 CLAIMED
```

## 3. Process Spec（行為基準，decision table）

```text
[去重]（idemKey）：首次正常；已存在回上次結果

[資格]
├─ 符合 batch 資格（新客/會員…）→ 繼續
└─ 不符 → NOT_ELIGIBLE

[單人限領]（維度一）
├─ 該 user 本 batch 未領 → 標記已領，繼續
└─ 已領 → ALREADY（不給第二張）

[總量]（維度二）
├─ 券餘量 > 0 → 原子扣 1，發券
└─ 券餘量 = 0 → SOLD_OUT（不超發，超發＝行銷成本失控）

不變量：兩維度必須同時成立——單人 set 標記與券量扣減要一致（發了券就一定占了一個名額且標記該人已領）。
```

## 4. State Transition（一張券）

```text
（無）──(搶到)──> 【CLAIMED】──(使用)──> 【USED】/（到期）──> 【EXPIRED】
```

## 5. Data Dictionary（邏輯欄位；實體 DDL 在各平台 SD）

```text
coupon（發出的券）
├─ couponId(PK) / userId / batchId
├─ status（CLAIMED|USED|EXPIRED）/ idemKey（唯一）
└─ claimedAt

（券餘量、單人已領 set 活在 util.redis；batch 資格規則於開搶前預熱）
```
