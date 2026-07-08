# SA.md — s04_bettingentry（下注入口系統）

> 事實文件（SASD）· 結構化分析。描述「下注入口要做什麼」的**業務行為**，平台無關（P1..Pn 共用這一份；各平台怎麼扛流量在各自 SD）。

## 1. Context Diagram

```text
[玩家] --(下注: tableId, roundId, userId, amount, idemKey)--> (下注入口) --(ACCEPTED+betId / REJECTED+原因)--> [玩家]

(下注入口) --(查可用餘額 / 凍結 / 解凍)--> [common.account]
(下注入口) --(原子凍結、idem 去重)-------> [util.redis]
(下注入口) --(下注流水、局狀態)----------> [PostgreSQL]
```

## 2. DFD（精簡 box flow）

```text
[玩家] --(下注請求)--> [驗局狀態] --(OPEN? 未封盤?)-->
   │(封盤 / 無此局)──> 拒絕（已封盤 / 無此局）
   │(合法)
   ▼
[去重] --(SET idem:{key} NX)--> [util.redis]
   │(已存在)──> 回上次結果（不重複凍結 / 不重複建注）
   │(首次)
   ▼
[凍結金額] --(原子：可用餘額 >= amount 則凍結)--> [common.account]
   │(餘額不足)──> 拒絕（餘額不足），不建注
   │(凍結成功)
   ▼
[受理下注] --(建注 status=ACCEPTED)--> [PostgreSQL bet]
   │(落注失敗)──> [解凍金額]（補償）──> 回錯誤
   ▼
回應 { ACCEPTED, betId }
```

## 3. Process Spec（行為基準，decision table）

```text
[驗局狀態]
├─ round.status=OPEN 且 now < closeTime → 通過
├─ now >= closeTime → 拒絕（已封盤）   ← 封盤是硬邊界，boundary-equal（now=closeTime）視為已封盤
└─ round 不存在      → 拒絕（無此局）

[去重]（idemKey）
├─ 首次（SET NX 成功） → 正常受理
└─ 已存在              → 回上次結果，不重複凍結 / 不重複建注

[凍結金額]
├─ 可用餘額 >= amount → 原子凍結 amount，繼續
└─ 可用餘額 <  amount → 拒絕（餘額不足），不建注（絕不透支）

[受理下注]
├─ 建注成功 → 回 ACCEPTED
└─ 建注失敗 → 解凍先前凍結（補償），回錯誤

[局結算 / 取消]（局結束後，非下注入口即時路徑；此處只定語意邊界，實際派彩見 s05）
├─ 中     → 凍結轉實扣 + 派彩
├─ 未中   → 凍結轉實扣（願賭服輸，不退）
└─ 局取消 → 解凍全額退回

邊界：封盤瞬間並發下注，封盤後的注一律不受理；同一 idemKey 並發只受理一次。
```

## 4. State Transition（一張下注單）

```text
[受理] ──> 【ACCEPTED】
              │
              ├─(局結算·中)────> 【SETTLED_WIN】（凍結轉扣 + 派彩）
              ├─(局結算·未中)──> 【SETTLED_LOSE】（凍結轉扣）
              └─(局取消)───────> 【CANCELLED】（解凍退回）
```

## 5. Data Dictionary（邏輯欄位；實體 DDL 在各平台 SD）

```text
bet（下注單）
├─ betId(PK) / userId / tableId / roundId
├─ amount / status（ACCEPTED|SETTLED_WIN|SETTLED_LOSE|CANCELLED）
├─ idemKey（唯一，去重用）
└─ createdAt

round（局）
├─ roundId(PK) / tableId
├─ status（OPEN|CLOSED|SETTLED）
└─ openTime / closeTime

（可用餘額 / 凍結金額在 common.account，非本情境資料表）
```
