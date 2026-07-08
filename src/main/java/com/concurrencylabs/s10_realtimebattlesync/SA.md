# SA.md — s10_realtimebattlesync（即時對戰狀態同步）

> 事實文件（SASD）· 結構化分析。描述「對戰同步要做什麼」的**業務行為**，平台無關（P1..Pn 共用；各平台怎麼扛量在各自 SD）。
> 特性：**大量長連線 + 極低延遲 + 單房事件順序**；過程可在記憶體跑，只有結果要可靠落地。

## 1. Context Diagram

```text
[房內玩家×N] --(WS 連線: 動作事件)--> (對戰同步) --(即時廣播房內狀態)--> [房內其他玩家]
[觀戰者]     --(WS 連線: 訂閱)-----> (對戰同步) --(降頻推送)---------> [觀戰者]

(對戰同步) --(對局結果落地)--> [PostgreSQL battle_result]
```

## 2. DFD（精簡 box flow）

```text
[玩家動作] --(WS 事件)--> [進房佇列] --(單房 FIFO 定序)-->
   ▼
[套用狀態] --(在房記憶體更新對局狀態)-->
   ▼
[廣播] --(推給房內所有玩家連線)--> [房內玩家]
   └─(觀戰者)──> 降頻推送（比玩家低頻）
   │
   ▼(對局結束)
[結果落地] --(可靠寫一次)--> [PostgreSQL battle_result]
```

## 3. Process Spec（行為基準，decision table）

```text
[事件定序]
└─ 同一房間內事件必須 FIFO 順序套用（不可你先我後亂掉）；跨房間彼此獨立

[廣播範圍]
├─ 玩家 → 房內全量、即時
└─ 觀戰者 → 同狀態、降頻（可捨棄中間幀）

[連線]
├─ 掉線 → 房內標記該玩家離線；重連 → 補當前狀態快照
└─ 全房離開 / 對局結束 → 關房

[結果落地]
└─ 對局結束只落地一次（冪等）；過程狀態不需逐幀持久化
```

## 4. State Transition（一個房間）

```text
【WAITING】──(玩家到齊)──> 【PLAYING】──(對局結束)──> 【ENDED】──> 結果落地、關房
```

## 5. Data Dictionary（邏輯欄位；實體 DDL 在各平台 SD）

```text
room_session（房會話，主要活在記憶體）
├─ roomId(PK) / players[]（含連線/在線狀態）/ seq（事件序號）
└─ status（WAITING|PLAYING|ENDED）

battle_result（對局結果，落地）
├─ roomId(PK) / result（勝負/比分）/ playersSnapshot
└─ endedAt
```
