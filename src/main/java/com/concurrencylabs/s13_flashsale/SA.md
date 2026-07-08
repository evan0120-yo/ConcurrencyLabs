# SA.md — s13_flashsale（秒殺限量商品）

> 事實文件（SASD）· 結構化分析。描述「秒殺要做什麼」的**業務行為**，平台無關（P1..Pn 共用；各平台怎麼扛量在各自 SD）。
> 特性：**先讀爆（開賣前刷頁）、再寫爆（開賣搶名額）**；絕不超賣，失敗請求不可拖垮成功的人。

## 1. Context Diagram

```text
[消費者] --(開賣前: 看商品頁/倒數)------> (秒殺) --(庫存近似 / 倒數)--> [消費者]
[消費者] --(開賣: 搶購 userId,itemId,idemKey)--> (秒殺) --(SUCCESS+orderId / SOLD_OUT)--> [消費者]

(秒殺) --(原子預扣庫存 / 去重)--> [util.redis]
(秒殺) --(秒殺訂單)------------> [PostgreSQL]
```

## 2. DFD（精簡 box flow）

```text
【讀階段（開賣前）】[看頁] --> 商品頁重度快取 + 庫存近似顯示

【寫階段（開賣瞬間）】
[搶購] --(idemKey)--> [去重] --(SET NX)--> [util.redis]
   │(已存在)──> 回上次結果
   │(首次)
   ▼
[原子預扣庫存] --(Lua 判餘量>0 則扣1)--> [util.redis]
   │(售完)──> 回 SOLD_OUT（快速失敗，不打 DB）
   │(扣成功)
   ▼
[建秒殺單] --(status=CREATED)--> [PostgreSQL]
   │(建單失敗)──> [回補庫存]（補償）──> 回錯誤
   ▼
回應 { SUCCESS, orderId }
```

## 3. Process Spec（行為基準，decision table）

```text
[去重]（idemKey）
├─ 首次 → 正常搶購
└─ 已存在 → 回上次結果，不重複扣 / 不重複建單

[原子預扣庫存]
├─ 餘量 > 0 → 原子扣 1，繼續
└─ 餘量 = 0 → 回 SOLD_OUT（絕不超賣；售完的請求快速失敗、不落 DB）

[建單]
├─ 成功 → SUCCESS
└─ 失敗 → 回補先前預扣的庫存（不可「扣了庫存卻沒建單」）

邊界：極端尖峰下，99% 註定失敗的請求必須快速在 Redis 層被擋掉，不得穿透到 DB。
```

## 4. State Transition（一張秒殺單）

```text
（無）──(預扣成功+建單)──> 【CREATED】──(進結帳付款)──> 【PAID】/（逾時未付）──> 【CANCELLED，回補庫存】
```

## 5. Data Dictionary（邏輯欄位；實體 DDL 在各平台 SD）

```text
flashsale_order（秒殺單）
├─ orderId(PK) / userId / itemId / idemKey（唯一）
├─ status（CREATED|PAID|CANCELLED）
└─ createdAt

（庫存餘量活在 util.redis，Lua 原子扣減；商品靜態資料另快取）
```
