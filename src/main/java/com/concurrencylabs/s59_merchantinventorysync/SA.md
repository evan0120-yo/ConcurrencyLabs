# SA.md — s59_merchantinventorysync（Marketplace 商家庫存同步）

> 事實文件（SASD）· 結構化分析。描述「商家同步要做什麼」的**業務行為**，平台無關（P1..Pn 共用；各平台怎麼扛量在各自 SD）。
> 與多租戶「吵鬧鄰居」同構：大商家的批量同步不能佔滿通道，害小商家的變更遲遲同步不上。同步天生非同步，失敗要能補償。

## 1. Context Diagram

```text
[商家系統: A/B/大商家C] --(變更: merchantId, 商品/庫存 delta)--> (庫存同步) --(平台商品更新)--> [平台]

(庫存同步) --(同步任務 / 補償)--> [MQ / PostgreSQL sync_task]
(庫存同步) --(平台商品 / 庫存)--> [平台商品庫]
```

## 2. DFD（精簡 box flow）

```text
[商家變更] --(merchantId, 變更集)--> [建同步任務]（依 merchant 分隔離通道）
   ▼
[非同步拉取 / 套用]
   ├─ 小商家（少量變更）→ 正常通道快速套用
   ├─ 大商家（一次 10 萬 SKU）→ 獨立通道 / 限流分批，不佔滿共用通道
   ▼
   ├─(成功)──> 平台商品 / 庫存更新
   └─(失敗)──> 補償重試（冪等，不重複套用）
```

## 3. Process Spec（行為基準，decision table）

```text
[非同步]
└─ 商家改完，平台在背後拉取套用（不同步阻塞商家）

[商家隔離]（核心：吵鬧鄰居）
├─ per-merchant 通道 / quota：大商家批量不得佔滿共用通道
└─ 大商家（hot merchant）→ 獨立通道 / 限流分批，保護小商家的即時性

[失敗補償]
├─ 同步失敗 → 補償重試
└─ 冪等：同一變更重試不得重複套用（依變更版本 / 序號）

[一致性]
└─ 最終一致（同步有延遲）；小商家單筆變更不應被大商家批量餓死
```

## 4. State Transition（一個同步任務）

```text
【PENDING】──(拉取套用)──> 【SYNCING】──(成功)──> 【DONE】
                              └──(失敗)──> 【RETRYING】──(耗盡)──> 【FAILED（人工）】
```

## 5. Data Dictionary（邏輯欄位；實體 DDL 在各平台 SD）

```text
sync_task（同步任務）
├─ taskId(PK) / merchantId / changeSet / version
├─ status（PENDING|SYNCING|DONE|RETRYING|FAILED）/ attempts
└─ createdAt

merchant（商家 / 租戶）
├─ merchantId(PK) / tier（NORMAL|HOT）/ syncQuota
```
