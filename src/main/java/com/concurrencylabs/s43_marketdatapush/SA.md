# SA.md — s43_marketdatapush（Market Data 行情推送）

> 事實文件（SASD）· 結構化分析。描述「行情推送要做什麼」的**業務行為**，平台無關（P1..Pn 共用；各平台怎麼扛量在各自 SD）。
> 一寫多讀的極致：一份高頻行情要扇出給海量長連線訂閱者。靠**分層推送**（交易者收 tick、看客收聚合 / 降頻）才扛得住。

## 1. Context Diagram

```text
[撮合成交 / 盤口更新] --(行情事件: symbol, price, 盤口)--> (行情推送) --(分層扇出)--> [海量訂閱連線]

(行情推送) --(訂閱關係 / 連線)--> [long-conn gateway]
```

## 2. DFD（精簡 box flow）

```text
[行情事件] --(成交 / 盤口 / K 線更新)--> [分層生成]
   ├─ tick 級（每筆，最即時）
   ├─ 秒級（節流聚合）
   └─ 聚合級（更粗，給看客）
   ▼
[扇出推送]（依 symbol + 訂閱精細度，推給對應連線）
   ├─ 交易者訂閱 → tick 級即時
   └─ 一般看客訂閱 → 秒級 / 聚合級（降頻）
   ▼
（波動時更新頻率飆高 → 對降頻層加大節流，保護整體）
```

## 3. Process Spec（行為基準，decision table）

```text
[一寫多讀扇出]
└─ 一份行情事件扇出給所有訂閱該 symbol 的連線

[分層推送]（核心不變量）
├─ tick 級：交易者，最低延遲、不節流
├─ 秒級 / 聚合級：一般看客，可節流 / 降頻 / 合併
└─ 依訂閱精細度分流，非交易者不吃 tick 洪流

[長連線]
├─ 海量連線同時維持；訂閱 / 退訂管理
└─ 掉線重連 → 補當前快照 + 續推

[波動保護]
└─ 更新頻率暴增時，對降頻層加大節流 / 丟中間幀（tick 層仍保即時）
```

## 4. State Transition

```text
（推送型，無業務狀態機；訂閱連線的 訂閱 / 推送 / 退訂 / 重連 生命週期）
```

## 5. Data Dictionary（邏輯欄位；實體 DDL 在各平台 SD）

```text
subscription（訂閱關係，活在 gateway / Redis）
├─ connId / userId / symbol / level（TICK|SECOND|AGG）

market_snapshot（行情快照，供重連補發）
├─ symbol(PK) / lastPrice / 盤口(bids/asks) / updatedAt
```
