# SA.md — s75_featurestoreonlineserving（Feature Store / Online ML Serving）

> 事實文件（SASD）· 結構化分析。描述「特徵服務要做什麼」的**業務行為**，平台無關（P1..Pn 共用；各平台怎麼扛量在各自 SD）。
> 線上推論的硬時限逼特徵取用必須**極快**，且要能容忍「特徵缺失」用 **fallback** 續命，不讓單一特徵拖垮整個回應。

## 1. Context Diagram

```text
[推論請求] --(取特徵: entityIds, featureNames)--> (Feature Store) --(特徵向量)--> [模型推論]

(Feature Store) --(online 特徵)--> [util.redis / 低延遲 KV]
(Feature Store) --(nearline / offline 特徵)--> [預先算好的特徵層]
```

## 2. DFD（精簡 box flow）

```text
[推論取特徵] --(entityId, featureNames)--> [分層取用]
   ├─ online 特徵（即時行為）→ 低延遲 KV / 快取（最新、最快）
   ├─ nearline / offline 特徵（近期 / 歷史統計）→ 預先算好備著
   ▼
[組裝特徵向量]
   ├─(某特徵缺: 新用戶 / 資料延遲)──> fallback 預設值（不卡住）
   └─(硬時限內取齊)──> 回特徵向量
   ▼
（超時保護：拿不到就用 fallback，確保線上限時回應）
```

## 3. Process Spec（行為基準，decision table）

```text
[分層取用]
├─ online（即時行為特徵）→ 要最新、走低延遲 KV / 快取
└─ nearline / offline（統計 / 歷史）→ 離線 / 近線預先算好

[極低延遲]（核心天條）
└─ 每次推論取一批特徵且有硬時限 → 走快取層，不臨場重算

[缺特徵 fallback]（核心不變量）
├─ 某特徵缺（新用戶 / 資料延遲 / 取用超時）→ 用預設值 fallback
└─ 絕不因單一特徵缺失 / 慢而讓整個推論卡住 / 超時

[一致性]
└─ 訓練 / 服務特徵定義一致（避免 train-serving skew）；online 值可稍舊
```

## 4. State Transition

```text
（讀取服務型，無業務狀態機；特徵取用的 命中 / 缺失 fallback / 超時保護 路徑）
```

## 5. Data Dictionary（邏輯欄位；實體 DDL 在各平台 SD）

```text
feature_value（特徵值）
├─ entityId / featureName（複合）/ value / tier（ONLINE|NEARLINE|OFFLINE）
└─ updatedAt

feature_spec（特徵定義）
├─ featureName(PK) / dataType / defaultValue（fallback）/ ttl
```
