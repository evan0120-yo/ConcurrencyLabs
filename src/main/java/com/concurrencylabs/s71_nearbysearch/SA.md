# SA.md — s71_nearbysearch（地理位置查詢 / Nearby Search）

> 事實文件（SASD）· 結構化分析。描述「附近查詢要做什麼」的**業務行為**，平台無關（P1..Pn 共用；各平台怎麼扛量在各自 SD）。
> 特性：把二維位置變成可快速查的索引（網格 / geohash）；熱門地區查詢集中，「附近」本就模糊、可近似換速度。

## 1. Context Diagram

```text
[使用者] --(查附近: lat, lng, radius, type)--> (Nearby) --(範圍內目標依距離排序)--> [使用者]
[移動目標] --(位置更新: objId, lat, lng)--> (Nearby) --(索引更新)--> [geo 索引]

(Nearby) --(geo 索引 / 熱門格快取)--> [util.redis]
```

## 2. DFD（精簡 box flow）

```text
[查附近] --(lat, lng, radius)--> [算涵蓋網格 / geohash 前綴]
   ▼
[取範圍格內目標]
   ├─(熱門格)──> 命中快取（熱門商圈查詢集中）
   │(非熱門)
   ▼
[算距離 + 排序]（濾掉超過 radius，依距離排）── 回結果（可近似）

【移動目標】[位置更新] --> 更新目標所在格（司機類高頻更新）
```

## 3. Process Spec（行為基準，decision table）

```text
[地理索引]
└─ 用網格 / geohash 把位置離散化；查詢 = 找涵蓋的格再過濾

[熱門格快取]
└─ 市中心 / 熱門商圈查詢集中 → 快取該格結果

[近似]（放寬）
└─「附近」本就模糊 → 可用格粒度近似、稍舊結果換速度

[移動目標]
└─ 司機類位置高頻更新 → 更新所在格；查詢用當前格快照

[排序]
└─ 範圍內依距離排序；濾掉超過 radius 的
```

## 4. State Transition

```text
（查詢型，無業務狀態機；移動目標的位置在網格間 遷移 更新）
```

## 5. Data Dictionary（邏輯欄位；實體 DDL 在各平台 SD）

```text
geo_object（地理目標）
├─ objId(PK) / type（SHOP|DRIVER|…）/ lat / lng / geohash（索引）
└─ updatedAt

grid_cache（熱門格結果，活在 Redis）
├─ geohashPrefix / objIds[] / cachedAt
```
