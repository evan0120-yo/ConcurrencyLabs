# SA.md — s29_tokensessionrefresh（Token / Session / Refresh 系統）

> 事實文件（SASD）· 結構化分析。描述「身分驗證要做什麼」的**業務行為**，平台無關（P1..Pn 共用；各平台怎麼扛量在各自 SD）。
> 特性：**驗 token 是全站最高頻的讀**，要極快（不打後端）；但登出 / 盜用要能**即時撤銷**——這與無狀態設計天生矛盾，取捨是核心。

## 1. Context Diagram

```text
[每個 API 請求] --(帶 token)--> (驗 token) --(VALID / EXPIRED / REVOKED)--> [後端]
[過期]         --(refresh token)--> (換發) --(新 access token)--> [端]

(驗 token) --(撤銷黑名單查詢)--> [util.redis]
(換發 / 撤銷) --(session / refresh)--> [session store]
```

## 2. DFD（精簡 box flow）

```text
[請求帶 token] --> [驗 token]（本地 / 快取，極快）
   ├─(簽章無效 / 過期)──> 401 / 走 refresh
   ├─(在撤銷黑名單)──> REVOKED，拒
   └─(有效)──> 放行

[過期] --(refresh token)--> [換發]
   ├─ refresh 有效且未被盜用（rotation 檢查）→ 發新 access（+ 輪換 refresh）
   └─ 偵測重放 / 已撤銷 → 拒，強制重新登入

[登出 / 盜用] --> [撤銷]（加入黑名單，短延遲全域生效）
```

## 3. Process Spec（行為基準，decision table）

```text
[驗證]（最高頻，必須極快）
├─ 簽章有效 + 未過期 + 不在黑名單 → VALID 放行
├─ 過期 → 走 refresh
└─ 在黑名單 → REVOKED 拒

[撤銷]（與無狀態的矛盾點）
├─ 登出 / 盜用 → 加入撤銷黑名單
└─ 生效延遲：容忍極短同步延遲（黑名單傳播）；不可長時間仍可用

[refresh 安全]
├─ refresh rotation（每次換發輪換），重放偵測 → 判定盜用即全撤銷
└─ refresh 為敏感操作，頻率遠低於 access 驗證

不變量：驗證不打後端主庫（否則成瓶頸）；撤銷需即時（短延遲）生效。
```

## 4. State Transition（一個 token / session）

```text
【VALID】──(到期)──> 【EXPIRED】──(refresh)──> 新 VALID
      └──(登出/盜用撤銷)──> 【REVOKED】（不可再用）
```

## 5. Data Dictionary（邏輯欄位；實體 DDL 在各平台 SD）

```text
session（登入態）
├─ sessionId(PK) / userId / refreshTokenHash
└─ createdAt / expireAt

revoke_entry（撤銷黑名單，活在 Redis，短 TTL = token 壽命）
├─ jti / sessionId / revokedAt
└─（access token 建議短命 + 黑名單；refresh 走 rotation）
```
