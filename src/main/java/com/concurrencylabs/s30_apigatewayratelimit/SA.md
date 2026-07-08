# SA.md — s30_apigatewayratelimit（API Gateway / Rate Limit）

> 事實文件（SASD）· 結構化分析。描述「Gateway 要做什麼」的**業務行為**，平台無關（P1..Pn 共用；各平台怎麼扛量在各自 SD）。
> 全站入口第一道門：路由 + 認證 + 限流。它慢全站都慢，本身要極輕快；限流要準（誤傷 vs 漏擋），且多台之間計數要一致。

## 1. Context Diagram

```text
[所有請求] --> (Gateway) --(通過 → 轉發)--> [後端服務]
                 │
                 └--(超量)--> 回 429

(Gateway) --(限流計數 / 認證)--> [util.redis]（跨多台共享）
```

## 2. DFD（精簡 box flow）

```text
[請求] --> [認證]（這人是誰、能不能進；驗 token 見 s29）
   ▼
[限流判定]（per user / IP / tenant / API）
   ├─ 計數走共享儲存（多台 Gateway 一致）
   ├─(未超量)──> 放行
   └─(超量)──> 429（快速拒，不打後端）
   ▼
[路由]（轉給正確後端服務）
```

## 3. Process Spec（行為基準，decision table）

```text
[限流演算法]
└─ 令牌桶 / 滑動窗；原子計數（Redis 原子 / Lua），避免併發超放

[限流維度]
└─ 可按 user / IP / tenant / API 分別設額；命中任一超額即擋

[分散式一致]（核心不變量）
└─ 多台 Gateway 的計數必須收斂到共享儲存（不可各算各的造成整體超放）

[精度取捨]
├─ 誤傷（多算）→ 擋到正常人 → 不可過嚴
└─ 漏擋（少算）→ 擋不住濫用 → 不可過鬆
   → 以窗口 / 桶容量平衡

[效能]
└─ Gateway 在最前端關鍵路徑，限流判定必須極快（本地快取 + 共享計數）
```

## 4. State Transition

```text
（無業務狀態機；只有限流計數的 窗口滾動 / 令牌補充 週期）
```

## 5. Data Dictionary（邏輯欄位；實體 DDL 在各平台 SD）

```text
rate_rule（限流規則）
├─ ruleId(PK) / dimension（USER|IP|TENANT|API）/ limit / windowSec
└─ enabled

rate_counter（限流計數，活在 Redis）
├─ key（dimension + 值 + 窗口）/ count / windowStart
```
