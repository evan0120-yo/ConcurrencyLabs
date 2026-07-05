# SA.md — common/account（使用者身分 + 積分）

> 事實文件（SASD）· 結構化分析。跨情境共用的**有狀態小模組**：只管「身分 + 積分」，無 auth。

## 1. Context Diagram

```text
[測試 / 上游] --(註冊: userId, 初始積分)--> (Account) 
[各情境 Service] --(扣積分 / 退積分 / 查餘額)--> (Account) --(結果)--> [各情境 Service]
(Account) --(users 讀寫)--> [PostgreSQL]
```

## 2. DFD

```text
[情境 Service] --(deductPoints: userId, amount)--> [Account]
   │
   ▼
[原子條件更新] UPDATE users SET points=points-amount WHERE id=? AND points>=amount
   ├─ rowsAffected=1 → 扣成功
   └─ rowsAffected=0 → 積分不足（回失敗，不扣成負）
        │
        ▼
   [PostgreSQL users]
```

## 3. Process Spec（行為基準，decision table）

```text
[deductPoints(userId, amount)]
├─ balance >= amount → 原子扣減，回 success
└─ balance <  amount → 回 insufficient（餘額不動）

[refundPoints(userId, amount)]
└─ 原子加回 amount（補償用；必成功）

[register(userId, initialPoints)]
└─ 建立 users 一筆（測試 / 初始化用）；已存在則冪等（不重建）

邊界
├─ 積分【不可扣成負】→ 靠「... AND points >= amount」原子條件保證
├─ 無 auth / 無 token：userId 由呼叫端直接傳入（labs 簡化）
└─ 多台 server：扣減原子性在 DB 行鎖，跨 server 安全（DEVELOPMENT §五）
```

## 4. State Transition

無長狀態（積分餘額是數值，非狀態機）。

## 5. Data Dictionary（邏輯）

```text
User
├─ userId        使用者識別（呼叫端傳入，非本模組驗證）
└─ points        積分餘額（>= 0）
（實體 DDL 在 SD §4）
```
