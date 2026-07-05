# SD.md — common/account（使用者身分 + 積分）@ L1

> 事實文件（SASD）· 結構化設計。共用有狀態小模組，正式交付級 self-contained。

## 0. 背景與範圍

```text
目的
└─ 提供跨情境共用的「使用者身分 + 積分餘額」：註冊、查餘額、原子扣減 / 退回

使用對象（呼叫者）
└─ 各情境的 Usecase / Service（如 s01_lottery 抽獎前扣 200 積分）

規模與未來估計（L1）
├─ 積分是「錢一般」的資料 → 真相放 DB（不放 Redis），用原子條件更新扣減
└─ 非目標：完整帳號系統（註冊/密碼/profile/session）與登入驗證
   └─ 那是 s28_loginspike / s29_tokensessionrefresh 的題目；本模組刻意無 auth
   └─ 真正的錢包/帳本一致性研究在 s06_gamewalletledger；本模組只是最薄積分
```

## 1. 架構總覽 + Structure Chart

```text
┌───────────────┐  呼叫  ┌────────────────┐  JPA  ┌──────────────┐
│ 情境 Usecase/ │ ────▶ │ AccountService │ ────▶ │ AccountRepo  │ ──▶ [PostgreSQL]
│ Service       │       └────────────────┘       └──────────────┘
└───────────────┘

AccountService
├─ deductPoints(userId, amount)   → 原子條件更新，回 success/insufficient
├─ refundPoints(userId, amount)   → 原子加回（補償）
├─ getBalance(userId)
└─ register(userId, initialPoints)（冪等）
```

## 2. Module Decomposition

```text
AccountService     積分扣減 / 退回 / 查詢 / 註冊（無 auth）
AccountRepository  users 表 JPA 存取；原子扣減用條件 UPDATE（@Modifying query）
User (Entity)      users 表映射
```

## 3. Coupling & Cohesion + 依賴方向

```text
allowed    情境 → AccountService → AccountRepository → PostgreSQL
must-not   AccountService ─X→ 任何情境類（不可反向依賴情境）
           不在此做登入 / token 驗證（無 auth）
```

## 4. 資料模型 / Table Schema

```text
users
├─ id            BIGINT / VARCHAR   PK   （= userId，呼叫端傳入）
├─ points        BIGINT   NOT NULL  CHECK(points >= 0)   積分餘額
├─ created_at    TIMESTAMP
└─ updated_at    TIMESTAMP

原子扣減（多台 server 安全）
UPDATE users SET points = points - :amount, updated_at = now()
WHERE id = :id AND points >= :amount        -- rowsAffected=0 → 積分不足
```

## 5. API 設計 / Schema（對內方法）

```text
本模組 L1 不對外開 HTTP（供其他情境內部呼叫）：

DeductResult deductPoints(String userId, long amount)   // SUCCESS / INSUFFICIENT
void         refundPoints(String userId, long amount)   // 補償
long         getBalance(String userId)
void         register(String userId, long initialPoints)// 冪等；測試/初始化

（若之後要暴露充值/查詢 HTTP，再於此節補端點與錯誤碼）
```

## 6. 關鍵流程 Sequence

```text
情境要扣 200 積分
 → AccountService.deductPoints(userId, 200)
 → AccountRepository 條件 UPDATE
     ├─ rowsAffected=1 → 回 SUCCESS
     └─ rowsAffected=0 → 回 INSUFFICIENT（餘額不足）
```

## 7. 非功能需求（NFR）

```text
├─ 一致性：積分不可透支（DB 原子條件更新 + CHECK 約束雙保險）
├─ 多台 server：扣減原子性在 DB 行鎖，跨 server 安全
└─ 補償：退積分必成功（供跨儲存失敗回滾）
```

## 8. 相依與外部整合

```text
└─ PostgreSQL（users 表）
```
