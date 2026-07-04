# ConcurrencyLabs

> 一本查得到的「高併發架構解法字典」。收錄 75 個常見高併發情境，當你要設計（或面試遇到）某個情境時，可以快速翻到「這種情境、這種流量下，架構大概怎麼做」。

---

## 這個專案想幫你解決什麼

假設你接到一個需求，或面試被問：

> 「做一個**秒殺**，怕超賣，你怎麼設計？」

你心裡大概知道要用 Redis、要削峰，但一時講不清楚「3,000 人搶跟 10 萬人搶，架構差在哪」。

這個專案就是為了這種時刻——**把 75 個高併發情境，各自在不同流量等級下的架構解法，整理成可以快速查閱的參考實作**。你不用從零想，翻到對應的情境就有方向。

> 注意：這是**設計層的參考**，目的是「快速找到對的解法方向」，不是拿來跑真實壓力測試的產品系統。

---

## 專案怎麼組織的

```text
兩個維度交叉，就能定位到「某情境 × 某流量」的解法
│
├─ 情境 → 用 package 切（75 個，攤平放在 com.concurrencylabs.<情境>）
│         例：s13_flashsale（秒殺）、s06_gamewalletledger（錢包帳本）、s55_feed（動態牆）
│
└─ 流量等級 → 用 branch 切（L1 / L2 / L3 / L4）
              同一個情境，在不同 branch 展示「流量長大後，架構怎麼演進」
```

流量等級是一把共用的尺：

```text
L1  ~3,000 QPS      履歷起點等級
L2  ~10,000 QPS     單純加機器不夠，開始要削峰 / 快取 / 非同步
L3  ~50,000 QPS     熱點、分片、MQ 堆積等問題明顯浮現
L4  100,000 QPS+    尖峰洪水，要隔離資源、降級、近似值、預計算
```

> ⚠ 讀的時候別把等級當成硬規定。它是「壓力大概到哪」的參考，不是「到這個數字一定要做某件事」。
> 有些情境（搶資源、熱點）在 3,000 就得上 Redis；有些情境讀少寫少，到 5 萬還只靠一個 DB index 就夠。
> 看**那個情境自己**的判斷，別套公式。

---

## 我要怎麼用它？（實際走一遍）

以「我要做秒殺，怕超賣」為例，四步：

```text
Step 1  打開 ROUTE.md，用「症狀」找情境
        └─ 找到這行：「限量商品會被搶爆、怕超賣」→ s13_flashsale / s14_couponclaim / s15_inventorydeduction

Step 2  進到那個 package，讀它的 README
        └─ src/main/java/com/concurrencylabs/s13_flashsale/README.md
           看 user story 跟痛點，確認「對，這就是我的情境」

Step 3  切到你要的流量等級 branch
        └─ 你預期 5 萬 QPS？切到 L3 branch，看 s13_flashsale 在那個量級的做法

Step 4  照著那份參考實作的結構，套回你自己的專案
```

`ROUTE.md` 是導覽核心，它用**五種鏡頭**幫你找情境：

```text
ROUTE.md 的五種找法
├─ 鏡頭 0  情境 ↔ package 對照總表（查名字）
├─ 鏡頭 1  by 業務 domain（我做博弈 / 電商 / 交易所…）
├─ 鏡頭 2  by 壓力模型（搶資源 / 熱點 / 讀爆 / 寫爆 / 低延遲…）
├─ 鏡頭 3  by 數據角色（ingestion / 分析查詢）
├─ 鏡頭 4  by 症狀（← 最常用：白話描述你的狀況，直接對到 package）
└─ 鏡頭 5  相似 / 對照組（學一個順便理解另一個）
```

**最簡單的用法**：把你的狀況白話講給 AI，讓它讀 `ROUTE.md` 幫你對應到該看哪個 package。

---

## 怎麼讀單一個情境

每個 package 裡有一份 `README.md`，固定三段，不含技術細節，先讓你搞懂「這是什麼問題」：

```text
package/README.md 的結構
├─ Block 1  這個情境同時符合哪些分類（domain / 壓力模型 / 數據角色）
├─ Block 2  為什麼被歸到這些分類
└─ Block 3  User Story（一張圖）+ 痛點分析
            └─ 用「平常 vs 尖峰」對比，講清楚痛在哪、什麼能丟 / 能延遲 / 要強一致
```

（`package-info.java` 只是佔位 + 一行情境說明，實際內容看 `README.md`。）

---

## 這個專案會用到哪些技術

心裡有個底就好，實際哪個情境用到哪個，看那個情境的解法：

```text
幾乎都會出現
├─ PostgreSQL   需要「對得起帳」的資料（訂單 / 帳本 / 交易狀態）
└─ Redis        快取、熱點計數、分布式鎖

流量上來後按需登場
├─ Kafka        資料流 / 事件 / 大量寫入削峰（可重放）
├─ RocketMQ     任務 / 延遲 / 順序 / 事務訊息
└─ NoSQL        某類資料壓力大到不適合關聯式時才導入
                （寫爆→Cassandra 類、分析→ClickHouse 類、彈性文件→Mongo 類）
```

---

## 檔案地圖

```text
ConcurrencyLabs/
├─ README.md                              ← 你正在看的（專案總覽 + 上手）
├─ ROUTE.md                               遇到狀況 → 該看哪個 package（導覽索引）
├─ 75_high_concurrency_catalog_by_package.md   每個情境的 QPS 斷點速查表
├─ docker-compose.yml                     本機基礎設施
└─ src/main/java/com/concurrencylabs/
   └─ <情境>/
      ├─ README.md                        該情境的 user story + 痛點
      └─ package-info.java                佔位 + 一行說明
```

---

## 怎麼跑起來

### 需要什麼

- Docker Desktop
- JDK 21
- Maven（或直接用 IDE 跑）

### 1. 啟動基礎設施

核心服務（Postgres + Redis + Kafka + RocketMQ）：

```
docker compose up -d
```

需要 NoSQL 時再加開（Mongo / Cassandra / ClickHouse，預設不啟動）：

```
docker compose --profile nosql up -d
```

### 2. 啟動應用程式

用 IDE 直接跑 `ConcurrencyLabsApplication`，或：

```
mvn spring-boot:run
```

> 目前各情境只有佔位與文件、還沒有 entity，所以 `application.yml` 的 JPA `ddl-auto` 設為 `none`，
> 讓 app 在沒有資料表時也能正常啟動。

### 連線資訊（本機預設）

| 服務 | 位址 | 備註 |
|---|---|---|
| PostgreSQL | `localhost:5432` | db=`concurrency_labs`，帳密都是 `postgres` |
| Redis | `localhost:6379` | 無密碼 |
| Kafka | `localhost:9092` | KRaft 單節點 |
| RocketMQ NameServer | `localhost:9876` | |
| RocketMQ Broker | `localhost:10911` | |
| MongoDB（需 `--profile nosql`） | `localhost:27017` | |
| Cassandra（需 `--profile nosql`） | `localhost:9042` | |
| ClickHouse（需 `--profile nosql`） | `localhost:8123` / `9000` | |

### 關閉

```
docker compose down
```

想連同資料一起清乾淨、重來一份：

```
docker compose down -v
```
