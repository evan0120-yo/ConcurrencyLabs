# SD.md — s01_lottery（抽獎活動系統）@ L1

> 事實文件（SASD）· 結構化設計。正式交付級 self-contained。當前 branch = L1（~3,000 QPS）。

## 0. 背景與範圍

```text
目的
└─ 活動抽獎：使用者消耗 200 積分抽一次，從有限獎品池公平發獎，尖峰不超發、不透支

產品規則
├─ 抽一次 = 消耗 200 積分（積分成本即天然節流，不另做限次）
├─ WIN → 發對應獎品；LOSE → 銘謝惠遠（積分照扣）
└─ 中獎機率 = 獎品權重設定

使用對象（預設人群）
└─ C 端活動參與者（一般使用者；身分/積分來自 common.account，無 auth，直接傳 userId）

規模與未來估計（當前 L1）
├─ 尖峰 ~3,000 QPS，活動開放瞬間集中
├─ 獎品有限（例：iPhone×5、折價券×1,000，其餘銘謝惠遠）
└─ 非目標（L1 不做，留 L2~L4）：MQ 削峰、預生成中獎序列(token)、活動獨立叢集
```

## 1. 架構總覽 + Structure Chart

```text
POST /draw
   │
   ▼
┌─────────────────────┐
│ LotteryController   │  HTTP 入口，*Req→*Resp
└─────────┬───────────┘
          ▼
┌─────────────────────┐
│ DrawLotteryUsecase  │  編排：驗活動→去重→扣積分→抽獎→(失敗退款)→記錄
└───┬───────┬─────┬───┘
    ▼       ▼     ▼
┌────────┐ ┌──────────┐ ┌────────────┐
│Activity│ │ Lottery  │ │ DrawRecord │
│Service │ │DrawService│ │ Service    │
└───┬────┘ └────┬─────┘ └─────┬──────┘
    ▼           ▼             ▼
[ DB活動/獎品] [util.redis]  [ DB draw_record ]
                +
        [common.account] ← 扣/退積分（Usecase 呼叫）

Structure Chart（呼叫階層）
DrawLotteryUsecase
├─ LotteryIdempotencyService   去重 claim / markDone / release（SET NX）→ util.redis
├─ LotteryGuardService         前置守門：checkActivity（驗活動）、chargePoints（扣款不足即擋）
│   ├─ LotteryActivityService.findActivity（純讀）
│   └─ common.account.AccountService.deductPoints
├─ LotteryActivityService      純讀：findActivity / listPrizes / findPrize
├─ common.account.AccountService   refundPoints（補償退款）
├─ LotteryDrawService          權重挑桶 + Lua 原子扣獎品 → util.redis.RedisUtil
└─ DrawRecordService           寫一筆抽獎流水 → DrawRecordRepository (JPA)
```

## 2. Module Decomposition

```text
LotteryController         僅路由 + dto 進 dto 出
DrawLotteryUsecase        流程編排（一註解=一行 service）：去重→驗活動→扣積分→抽獎+記錄→退款補償→標記完成
LotteryGuardService       前置守門：checkActivity（活動狀態/時間窗）、chargePoints（扣款不足即 throw）
LotteryIdempotencyService 去重 claim / markDone / release（Redis SET NX）
LotteryActivityService    純讀：活動 / 獎品設定（findActivity / listPrizes / findPrize）
LotteryDrawService        權重挑桶 + Redis Lua 原子扣獎品，判 WIN/LOSE
DrawRecordService         寫抽獎流水（含未中獎）
DrawRecordRepository      PostgreSQL 抽獎流水 CRUD
（積分：common.account；Redis：util.redis）
```

## 3. Coupling & Cohesion + 依賴方向

```text
allowed
├─ Controller → Usecase → Service → Repository(僅本情境 DB)
├─ Service → Service（如 Guard 呼叫 Activity 純讀 / Account facade）
├─ Usecase/Service → common.account（借積分能力）
└─ Service → util.redis（借 Redis 原子能力）

must-not
├─ Controller ─X→ Service / Repository（越層）
├─ Usecase ─X→ Repository（必須經 Service）
└─ 本情境 Repository ─X→ 封裝 Redis / MQ（那是 util）
```

## 4. 資料模型 / Table Schema

```text
lottery_activity   活動主檔
├─ id (PK)  name  start_at  end_at  status(NOT_STARTED/RUNNING/ENDED)  created_at

lottery_prize      獎品設定（master；即時餘量在 Redis）
├─ id (PK)  activity_id (FK→lottery_activity)  name  level
├─ total_qty  weight(中獎權重)  is_thanks(是否銘謝惠遠桶)
└─ index (activity_id)

lottery_draw_record  抽獎流水（每抽一筆，含未中獎）
├─ id (PK)  activity_id  user_id  prize_id (nullable, null=銘謝惠遠)
├─ is_win  points_cost(=200)  draw_at  idempotency_key
└─ unique(idempotency_key)   index(activity_id, user_id)

Redis key（util.redis 操作，非本情境 DB）
├─ lottery:stock:{activityId}:{prizeId} → 獎品餘量（活動開始由 total_qty 載入）
└─ lottery:idem:{idempotencyKey}        → 去重 claim（SET NX EX；可存上次結果）

（積分餘額 users.points 屬 common.account，非本表；user_id 為邏輯引用，不設跨模組實體 FK）
```

## 5. API 設計 / Schema

```text
POST /api/lottery/{activityId}/draw
Req   { userId, idempotencyKey }        // 無 auth，userId 直接傳入（labs 簡化）
Resp  200 { result: "WIN"|"LOSE", drawId, prize?: { id, name, level } }

錯誤碼
├─ 404 40401  活動不存在
├─ 409 40901  活動未開始 / 40902 活動已結束
├─ 402 40201  積分不足（< 200）
├─ 200 + LOSE 獎品發完 → 回銘謝惠遠（非錯誤）
└─ 去重命中 → 回上次結果（同 idempotencyKey 冪等，不重複扣/抽）
```

## 6. 關鍵流程 Sequence

```text
使用者點抽獎
 → Controller /draw
 → Usecase:
    1) Idempotency claim（SET lottery:idem NX）           DONE→回放上次結果 / PENDING→PROCESSING
    2) Guard.checkActivity（RUNNING? 時間窗?）             失敗→釋放 claim、拒
    3) Guard.chargePoints → account.deductPoints(200)     不足→釋放 claim、拒（不碰獎品）
    4) 抽獎 + 記錄（任一步系統失敗 → account.refundPoints 補償、釋放 claim、回錯誤）
         ├─ DrawService 權重挑桶 → RedisUtil.eval(扣減Lua) → WIN/LOSE
         └─ DrawRecordService 寫 draw_record（WIN/LOSE 都記）
    5) Idempotency markDone → 回 { result, prize? }
```

## 7. 非功能需求（NFR）

```text
├─ 一致性（硬紅線）
│   ├─ 獎品不可超發 → Redis Lua 原子判空+扣減
│   ├─ 積分不可透支 → DB 原子條件更新（common.account）
│   └─ 一次抽獎只發生一次 → idempotencyKey（SET NX）
├─ 跨儲存一致（DB積分 ↔ Redis獎品，無 ACID）
│   └─ 先扣積分→再抽獎品；抽獎失敗補償退積分
├─ 多台 server（DEVELOPMENT §五）
│   └─ 無狀態；所有原子性在共享 Redis / DB，不用本地鎖 / synchronized
├─ 延遲：抽獎 P99 低（去重1 + 扣積分1 + Lua1 + insert1，皆單步）
└─ 公平/防刷：中獎率=權重；積分成本天然節流；去重防重複扣
```

## 8. 相依與外部整合

```text
├─ util.redis     獎品餘量原子扣減、去重 claim
├─ common.account 積分扣減 / 退回
└─ PostgreSQL     活動/獎品設定、抽獎流水
（L1 無 MQ；DB 寫入同步）
```
