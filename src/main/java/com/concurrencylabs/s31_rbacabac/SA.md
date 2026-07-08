# SA.md — s31_rbacabac（權限中心 RBAC / ABAC）

> 事實文件（SASD）· 結構化分析。描述「權限中心要做什麼」的**業務行為**，平台無關（P1..Pn 共用；各平台怎麼扛量在各自 SD）。
> 特性：**極高頻權限查詢**（每受保護操作一問）靠快取扛，但「權限變更即時生效」逼快取必須能快速失效——取捨是核心。

## 1. Context Diagram

```text
[受保護操作] --(問: subject(role/attr), action, resource)--> (權限中心) --(ALLOW / DENY)--> [呼叫方]
[管理員]     --(改角色 / 收權)--> (權限中心) --(變更 → 失效快取)--> 即時生效

(權限中心) --(權限查詢 / 快取)--> [util.redis]
(權限中心) --(角色 / 策略)-----> [PostgreSQL role / policy]
```

## 2. DFD（精簡 box flow）

```text
[問權限] --(subject, action, resource, tenant)--> [查快取]
   │(命中)──> 直接回 ALLOW / DENY（讀爆靠這扛）
   │(未命中)
   ▼
[評估]
   ├─ RBAC：subject 的 role 是否擁有該 action 權限
   └─ ABAC：依屬性 / 條件（部門 / 時間 / 資源歸屬）判定
   ▼
[填快取] ──> 回結果

[變更（收權 / 改角色）] --> [主動失效相關快取]（即時生效）
```

## 3. Process Spec（行為基準，decision table）

```text
[查詢]（讀爆）
├─ 快取命中 → 直接回
└─ 未命中 → 評估後填快取（不打主庫的高頻路徑）

[RBAC / ABAC]
├─ RBAC：role → permission 映射
└─ ABAC：屬性 / 條件式（更細粒度）

[變更即時生效]（核心矛盾）
└─ 權限變更（收權 / 改角色 / 改策略）→ 主動失效相關快取；容忍極短傳播延遲
   絕不可讓被收權者長時間仍以舊快取通過

[多租戶]
└─ 每租戶角色體系 / 策略獨立，查詢帶 tenant 維度隔離

不變量：高頻查詢走快取不打主庫；變更能快速失效使其即時生效。
```

## 4. State Transition

```text
（無業務狀態機；權限快取的 命中 / 未命中 / 失效 生命週期，由變更事件驅動失效）
```

## 5. Data Dictionary（邏輯欄位；實體 DDL 在各平台 SD）

```text
role_permission（RBAC 映射）
├─ tenantId / roleId / permission（複合）

policy（ABAC 策略）
├─ policyId(PK) / tenantId / effect（ALLOW|DENY）/ condition（屬性條件）
└─ priority

perm_cache（權限決策快取，活在 Redis）
├─ key（subject+action+resource+tenant）/ decision / ttl
```
