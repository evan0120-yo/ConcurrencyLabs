# SA.md — s54_eventpipeline（事件資料管線 Pipeline）

> 事實文件（SASD）· 結構化分析。描述「事件管線要做什麼」的**業務行為**，平台無關（P1..Pn 共用；各平台怎麼扛量在各自 SD）。
> 多方事件的中樞：收 → 驗 → 分 → 扇出。要在海量寫入下同時做到**壞資料隔離**（死信，不擋好資料）與**多下游互不拖累**。

## 1. Context Diagram

```text
[各方: App / Web / Server] --(事件: type, schema, payload)--> (事件管線) --(分流扇出)--> [多下游: 計費/報表/推薦]

(事件管線) --(壞資料)--> [死信 DLQ]（隔離）
(事件管線) --(分 topic / 扇出)--> [MQ topics]
```

## 2. DFD（精簡 box flow）

```text
[事件湧入] --> [驗 schema]
   ├─(格式不對)──> [死信 DLQ]（隔離，不擋好資料流）
   │(合法)
   ▼
[分流]（依 type → 對應 topic）
   ▼
[扇出]（同一份事件 → 多個下游各自消費）
   ├─ 計費 consumer
   ├─ 報表 consumer
   └─ 推薦 consumer
   （各下游速度不同，彼此解耦、互不拖累）
```

## 3. Process Spec（行為基準，decision table）

```text
[schema 驗證]（核心不變量）
├─ 合法 → 進分流
└─ 不合法（某上游改壞格式）→ 死信隔離；絕不污染 / 堵塞好資料

[分流]
└─ 依事件 type 路由到對應 topic

[扇出 / 解耦]（核心不變量）
├─ 一份事件供多下游各取所需
└─ 下游各自消費進度獨立；某下游慢 / 掛不得拖累其他下游與主流

[寫爆]
└─ 入口承海量寫入 → 緩衝 + 分區承接
```

## 4. State Transition（一個事件）

```text
【RECEIVED】──(schema 合法)──> 【ROUTED】──(扇出)──> 【DISPATCHED（多下游各自消費）】
        └──(schema 不合法)──> 【DEAD（DLQ 隔離）】
```

## 5. Data Dictionary（邏輯欄位；實體 DDL 在各平台 SD）

```text
event（事件）
├─ eventId(PK) / type / schemaVersion / payload / source
├─ status（RECEIVED|ROUTED|DISPATCHED|DEAD）
└─ ts

（topic 路由表、consumer group、DLQ 為基礎設施，見各平台 SD）
```
