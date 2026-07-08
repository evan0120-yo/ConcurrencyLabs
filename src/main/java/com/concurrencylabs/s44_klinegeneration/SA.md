# SA.md — s44_klinegeneration（K 線生成系統）

> 事實文件（SASD）· 結構化分析。描述「K 線生成要做什麼」的**業務行為**，平台無關（P1..Pn 共用；各平台怎麼扛量在各自 SD）。
> 特性：進來寫爆（tick 流）、出去讀爆（看圖）。同一份 tick 要餵養多個時間週期，還要正確處理「當前未收 K」的即時更新與**收尾定案**。熱門即時、冷門延遲。

## 1. Context Diagram

```text
[成交 tick] --(symbol, price, qty, ts)--> (K 線生成) --(多週期 K 線)--> [看盤者]

(K 線生成) --(即時聚合 / 當前 K)--> [util.redis]
(K 線生成) --(已收 K 落地)--------> [PostgreSQL / 時序庫]
```

## 2. DFD（精簡 box flow）

```text
[tick 流入] --(symbol, price, qty, ts)--> [多週期聚合]
   ├─ 1 分 K：更新當前這根（O 不變、H/L/C 隨新 tick 更新、V 累加）
   ├─ 5 分 K / 1 小時 K / 日 K：同一 tick 同步餵各週期當前 K
   ▼
[當前 K 更新]（未收，持續變）── 推給看圖者「正在成形的那根」
   ▼（時間窗結束）
[收尾定案]（該根 K 封閉、落地）── 開新一根
   ▼
[看圖讀取]：歷史 K 走落地、當前 K 走即時（熱門即時算、冷門延遲產）
```

## 3. Process Spec（行為基準，decision table）

```text
[多週期聚合]
└─ 同一 tick 同步更新所有週期的「當前 K」（1m/5m/1h/1d…）

[當前 K 更新]
├─ 第一筆 tick → 開盤 O = price
├─ 後續 tick → H = max、L = min、C = 最新 price、V += qty
└─ 當前 K 持續推送（未收，可變）

[收尾定案]（時間窗邊界，核心不變量）
└─ 窗結束（如整分鐘）→ 該 K 封閉定案、落地，不再變；開新一根
   邊界：tick 依 ts 歸屬正確的時間窗（跨窗 tick 不可算錯根）

[熱冷分級]
├─ 熱門 symbol → 即時聚合
└─ 冷門 symbol → 延遲 / 按需產

[讀]
└─ 歷史 K 走落地 + 快取；當前 K 走即時
```

## 4. State Transition（一根 K 線）

```text
【FORMING（當前未收，隨 tick 變）】──(時間窗結束)──> 【CLOSED（定案落地，不再變）】
```

## 5. Data Dictionary（邏輯欄位；實體 DDL 在各平台 SD）

```text
kline（K 線）
├─ symbol / period（1m|5m|1h|1d）/ openTime（複合鍵）
├─ open / high / low / close / volume
├─ closed（false=當前形成中 / true=已定案）
└─ （當前 K 活在 Redis，收尾後落地）
```
