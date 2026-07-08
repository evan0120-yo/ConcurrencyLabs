# SA.md — s41_matchingengine（鏈下撮合 Engine）

> 事實文件（SASD）· 結構化分析。描述「撮合要做什麼」的**業務行為**，平台無關（P1..Pn 共用；各平台怎麼扛量在各自 SD）。
> 交易所心臟：**價格優先、時間優先**撮合；同一 symbol 必須**嚴格順序、極低延遲**——這逼核心走順序化，非撮合工作全趕出去。不同 symbol 互相獨立。

## 1. Context Diagram

```text
[下單入口 s42] --(訂單: symbol, side, price, qty, orderId)--> (撮合 Engine) --(成交 / 掛單)--> 
                                                                    ├--(成交事件)--> [s43 行情 / 結算]
                                                                    └--(訂單簿快照)--> [恢復用]
```

## 2. DFD（精簡 box flow）

```text
[訂單流入]（同一 symbol 嚴格 FIFO 排序）
   ▼
[撮合]（對該 symbol 訂單簿）
   ├─ 買一 >= 賣一 → 成交（依價格優先、同價時間優先，逐檔吃）
   │     └─ 產生 trade 事件 → 推行情 / 結算 / 更新訂單狀態
   └─ 不可成交 → 掛進訂單簿等待
   ▼
（撤單）── 從訂單簿移除該掛單
```

## 3. Process Spec（行為基準，decision table）

```text
[撮合規則]
├─ 價格優先：買單價高者優先、賣單價低者優先
└─ 時間優先：同價先到先成交

[嚴格順序]（核心不變量）
└─ 同一 symbol 的訂單必須依序處理（單元內序列化）；絕不並發亂序
   → 不同 symbol 之間獨立，可分開處理

[成交]
├─ 完全成交 → FILLED
├─ 部分成交 → PARTIAL（剩餘續掛）
└─ 撤單 → CANCELLED（移出訂單簿）

[低延遲]
└─ 核心只做撮合；記帳 / 通知 / 行情等一律事件外拋、非同步（不進撮合關鍵路徑）

不變量：訂單簿一致（成交量守恆，買賣配對金額相等）；同 symbol 事件序可回放恢復。
```

## 4. State Transition（一張訂單）

```text
【NEW】──(部分成交)──> 【PARTIAL】──(全部成交)──> 【FILLED】
    ├──(直接全成)──> 【FILLED】
    └──(撤單)──> 【CANCELLED】
```

## 5. Data Dictionary（邏輯欄位；實體 DDL 在各平台 SD）

```text
order（訂單，活在撮合單元記憶體 + 落地）
├─ orderId(PK) / symbol / side（BUY|SELL）/ price / qty / filledQty
├─ status（NEW|PARTIAL|FILLED|CANCELLED）/ seq（同 symbol 序號）
└─ createdAt

trade（成交）
├─ tradeId(PK) / symbol / price / qty / makerOrderId / takerOrderId
└─ ts
```
