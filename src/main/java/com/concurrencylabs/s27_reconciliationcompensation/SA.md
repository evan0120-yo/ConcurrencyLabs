# SA.md — s27_reconciliationcompensation（對帳與補償平台）

> 事實文件（SASD）· 結構化分析。描述「對帳補償要做什麼」的**業務行為**，平台無關（P1..Pn 共用；各平台怎麼扛量在各自 SD）。
> 特性：**事後批次**，主交易絕不等它。找差異只是第一步，用補償狀態機把差異收斂補平才是目的；全程不影響即時交易。

## 1. Context Diagram

```text
[排程觸發] --(對帳: period)--> (對帳補償) --(差異單 → 補償 → 補平 / 人工)--> [營運 / 稽核]

(對帳補償) --(拉我方帳)----> [s25 ledger]
(對帳補償) --(拉第三方帳)--> [第三方對帳檔]
(對帳補償) --(差異單 / 補償狀態)--> [PostgreSQL diff]
```

## 2. DFD（精簡 box flow）

```text
[排程] --(period)--> [拉雙方帳檔]（我方 ledger + 第三方檔，分批）
   ▼
[逐筆比對]（依 bizNo / 金額 / 時間）
   ├─(一致)──> 打勾（對平）
   └─(差異)──> 建差異單
        ▼
   [補償狀態機]
   ├─ 我方有對方無 / 金額不符 → 依規則補償（補記 / 沖正 / 重放）
   ├─ 對方有我方無 → 補記或掛帳
   └─ 無法自動補 → 轉人工
```

## 3. Process Spec（行為基準，decision table）

```text
[事後隔離]（核心不變量）
└─ 對帳一律事後批次，主交易不等它、不被它拖累

[比對]
├─ 依 bizNo 配對；金額 / 時間 / 狀態逐項核對
└─ 一致打勾；任一不符 → 差異單

[差異分類 → 補償]
├─ 我方少記 → 補記
├─ 我方多記 / 重記 → 反向沖正
├─ 遲到未達 → 等待 / 重放
└─ 無法自動判定 → 轉人工處理

[補償冪等]
└─ 同一差異單的補償只執行一次（避免補過頭）

不變量：收斂後雙方帳最終對平；每筆差異都有交代（補平 / 掛帳 / 人工）。
```

## 4. State Transition（一張差異單）

```text
【PENDING】──(自動補償)──> 【PROCESSING】──(成功)──> 【RESOLVED】
                                    └──(無法自動)──> 【MANUAL】（人工）
```

## 5. Data Dictionary（邏輯欄位；實體 DDL 在各平台 SD）

```text
recon_batch（對帳批次）
├─ batchId(PK) / period / status（RUNNING|DONE）
└─ ourCount / theirCount / diffCount

diff（差異單）
├─ diffId(PK) / bizNo / ourRecord / theirRecord
├─ type（MISSING_OURS|MISSING_THEIRS|AMOUNT_MISMATCH|LATE）
├─ status（PENDING|PROCESSING|RESOLVED|MANUAL）
└─ createdAt / resolvedAt
```
