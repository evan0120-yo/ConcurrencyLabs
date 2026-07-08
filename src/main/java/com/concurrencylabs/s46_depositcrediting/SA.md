# SA.md — s46_depositcrediting（充值入帳系統）

> 事實文件（SASD）· 結構化分析。描述「充值入帳要做什麼」的**業務行為**，平台無關（P1..Pn 共用；各平台怎麼扛量在各自 SD）。
> 特性：在「事件會重放、鏈會回滾」的不確定環境下，保證每筆充值**只入帳一次、金額正確**。到帳可稍延遲（等確認），但不可錯。

## 1. Context Diagram

```text
[鏈上轉入] --(掃鏈事件: txHash, address, asset, amount)--> (充值入帳) --(確認足夠 → 入帳)--> [s48 資產錢包]

(充值入帳) --(掃鏈 / 確認數)--> [區塊鏈節點]
(充值入帳) --(充值單狀態)-----> [PostgreSQL deposit]
```

## 2. DFD（精簡 box flow）

```text
[掃鏈] --(偵測到轉入 txHash)--> [建/更新充值單]（依 txHash 去重）
   ▼
[等確認數]（累積區塊確認，避免鏈回滾造成假到帳）
   ├─(確認不足)──> 繼續等
   ├─(達確認數)
   │     ▼
   │  [冪等入帳]（txHash 唯一 → 只入一次）--> [s48 加對應資產]
   │     └─ 標記 CREDITED
   └─(未達確認前分叉 / 回滾)──> 作廢該筆（ORPHANED，不入帳）
```

## 3. Process Spec（行為基準，decision table）

```text
[確認機制]
├─ 確認數 >= 門檻 → 可入帳
└─ 確認數 < 門檻 → 繼續等（不入帳）

[冪等入帳]（txHash，核心命脈）
├─ 首次達確認 → 入帳一次
└─ 掃鏈事件重放 / 同 txHash 再現 → 忽略（絕不重複加）

[回滾處理]
└─ 入帳前發生鏈重組 / 分叉，該 tx 消失 → 作廢（ORPHANED）；已入帳者依政策沖正

[語意]
└─ 到帳可延遲（等確認），但金額必正確、只發生一次
```

## 4. State Transition（一筆充值）

```text
【DETECTED】──(累積確認)──> 【CONFIRMING】──(達確認數)──> 【CREDITED】
                                    └──(回滾/分叉)──> 【ORPHANED】
```

## 5. Data Dictionary（邏輯欄位；實體 DDL 在各平台 SD）

```text
deposit（充值單）
├─ txHash(PK, 去重) / userId / asset / amount
├─ confirmations / requiredConfirmations
├─ status（DETECTED|CONFIRMING|CREDITED|ORPHANED）
└─ detectedAt / creditedAt
```
