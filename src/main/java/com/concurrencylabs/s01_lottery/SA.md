# SA.md — s01_lottery（抽獎活動系統）@ L1

> 事實文件（SASD）· 結構化分析。當前 branch = L1（~3,000 QPS）。

## 1. Context Diagram

```text
[使用者] --(抽獎: activityId, userId, idemKey)--> (抽獎系統) --(WIN+獎品 / LOSE)--> [使用者]

(抽獎系統) --(去重 claim / 獎品原子扣減)--> [util.redis]
(抽獎系統) --(扣 200 積分 / 失敗退回)-----> [common.account]
(抽獎系統) --(活動·獎品設定 / 抽獎流水)---> [PostgreSQL]
```

## 2. DFD（精簡 box flow）

```text
[使用者] --(抽獎請求)--> [驗活動] --(RUNNING? 在時間窗?)-->
   │(不合法)──> 拒絕（未開始 / 已結束）
   │(合法)
   ▼
[去重 claim] --(SET idem:{key} NX)--> [util.redis]
   │(已存在)──> 回上次結果（不重複扣/抽）
   │(首次)
   ▼
[扣 200 積分] --(原子條件更新)--> [common.account]
   │(積分不足)──> 拒絕（積分不足），不碰獎品
   │(扣成功)
   ▼
[抽獎品] --(權重挑桶 + Lua 判空扣減)--> [util.redis]
   │(系統失敗)──> [退回 200 積分] ──> 回錯誤
   ├─(餘量>0)──> WIN(prizeId)
   └─(餘量=0)──> LOSE（銘謝惠遠；積分照扣）
   ▼
[記抽獎流水] --(WIN 記 prizeId / LOSE 記 null)--> [PostgreSQL draw_record]
   ▼
回應 { result, prize? }
```

## 3. Process Spec（行為基準，decision table）

```text
[驗活動]
├─ status=RUNNING 且 now ∈ [start, end] → 通過
├─ now < start → 拒絕（活動未開始）
└─ status=ENDED 或 now > end → 拒絕（活動已結束）

[去重]（idempotencyKey）
├─ 首次（SET NX 成功） → 正常執行整個抽獎
└─ 已存在            → 回上次結果，不重複扣積分 / 不重複抽

[扣積分]
├─ 餘額 >= 200 → 原子扣 200，繼續
└─ 餘額 <  200 → 拒絕（積分不足），不碰獎品

[抽獎品]（先依權重挑一個獎品桶，再對該桶原子扣減）
├─ 該桶餘量 > 0 → 原子扣 1 → WIN
└─ 該桶餘量 = 0 → LOSE（不回補積分——付費玩一次，願賭服輸）

[補償]
└─ 扣積分成功後、「抽獎品或記流水」步驟系統失敗 → 退回 200 積分（LOSE 不算失敗）

全局邊界（不變量）
├─ 積分【不可透支】：靠 DB 原子條件更新
├─ 獎品【不可超發】：靠 Redis Lua 原子判空+扣減
├─ 一次抽獎【只發生一次】：靠 idempotencyKey（SET NX）
├─ 順序：先扣積分 → 再抽獎品（避免送出稀缺獎品卻沒收到積分）
└─ 多台 server：所有原子性在共享 Redis / DB，不靠本地鎖（DEVELOPMENT §五）
```

## 4. State Transition

```text
活動狀態（時間驅動）
NOT_STARTED ──(到 start)──> RUNNING ──(到 end)──> ENDED

單次抽獎：一個請求內完成，無長狀態。
```

## 5. Data Dictionary（邏輯）

```text
抽獎請求   activityId, userId, idempotencyKey
抽獎結果   result(WIN/LOSE), prizeId?(WIN 才有), drawId
獎品桶     prizeId, name, level, weight(中獎權重), 餘量(執行期在 Redis)
抽獎流水   drawId, activityId, userId, prizeId?, isWin, pointsCost(=200), drawAt, idempotencyKey
（實體 DDL 在 SD §4；積分餘額屬 common.account）
```
