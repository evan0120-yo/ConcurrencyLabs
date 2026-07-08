# SA.md — s07_leaderboard（活動排行榜系統）

> 事實文件（SASD）· 結構化分析。描述「排行榜要做什麼」的**業務行為**，平台無關（P1..Pn 共用；各平台怎麼扛量在各自 SD）。
> 特性：**讀爆 + 排名可近似**。真正被看的只有 Top N 一小段。

## 1. Context Diagram

```text
[玩家得分事件] --(userId, boardId, deltaScore)--> (排行榜) --(ok)--> 
[玩家看榜] ------(boardId)------------------------> (排行榜) --(Top N / 我的名次)--> [玩家]

(排行榜) --(分數增減、排名查詢)--> [util.redis ZSet]
(排行榜) --(榜設定 / 快照落地)---> [PostgreSQL]
```

## 2. DFD（精簡 box flow）

```text
【寫路徑】
[得分事件] --(deltaScore)--> [更新分數] --(ZINCRBY board:{id})--> [util.redis ZSet]

【讀路徑】
[看榜請求]
   ├─(要 Top N)──> [取榜頂] --(ZREVRANGE 0..N)--> 回 Top N（熱點）
   └─(要我的名次)──> [查排名] --(ZREVRANK member)--> 回名次（可近似）
```

## 3. Process Spec（行為基準，decision table）

```text
[更新分數]
├─ deltaScore 累加到 member（ZINCRBY） → 分數即時反映到排序
└─ 允許近似：批量 / 稍延遲更新對「榜」語意可接受（不涉及錢）

[讀 Top N]
└─ 取分數最高的前 N 名；同分依既定 tie-break（如較早達成者在前）

[讀我的名次]
├─ 存在 → 回目前名次（允許近似 / 稍舊）
└─ 不在榜 → 回「未上榜」

[榜維度]
└─ 同一玩法可有多榜：日榜 / 週榜 / 區服榜，各自獨立 board key 與週期歸零
```

## 4. State Transition（榜週期）

```text
【榜週期】OPEN（累積中）──(到期)──> FROZEN（結算快照）──(歸檔)──> ARCHIVED
                                              └─ 落 PostgreSQL 快照，Redis 榜歸零重開
```

## 5. Data Dictionary（邏輯欄位；實體 DDL 在各平台 SD）

```text
board（榜設定）
├─ boardId(PK) / type（DAILY|WEEKLY|SERVER）/ periodKey（如 2026-07-07）
└─ startTime / endTime / status（OPEN|FROZEN|ARCHIVED）

entry（榜內一名，主要活在 Redis ZSet）
├─ boardId / member(userId) / score
└─（結算時快照落 PostgreSQL：boardId / periodKey / rank / userId / score）
```
