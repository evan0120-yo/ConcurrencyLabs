# SA.md — s63_traceingestion（Distributed Trace Ingestion）

> 事實文件（SASD）· 結構化分析。描述「Trace 收集要做什麼」的**業務行為**，平台無關（P1..Pn 共用；各平台怎麼扛量在各自 SD）。
> 資料量 = 請求數 × 每請求 span 數，極大。正常 trace 可大量丟，但「出錯 / 異常慢」的那條恰恰最該留——逼出**尾部抽樣**（先看完再決定）。

## 1. Context Diagram

```text
[跨服務呼叫] --(span: traceId, spanId, service, duration, error)--> (Trace 收集) --(抽樣後存)--> [trace 儲存]
```

## 2. DFD（精簡 box flow）

```text
[span 湧入] --(traceId)--> [按 traceId 暫存組裝]（等同一 trace 的 span 到齊 / 逾時）
   ▼
[尾部抽樣決策]（trace 完成後才決定留不留）
   ├─(有 error / 異常慢)──> 全保留（最想看的）
   ├─(正常)──> 抽樣保留（大量丟，保留代表性）
   ▼
[寫入 trace 儲存]
```

## 3. Process Spec（行為基準，decision table）

```text
[必須抽樣]
└─ 全存不現實（量體 = 請求 × span）

[尾部抽樣]（tail-based，核心）
├─ 等一條 trace 的 span 收齊（或逾時）後再決定
├─ 含 error / 超過延遲門檻 → 全保留
└─ 正常 trace → 按比例抽樣（其餘丟棄）

[取捨]
└─ 隨機抽樣省錢但會漏掉稀有錯誤（最想看的那條）→ 故用尾部抽樣，錯/慢的一定留

[組裝]
└─ 依 traceId 聚 span；逾時未齊的 trace 也要能決策（不無限等）
```

## 4. State Transition（一條 trace）

```text
【COLLECTING（span 收集中）】──(收齊 / 逾時)──> [尾部抽樣判定]
                                              ├─(錯/慢)──> 【KEPT】
                                              └─(正常抽中/抽不中)──> 【SAMPLED / DROPPED】
```

## 5. Data Dictionary（邏輯欄位；實體 DDL 在各平台 SD）

```text
span
├─ traceId / spanId / parentSpanId / service / operation
├─ startTs / duration / error（是否錯誤）

trace（一條完整鏈路，抽樣決策單位）
├─ traceId(PK) / spanCount / hasError / maxDuration
└─ decision（KEPT|SAMPLED|DROPPED）
```
