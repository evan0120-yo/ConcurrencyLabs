# SA.md — util/redis（Redis 基礎件）

> 事實文件（SASD）· 結構化分析。這是**無狀態基礎件**，沒有業務規則，SA 刻意很薄。

## 1. Context Diagram

```text
[各情境 Service] --(原子指令 / Lua 腳本)--> (RedisUtil) --(結果)--> [各情境 Service]
(RedisUtil) --(封裝呼叫)--> [共享 Redis]
```

## 2. DFD

```text
[Service] --(op + key + args)--> [RedisUtil]
   ├─ 一般指令 → GET / SET / SETNX / INCR / DECR / EXPIRE
   └─ 複合原子 → EVAL(Lua script)  ← 判空+扣減、判上限+計數 等
        │
        ▼
   [共享 Redis] 執行（單執行緒序列化）
        │
        ▼
   回結果給 Service
```

## 3. Process Spec（行為基準）

```text
[RedisUtil 職責邊界]
├─ 只做「Redis 存取的薄封裝」：把 key/args 傳進去、把結果傳回來
├─ 提供的原子能力（不自己實作業務判斷）：
│   ├─ setIfAbsent(key, val, ttl)   → SET NX EX（一次性 claim / 去重）
│   ├─ incr / decr(key)             → 原子增減
│   └─ eval(script, keys, args)     → 執行 Lua（複合原子操作）
└─ 【嚴禁】在此寫任何情境業務規則（中獎率、限次數字…那是情境 Service 的事）

邊界
├─ 原子性由 Redis 保證（單執行緒 / Lua 內序列化），非本工具自行加鎖
└─ 多台 app server 共享同一 Redis → 原子性天生跨 server（見 DEVELOPMENT §五）
```

## 4. State Transition

無（無狀態工具）。

## 5. Data Dictionary（邏輯）

```text
本工具不擁有資料；key/value 的語意由呼叫端情境定義。
（實體上就是 Redis key-value，無 DB schema）
```
