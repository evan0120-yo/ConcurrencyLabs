# SA.md — s28_loginspike（登入尖峰系統）

> 事實文件（SASD）· 結構化分析。描述「登入尖峰要做什麼」的**業務行為**，平台無關（P1..Pn 共用；各平台怎麼扛量在各自 SD）。
> 特性：尖峰洪流混著攻擊（暴力破解 / 撞庫），要**擋壞流量又不拖慢、不誤傷正常登入**。

## 1. Context Diagram

```text
[使用者 / 攻擊者] --(登入: account, pwd, ip, device)--> (登入) --(session / DENY / CAPTCHA)--> [端]

(登入) --(限頻 / 異常偵測)--> [util.redis]
(登入) --(驗帳密)----------> [PostgreSQL user]
(登入) --(發 / 查 session)--> [session store]
```

## 2. DFD（精簡 box flow）

```text
[登入請求] --> [限流 / 風控守門]（per account / per IP）
   ├─(超頻 / 可疑: 撞庫特徵)──> 要求 CAPTCHA / 限頻 / 擋
   │(正常)
   ▼
[驗帳密] --(比對)--> [PostgreSQL]
   ├─(失敗)──> 計失敗次數（達門檻升級防護）
   └─(成功)
   ▼
[發 session] --> [session store] ──> 登入成功
```

## 3. Process Spec（行為基準，decision table）

```text
[限流 / 守門]（尖峰洪流第一關）
├─ per account / per IP 超頻 → 限頻 / CAPTCHA
└─ 撞庫 / 暴力破解特徵（大量帳號同 IP、大量失敗）→ 升級防護

[驗帳密]
├─ 成功 → 發 session
└─ 失敗 → 累計失敗次數；連續失敗達門檻 → 鎖定 / CAPTCHA

[不誤傷]（核心取捨）
└─ 防護分級：先 CAPTCHA / 限頻，非直接封鎖；避免把正常人擋在門外

熱點：熱門帳號、攻擊來源 IP → 需分散承接（見各平台 SD）。
```

## 4. State Transition（登入防護狀態）

```text
【正常】──(失敗累積/可疑)──> 【CHALLENGED（CAPTCHA/限頻）】──(持續惡意)──> 【LOCKED / BLOCKED】
```

## 5. Data Dictionary（邏輯欄位；實體 DDL 在各平台 SD）

```text
login_attempt（登入嘗試計數，活在 Redis）
├─ key（account / ip）/ failCount / windowStart
└─ state（NORMAL|CHALLENGED|LOCKED）

session（登入態）
├─ sessionId(PK) / userId / createdAt / expireAt
└─（帳密驗證走 user 表；session 見 s29）
```
