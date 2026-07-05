# SD.md — util/redis（Redis 基礎件）@ L1

> 事實文件（SASD）· 結構化設計。無狀態基礎件，正式交付級但天生輕薄。

## 0. 背景與範圍

```text
目的
└─ 提供跨情境共用的「Redis 原子操作」薄封裝，讓情境 Service 不必各自重寫 Redis client

使用對象（呼叫者）
└─ 各情境的 Service 層（如 s01_lottery 的 LotteryDrawService）

規模與未來估計（L1）
├─ 承接所有情境的 Redis 存取；L1 單一 Redis 實例
└─ 非目標：Redis 叢集 / 分片治理（留高 L 或 cloud-platform 情境研究）
```

## 1. 架構總覽 + Structure Chart

```text
┌──────────────┐   呼叫    ┌───────────────┐   封裝   ┌──────────────┐
│ 情境 Service │ ───────▶ │  RedisUtil    │ ──────▶ │ Redis (共享) │
└──────────────┘          │ (Component)   │         └──────────────┘
                          └───────────────┘

RedisUtil
├─ setIfAbsent / get / set / expire
├─ incr / decr
└─ eval(script, keys, args)      ← Lua 複合原子
```

## 2. Module Decomposition

```text
RedisUtil   Spring @Component，注入 StringRedisTemplate / RedisTemplate，提供原子操作方法
```

## 3. Coupling & Cohesion + 依賴方向

```text
allowed    情境 Service → RedisUtil → RedisTemplate
must-not   RedisUtil ─X→ 任何情境的業務類（不可反向依賴情境）
           RedisUtil 內【嚴禁】情境業務規則
```

## 4. 資料模型 / Table Schema

```text
無 DB 表（Redis key-value，語意由呼叫端定義）。
```

## 5. API 設計 / Schema（對內方法）

```text
boolean setIfAbsent(String key, String val, Duration ttl)   // SET NX EX
Long    incr(String key)      /  Long decr(String key)
<T> T   eval(String luaScript, List<String> keys, Object... args)
String  get(String key)  /  void set(String key, String val, Duration ttl)
```

## 6. 關鍵流程 Sequence

```text
Service 要「原子判空+扣減」
 → RedisUtil.eval(扣減腳本, [stockKey], [])
 → Redis 執行 Lua（原子）→ 回 1(扣成功)/0(空)
```

## 7. 非功能需求（NFR）

```text
├─ 原子性：由 Redis 保證；RedisUtil 不自行加鎖
├─ 多台 server：共享同一 Redis，原子性跨 server 成立
└─ 低延遲：單次往返；Lua 把多步壓成一次 round-trip
```

## 8. 相依與外部整合

```text
└─ 共享 Redis（Spring Data Redis / Lettuce）
```
