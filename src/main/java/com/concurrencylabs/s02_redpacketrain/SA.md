# SA.md — s02_redpacketrain（紅包雨系統）@ L1

> 事實文件（SASD）· 結構化分析。當前 branch = L1（~3,000 QPS）。

## 1. Context Diagram

```text
[使用者] --(搶紅包: rainId, userId, requestId)--> (紅包雨系統) --(CLAIMED+amount / EMPTY / REJECTED)--> [使用者]

(紅包雨系統) --(活動設定 / 領取流水)---> [PostgreSQL]
(紅包雨系統) --(去重 claim / 防重領 / 紅包池原子扣減)--> [util.redis]
(紅包雨系統) --(金額落袋 / 失敗補償)--> [common.account]
```

## 2. DFD（精簡 box flow）

```text
[使用者] --(高頻點擊請求)--> [驗紅包雨活動] --(RUNNING? 在時間窗?)-->
   │(不合法)──> 拒絕（未開始 / 已結束 / 活動不存在）
   │(合法)
   ▼
[請求去重] --(requestId SET NX / replay)--> [util.redis]
   │(同 requestId 已完成)──> 回上次結果（不重複扣池 / 不重複落袋）
   │(同 requestId 處理中)──> 回處理中
   │(首次)
   ▼
[防重領] --(rainId:userId claim set)--> [util.redis]
   │(已成功領過)──> 回已領過（不再扣紅包池）
   │(尚未成功領過)
   ▼
[搶紅包池] --(Lua 原子判空 + 扣包數/金額)--> [util.redis]
   │(池空)──> EMPTY（空手，不落袋）
   │(系統失敗)──> 釋放 request claim，回錯誤
   │(扣成功)
   ▼
[金額落袋] --(加到使用者帳戶)--> [common.account]
   │(失敗)──> [回補紅包池 + 釋放成功領取標記] ──> 回錯誤md
   │(成功)
   ▼
[記領取流水] --(rainId / userId / amount / requestId)--> [PostgreSQL claim_record]
   │(失敗)──> [扣回落袋金額 + 回補紅包池 + 釋放成功領取標記] ──> 回錯誤
   ▼
[標記完成] --(requestId -> result)--> [util.redis]
   ▼
回應 { result, amount?, claimId? }
```

## 3. Process Spec（行為基準，decision table）

```text
[驗紅包雨活動]
├─ status=RUNNING 且 now ∈ [start, end] → 通過
├─ 活動不存在 → 拒絕（活動不存在）
├─ now < start 或 status=NOT_STARTED → 拒絕（活動未開始）
└─ now > end 或 status=ENDED → 拒絕（活動已結束）

[請求去重]（requestId）
├─ 首次 → 正常執行後續流程
├─ 已完成 → 回放同一次點擊的上次結果
└─ 處理中 → 回處理中，不重複打紅包池

[防重領]（rainId + userId）
├─ 尚未成功領過 → 允許嘗試搶紅包池
└─ 已成功領過 → 回已領過，不重複扣紅包池 / 不重複落袋

[搶紅包池]
├─ 剩餘包數 > 0 且剩餘金額 > 0 → 原子扣減 → CLAIMED(amount)
├─ 剩餘包數 = 0 或剩餘金額 = 0 → EMPTY（空手，非錯誤）
└─ Redis / Lua 執行失敗 → 系統錯誤，不能假裝 EMPTY

[落袋與流水]
├─ CLAIMED 後，金額成功落袋且流水成功寫入 → 成功回應
├─ CLAIMED 後，落袋失敗 → 回補紅包池，釋放成功領取標記，回錯誤
└─ CLAIMED 後，流水失敗 → 扣回落袋金額，回補紅包池，釋放成功領取標記，回錯誤

全局邊界（不變量）
├─ 紅包總額【不可超發】：靠 Redis 原子判空+扣減
├─ 單一使用者【不可重領】：同一 rainId + userId 最多成功 CLAIMED 一次
├─ 同一次點擊【不可重放成多次】：靠 requestId 去重
├─ 池空是正常結果：回 EMPTY，不回補、不落袋、不寫成功領取流水
├─ 系統失敗不是池空：不得把 Redis / DB / account failure 包裝成 EMPTY
└─ 多台 server：所有原子性在共享 Redis / DB，不靠本地鎖（DEVELOPMENT §五）
```

## 4. State Transition

```text
紅包雨活動（時間驅動）
NOT_STARTED ──(到 start)──> RUNNING ──(到 end 或營運關閉)──> ENDED

紅包池
AVAILABLE ──(最後一包/最後金額被扣完)──> EMPTY

使用者在單場紅包雨的領取狀態
NONE ──(CLAIMED 成功落袋與記流水)──> CLAIMED

單次點擊 requestId
NEW ──(claim 成功)──> PROCESSING ──(完成)──> DONE
                         │
                         └─(系統失敗且已補償)──> RELEASED
```

## 5. Data Dictionary（邏輯）

```text
搶紅包請求     rainId, userId, requestId
搶紅包結果     result(CLAIMED/EMPTY/ALREADY_CLAIMED/PROCESSING/REJECTED), amount?, claimId?
紅包雨活動     rainId, name, startAt, endAt, status, totalBudget, totalPackets
紅包池         rainId, remainingBudget, remainingPackets（執行期在 Redis）
使用者領取狀態 rainId, userId, claimedAt?, amount?（防重領依據）
領取流水       claimId, rainId, userId, amount, claimedAt, requestId

（實體 DDL 在 SD §4；落袋帳戶屬 common.account；Redis key 設計在 SD §4 / §8）
```
