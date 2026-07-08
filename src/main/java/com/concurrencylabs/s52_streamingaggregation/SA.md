# SA.md — s52_streamingaggregation（即時報表 / Streaming Aggregation）

> 事實文件（SASD）· 結構化分析。描述「即時聚合要做什麼」的**業務行為**，平台無關（P1..Pn 共用；各平台怎麼扛量在各自 SD）。
> 特性：在事件還在流時就出數字（近 N 分鐘曝光 / 點擊 / 收益）。事件會遲到，逼你在「即時但近似」與「準確但延遲」間選邊——多半選前者 + 事後修正。

## 1. Context Diagram

```text
[事件流: 曝光/點擊/轉換] --(event: metric, dims, ts)--> (即時聚合) --(即時報表數字)--> [Dashboard]

(即時聚合) --(時間窗聚合狀態)--> [util.redis / stream state]
(即時聚合) --(最終結果落地)----> [PostgreSQL / OLAP]
```

## 2. DFD（精簡 box flow）

```text
[事件流入] --(metric, dims, eventTime)--> [時間窗聚合]
   ├─ 近 1 分鐘曝光 / 近 5 分鐘收益 / 各維度即時值
   ▼
[即時輸出]（先給近似值）── Dashboard 高頻讀
   ▼（watermark 推進 / 遲到事件到達）
[修正]（把遲到事件補進對應窗，更新該窗結果）
   ▼（窗最終關閉）
[定案落地]
```

## 3. Process Spec（行為基準，decision table）

```text
[時間窗聚合]
└─ 依 eventTime 歸窗（非到達時間）；同事件餵各維度 / 各窗

[即時 vs 遲到]（核心取捨）
├─ 先給近似即時值（不等齊）→ Dashboard 秒級可見
└─ 遲到事件（watermark 之後才到）→ 修正對應窗結果

[定案]
└─ 窗超過允許遲到期 → 關閉定案、落地；之後不再變

[讀]
└─ 即時值走聚合狀態（近似）；歷史定案走落地
```

## 4. State Transition（一個聚合時間窗）

```text
【OPEN（累積中，近似即時）】──(遲到事件)──> 仍 OPEN（修正）──(超過遲到期)──> 【CLOSED（定案落地）】
```

## 5. Data Dictionary（邏輯欄位；實體 DDL 在各平台 SD）

```text
agg_window（聚合視窗結果）
├─ metric / dims / windowStart（複合鍵）/ value
├─ status（OPEN|CLOSED）/ approx（是否近似）
└─ updatedAt
```
