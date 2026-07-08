# SA.md — s66_configfeatureflag（Config Center / Feature Flag）

> 事實文件（SASD）· 結構化分析。描述「Config 中心要做什麼」的**業務行為**，平台無關（P1..Pn 共用；各平台怎麼扛量在各自 SD）。
> config 是全站依賴的單點：變更要能快速安全下發（灰度、秒回退），同時要防「所有服務同時來打」的**驚群**把中心自己弄垮。

## 1. Context Diagram

```text
[成百上千服務實例] --(拉 config / 監聽變更)--> (Config 中心) --(config + 變更推送)--> [實例本地快取]
[管理員] --(改設定 / 開關 + 灰度)--> (Config 中心) --(推送生效)--> [實例]

(Config 中心) --(config / 版本)--> [PostgreSQL config]
```

## 2. DFD（精簡 box flow）

```text
[實例讀 config] --> 本地快取一份 + 監聽變更（長輪詢 / 推送）
   └─ 未快取 / 首連 → 拉中心（驚群防護：隨機錯開 / 多層快取）

[管理員改開關] --> [灰度下發]
   ├─ 先 1% / 特定租戶生效
   ├─ 觀察無異常 → 逐步全量
   └─ 出事 → 秒回退（切回舊值）
   ▼
[推送變更] → 所有相關實例更新本地快取
```

## 3. Process Spec（行為基準，decision table）

```text
[讀]（讀爆）
└─ 實例本地快取 config，不每次打中心；監聽變更增量更新

[變更下發]
├─ 灰度：按比例（1%→全量）/ 按租戶 / 按環境生效
└─ 秒回退：出事立即切回舊版本

[驚群防護]（核心）
└─ config 中心重啟 / 大量實例同時重連 → 隨機退避 + 多層快取，避免同時打爆單點

[一致性]
└─ 變更容忍極短傳播延遲；同一版本全實例最終一致
```

## 4. State Transition（一個 feature flag）

```text
【OFF】──(灰度開)──> 【GRAY（x%）】──(全量)──> 【ON】
   ▲──────────(秒回退)──────────┘
```

## 5. Data Dictionary（邏輯欄位；實體 DDL 在各平台 SD）

```text
config_item（設定 / 開關）
├─ key(PK) / value / version
├─ rolloutPercent（灰度比例）/ targetTenants[]
└─ updatedAt
```
