# SA.md — s47_withdrawalreview（提幣審核 / 出金系統）

> 事實文件（SASD）· 結構化分析。描述「提幣出金要做什麼」的**業務行為**，平台無關（P1..Pn 共用；各平台怎麼扛量在各自 SD）。
> 特性：出金**不可逆**——寧可慢、不可錯。用多重審核 + 可靠上鏈重試，把「錯誤 / 重複出金」機率壓到零；安全優先於即時。

## 1. Context Diagram

```text
[使用者] --(提幣申請: userId, asset, amount, toAddress)--> (提幣審核) --(受理 / 拒絕 / 完成)--> [使用者]

(提幣審核) --(風控)--------> [s26 風控]
(提幣審核) --(扣資產)------> [s48 資產錢包]
(提幣審核) --(簽名上鏈)----> [熱錢包 / 區塊鏈]
```

## 2. DFD（精簡 box flow）

```text
[提幣申請] --(withdrawId 去重)--> [扣可用資產 / 凍結]（先鎖住，避免重複提）
   ▼
[風控審核]（額度 / 名單 / 行為）
   ├─(可疑 / 大額)──> 人工 / 多簽審核
   │       └─(駁回)──> 解凍退回，REJECTED
   └─(通過)
   ▼
[熱錢包簽名] --> [上鏈廣播]
   ├─(廣播失敗)──> 安全重試（同 withdrawId 不重複出金）
   └─(成功 + 鏈確認)──> COMPLETED
```

## 3. Process Spec（行為基準，decision table）

```text
[前置鎖定]（withdrawId 去重）
└─ 受理即扣 / 凍結資產，避免同一申請重複出金

[風控審核]
├─ 小額 + 通過規則 → 自動放行
├─ 大額 / 可疑 → 人工 / 多簽（安全優先）
└─ 駁回 → 解凍退回，REJECTED

[上鏈可靠性]（核心不變量）
├─ 簽名 + 廣播；失敗可重試
└─ 重試不得造成重複出金（同 withdrawId 綁定唯一鏈上交易；已廣播者查詢確認而非重發）

[不可逆語意]
└─ 可延遲（審核 / 排程 / 重試都可等），但絕不可錯 / 重複；限流攔異常提幣
```

## 4. State Transition（一筆提幣）

```text
【APPLIED】──(扣資產)──> 【REVIEWING】──(通過)──> 【SIGNING】──> 【BROADCASTING】──(鏈確認)──> 【COMPLETED】
                              └──(駁回)──> 【REJECTED】（解凍退回）
```

## 5. Data Dictionary（邏輯欄位；實體 DDL 在各平台 SD）

```text
withdrawal（提幣單）
├─ withdrawId(PK, 去重) / userId / asset / amount / toAddress
├─ status（APPLIED|REVIEWING|SIGNING|BROADCASTING|COMPLETED|REJECTED）
├─ txHash（上鏈後）/ reviewType（AUTO|MANUAL|MULTISIG）
└─ createdAt / completedAt
```
