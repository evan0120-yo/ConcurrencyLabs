# 專案目的與思考框架（每個接手的 AI 先讀這段）

> 讀完這段，你要自動站到一個角度思考：
> **在真實系統裡，工程師如何判斷「下一次大改要動哪一包架構」，以及改完後怎麼估「這套平台能撐到哪」。**
> 這個 lab 練的是這種判斷力，不是背技術名詞。

## 先看一個具體場景：S01 抽獎，現在 3,000 QPS 很穩

```text
【S01 抽獎系統 · 目前站在 Platform A】
├─ 現況：約 3,000 QPS，線上很穩
├─ 已做過的第一輪改造
│   ├─ hot path 的 JOIN 拆了
│   ├─ [Redis]   原子扣庫存（Lua 判空+扣減）
│   ├─ idempotency 去重（SET NX）
│   └─ [DB]      該建的 index 建了
└─ 監控開始亮紅燈（接近天花板的訊號）
    ├─ [DB]      draw_record insert 的 P99 開始飆
    ├─ [Redis]   stock key 出現熱點
    └─ [account] service 的 P99 被拖慢
```

此刻工程師腦中跑的**不是**「我要 20,000 QPS」，而是這四個問題：

```text
├─ 這套架構還能撐多久？
├─ 下一個會爆的是哪一塊？
├─ 如果要大改，這次該動哪幾個大方塊？
└─ 改完大概能站到哪個容量平台？
```

## 最容易搞錯的一件事：QPS 不是主詞

```text
────────── ❌ 錯的思考（QPS 帶路）──────────
「我要 20,000 QPS，所以架構要怎麼設計？」
   └─ 先訂數字、再湊架構 —— 像背考古題

────────── ✅ 對的思考（瓶頸帶路）──────────
「這包架構在 3,000 QPS 很穩，
  但 draw_record / stock key / account P99 接近危險區，
  這次我改哪些大方塊？改完估計能站到哪？」
   └─ QPS 是『改造完才算出來的容量估算』，不是目標
```

一句話收成通則：

```text
切分點   = 架構平台版本（Platform A / B / C …）   ← 主詞
QPS      = 這個平台改造完的容量估算               ← 結果，不是原因
改造內容 = 這一版實際動了哪幾個大方塊             ← 內容
```

## 第二個坑：不是「全部叢集化」

叢集不是免費的，一升級就綁進一整包代價：

```text
上叢集 / 分散式的代價
├─ 成本上升        多機器 / 授權 / 流量費
├─ 維運複雜度上升  部署 / 監控 / 擴縮容
├─ 一致性問題      分片 / 複寫延遲
├─ debug 變難      問題要跨節點追
└─ failover 行為   主從切換 / 腦裂
```

所以決策規則是：

```text
一次 checkpoint = 一包架構改造
└─ 但每個大方塊要不要動 → 只看它是不是『下一個瓶頸』
   ├─ 是下一個瓶頸 → 這次動它
   └─ 還沒到天花板 → 這次不動，並寫進「沒改的大方塊 ＋ 為何現在不用更重方案」
```

S01 從 Platform A 跳到 Platform B 的具體示範（重點：**不是全上叢集**）：

```text
【S01 · Platform A ── 一次改造 ──▶ Platform B】
├─ 這次動的大方塊
│   ├─ [Redis]   stock key 分桶（打散熱點）
│   ├─ [DB]      draw_record 改 outbox / 批次寫（削 insert 壓力）
│   └─ [account] 呼叫加 timeout + bulkhead（隔離慢依賴）
├─ 只是配套（不是主角）
│   └─ app replica 加幾台
├─ 這次『沒』動的大方塊 ＋ 為何
│   └─ [DB] 沒上讀寫分離：目前讀壓力還沒到，硬上只是徒增一致性複雜度
└─ 改完才估算容量：約 10,000 QPS
```

## 每個「架構平台版本」都用這個模板寫

```text
Platform X
├─ 當前問題      哪個指標最接近天花板（附真實 metric）
├─ 改造內容    這次動了哪幾個大方塊
├─ 沒改的塊    為什麼現在不用更重方案
├─ 估算容量      改完大概站到多少 QPS
└─ 驗證指標      用哪些指標證明真的站上去了
```

> 所以本專案的 HTML／教學主線，**不叫**「不同 QPS 下要看什麼」，
> 而叫 **「容量平台演進」**：讀者看到的是工程師如何一包一包把系統推上下一個容量平台。

## 讀完這段，你（AI）該有的反射

```text
不要問  「這需要多少 QPS？」
要問    「現在哪個大方塊最接近天花板？
         這次改哪一包？改完估到哪？哪些先不動、為什麼？」
```

---
# DEVELOPMENT.md — ConcurrencyLabs 開發規範

> 跨情境的穩定開發規範書（所有情境共同遵守的硬規則）。
> 事實文件版本：**SASD**（規格階段產出 `SA.md` + `SD.md`）。
> 架構平台版本（Platform）：情境內以 **P1 / P2 / … / Pn 子資料夾** 表示（Pn = 第 n 個架構平台；P1 = QPS 3,000+ 基線；平台數量依情境分析而定，見第一節）。

---

## 一、開發協作流程

### 事實文件版本宣告

```text
本專案事實文件版本 = SASD
└─ 規格階段產出：SA.md + SD.md
   ├─ 事實文件是「per 模組」的：不只 75 情境，util、common 底下的
   │  共用子模組（如 util/redis、common/account）也各自有 SA.md / SD.md
   ├─ 放在「該模組的子資料夾內」，與 README.md 同層
   └─ 【不產出】BDD.md / SDD.md / TDD.md / PLAN.md
      （背景/人群/規模估計併入 SD.md §0；行為驗證基準用 SA 的 Process Spec）
```

### 兩軸正交（本專案特性）

```text
package    = 情境          com.concurrencylabs.<scenario>（75 個 flat）
P 子資料夾 = 架構平台版本   <scenario>/P1、P2 … Pn（同一情境內並存，可並排對照）

【切分依據 = 架構平台，不是 QPS】
├─ 起點：每個情境 user story 一律先假設 QPS 3,000+ = P1（基線平台）
├─ 往上爬：AI 分析這情境會經過幾個「容量平台（Platform）」，每個平台 = 一次架構改造
│          = 一組因互相依賴、必須同時改的『大方塊』；每個平台附「為何這些要綁一起」
├─ 數量不固定：顆粒度依情境的壓力模型 / 一致性可放寬程度而定
│              業務越能容忍不精確 → 連鎖越少 → 包越少（有些 3 包、有些 8 包）
└─ QPS 是『果』：某平台改造完才估算它能站到多少 QPS，不是先訂 QPS 再湊架構
```

### 【強制 4 階段】不可省略、不可合併、不可換順序

```text
討論階段：與開發者對齊「某情境 × 某架構平台 Pn」的需求與壓力模型
       │
       ▼ (開發者下達指令："開始寫文件")
【規格階段：撰寫事實文件（SASD）】
(情境 root 產出 SA.md〔業務行為，平台無關，一份〕+ README〔user story + 容量平台演進地圖〕；
 每個架構平台於 <scenario>/Pn/ 產出該平台 SD.md)
├─ SA.md（root，一份共用）：Context / DFD / Process Spec / State / Data Dictionary
└─ Pn/SD.md（每平台一份，Code 階段才寫）：§0 背景+規模估計 / 架構+Structure Chart / Table Schema / API Schema / NFR + 該平台改造明細
       │
       ▼ (開發者審閱通過 SA / SD)
【實作階段：依規編碼與驗證】
(嚴格遵循 SA / SD 編寫原始碼，執行閉環驗證：Code Review + code↔SA/SD 同步 + mvn compile)
└─ 閉環 against 事實文件：code vs SA + SD（以 SA 的 Process Spec 當行為基準）
       │
       ▼ (開發者下達指令："好了")
【審查階段：程式碼審查與封裝確認】
(撰寫/更新 CODE_REVIEW.md、TECH_SUPPLEMENT.md，完成文件同步)
```

**原則**：

- **必須是 4 階段**（討論 → 規格 → 實作 → 審查），不可只寫 3 或 5。
- 規格階段必須**明文產出 `SA.md` + `SD.md`**（SASD 版本），不可用「規格文件」帶過；本專案不產 BDD/SDD/TDD/PLAN。
- 實作階段必須**明寫閉環驗證 against SA + SD**（Code Review + code↔SA/SD 同步 + `mvn compile`，見第四節）。
- 審查階段必須**與 code sync**（CODE_REVIEW 是目前實作真相，不是 aspirational 設計）。

---

## 二、N-Layer 架構職責（4 層）

每個平台（`<scenario>/Pn`）內部一律遵守同一套四層，跨 75 情境、跨所有平台一致。

* **Controller (控制層)**：接收 HTTP 請求，僅處理路由與 `*Req` / `*Resp` 轉換，並直接呼叫對應 Usecase。**【嚴格禁止】包含任何業務邏輯，或直接呼叫 Service / Repository。** 合法方向：`Controller → Usecase`。
* **Usecase (用例層)**：**核心流程編排者**。必須與 SA 的 DFD、SD 的 Structure Chart 對齊；負責協調呼叫不同 Service 完成業務流程。**【嚴格禁止】直接注入與呼叫 Repository，所有持久化與外部存取必須經由 Service 執行。** 合法方向：`Usecase → Service`。
* **Service (服務層)**：**領域與持久化邏輯實作者**。包含無狀態領域邏輯、計算與併發規則（呼叫 `util` 做 Redis 原子操作 / MQ、決定冪等與扣減時機），以及所有與 Repository 交互的業務邏輯；跨情境的共用能力向 `util` / `common` 借用。**【嚴格禁止】被 Controller 越位直接呼叫。** 合法方向：`Service → Repository`、`Service → util / common`。
* **Repository (倉儲層)**：專注**該情境自己的領域資料持久化**（PostgreSQL / JPA），保持乾淨、無業務編排。**【嚴格禁止】被 Controller 或 Usecase 越位直接呼叫；亦【嚴格禁止】在此封裝 Redis / MQ（那是基礎設施，一律走 `util`）。**

> 跨情境的東西不放進任一情境的 Repository：
> ├─ `util`（無狀態基礎件）  Redis 原子操作、MQ client → Service 直接用
> └─ `common`（有狀態共用小模組）身分 / 積分（account）等 → 由 Usecase / Service 呼叫

---

## 三、物件劃分與封裝定義

### Package 結構

```text
com.concurrencylabs
├── util                         （跨情境「無狀態基礎件」；少數特例，平行於情境）
│   └── redis                    （RedisUtil：原子操作 / Lua）＋ 自己的 SA.md / SD.md
├── common                       （跨情境「有狀態共用小模組」；平行於情境）
│   └── account                  （User + 積分；無 auth，直接傳 userId）＋ 自己的 SA.md / SD.md
└── <scenario>                   （一個情境一個 package，共 75 個 flat）
    ├── README.md                （user story + 痛點 + 容量平台演進地圖：幾個平台 / 每平台改哪些大方塊 / 容量估算）
    ├── SA.md                    （事實文件-分析；業務行為，平台無關，一份共用）
    ├── <scenario>.html          （教學頁：容量平台演進；第一階段先寫）
    ├── package-info.java        （情境佔位 + 一行說明）
    └── P1 / P2 / … / Pn         （★ 架構平台；每平台【完全自足】，看某 Pn 不用跳出去。第一階段頂多開空 package，下列內容 Code 階段才寫）
        ├── controller           （該平台 HTTP 入口：*Req dto 進、*Resp dto 出）
        ├── usecase              （該平台流程編排＝對齊該平台 SD Sequence；見第六節）
        ├── service              （該平台領域邏輯 + 併發編排 + 守門；隨架構升級而不同）
        ├── repository           （該平台 DB 持久化，不含 Redis / MQ）
        ├── model                （該平台 JPA Entity；各平台各一份，分表等差異直接落自己這份）
        ├── enums                （該平台 enum）
        ├── object
        │   ├── dto              （該平台邊界資料：Controller HTTP + 第三方；Lombok class）
        │   └── bo               （該平台純內部業務載體；Lombok class）
        ├── error                （ErrorCode enum + Exception + Handler 綁一起）
        └── SD.md                （事實文件-設計；該平台架構 + Structure Chart + Table/API Schema + 該平台改造明細）
```

> 各平台【完全自足】：model / enums / dto / bo / error / 四層，每個 Pn 各自一套，刻意不共用 root——
> 目的是「看 P2 就只看 P2」，朋友 / 面試對照時不用在平台間跳；代價是 domain 會重複，接受。
> SA.md 例外：業務行為平台無關，只在 root 放一份共用（不進 Pn）。
>
> 【資訊都在 root，不往 Pn 寫】平台演進的所有 info（幾個平台、每平台改哪些大方塊、容量估算）
> 一律寫在情境 root 的 README / SA / HTML——尤其 HTML 要能直接畫出多平台的架構變化。
> 第一階段 Pn 資料夾【頂多開好空 package】，不放任何 doc / code。
>
> 產生順序（分階段）：
> ├─ 第一階段：寫滿情境 root 的 README + SA + HTML（涵蓋 75 情境）；Pn 至多開空 package
> └─ Code 階段：真要看某情境 code 時，才在其 Pn 內寫 controller…/SD.md（自足），按需生成

> `util` 與 `common` 是刻意的少數特例（平行於 75 情境）：
> 前者是無狀態工具、後者是有自己資料表的共用小模組；兩者都各自產 SA.md / SD.md。

### 物件類型表

| 物件類型 | 所屬 Package | 主要職責 | 套用場景 / 限制 |
|:---|:---|:---|:---|
| **Entity** | `<scenario>.Pn.model` | 映射 RDBMS 實體資料表 | **此 package 僅放 Entity，嚴禁放 enum / DTO / BO**；各平台各一份 |
| **Enum** | `<scenario>.Pn.enums` | 該情境列舉型別（狀態等） | 平行於 model；【不能】命名為 `enum`（Java 保留字） |
| **DTO** | `<scenario>.Pn.object.dto` | 邊界資料：Controller HTTP + 第三方呼叫 | 進入必為 `*Req`、輸出必為 `*Resp`（方向留在名字） |
| **BO** | `<scenario>.Pn.object.bo` | 純內部業務載體 | 僅用於工廠輸出、Usecase / Service 處理主體 |

> 一律用 **Lombok class，不用 `record`**（非 DDD 專案，且要與 75 情境保持一致；避免「bo 包裡塞 record」的名實不符）。
> `ErrorCode` 這類「與 Exception / Handler 綁定」的 enum 留在 `error` package，不散到 `enums`。

**原則**：

- 每個 package 職責**單一**，嚴禁混用。
- 兩軸正交：**package 決定「哪個情境」、P 子資料夾決定「哪個架構平台版本」**；同一情境在 P1…Pn 各有一版 SD + 該平台 code（SA 一份共用，見第一節）。

---

## 四、驗證政策（閉環驗證為主，不強制寫測試）

本專案以「架構正確」為主，**不強制寫單元測試**。每次實作的驗證靠「閉環驗證」，不靠測試覆蓋率。

**閉環驗證（每次編碼後必做）**：

```text
Step 1  Code Review 逐檔     語法 / 編譯錯誤、邏輯錯誤、命名風格、遺漏 edge case、dead code
Step 2  code ↔ SA/SD 同步    以 SA 的 Process Spec 當行為基準；SD 的架構 / Table Schema / API 對齊
Step 3  能編譯               mvn compile（EXIT=0）
發現問題 → 修正 → 重跑整個 loop，直到連續一輪零問題
```

* SASD 模式閉環比對對象 = **SA + SD**：`code vs SA`（行為對齊 Process Spec）、`code vs SD`（架構 / Table Schema / API 對齊）。

**測試（選配，不是每次必須）**：

```text
只有「想實際跑起來證明行為」時才寫，非強制：
├─ 單元測試   Mock 掉 Service / Repository / 外部中介，驗編排與併發規則（防重 / 不超賣 / 冪等）
└─ 整合測試   接 docker-compose.yml 起的真服務（Postgres / Redis / Kafka / RocketMQ），不 Mock
要跑時的命令：mvn test（目前尚無 mvnw wrapper，需要時再補）
```

---

## 五、並發設計不變量（多台 server）

本專案所有情境一律假設**正式部署 = 多台無狀態 app server**（LB 後方多實例）。因此：

```text
├─ app server 無狀態
│   └─ 【嚴格禁止】把並發正確性放在本地變數 / synchronized / 本地鎖
│
├─ 並發正確性一律壓在『共享儲存』的原子操作
│   ├─ Redis：原子指令 / Lua（判空+扣減、SET NX 去重、原子計數）
│   └─ DB：原子條件更新（UPDATE ... WHERE 條件；行鎖）
│
├─ 有原子原語就用原子操作，不要反射性上分散式鎖
│   └─ 只有「多步 + 跨多 key + 無法塞進一段原子操作」的臨界區才用分散式鎖
│      且優先 per-key / per-user 範圍，避免全域爭用
│
└─ 跨儲存（DB ↔ Redis）無法一個 ACID 交易
    └─ 用「補償（失敗回滾）」+「idempotency 去重」保證最終正確、且只發生一次
```

---

## 六、Coding Style 細則（分層寫法）

**Usecase = 對齊流程圖（一個 `//` 註解 = 一行 service 呼叫）**：

```text
├─ Usecase 概念要 mapping SA DFD / SD Sequence
├─ 每個步驟一個 // 註解，底下只放『一行』service 呼叫
├─ 這裡「一行」不含 if / for / throw / try 這類控制流（控制流可保留當骨架）
└─ 出現多行商業邏輯 = 放錯層 → 該下沉到 Service（Usecase 不寫商業邏輯本體）
```

**驗證 / 守門集中在 GuardService**：

```text
├─ 「檢查不合法就 throw」的前置守門，集中到 <scenario>GuardService
│   （例：活動狀態 / 時間窗、積分是否足夠）
└─ Usecase 每個守門只呈現一行：guardService.checkXxx(...)
```

**@Transactional 邊界**（不是「一律加 usecase」，也不是「一律加 service」）：

```text
├─ 一般：多個『同一 DB』的寫入要一起成敗 → @Transactional 放在該 unit of work 層（通常 Usecase）
├─ 跨儲存 saga（Redis + DB 等，無法一個 ACID 交易）
│   └─ Usecase 【不加】@Transactional；各自獨立提交的 DB 步驟由 Service 方法自帶（補償才做得到）
├─ 單一 read / 單一 save → 【不需】額外 @Transactional（Spring Data 已內建）
└─ @Modifying 的更新語句 → 【必須】在交易中（其 Service 方法要 @Transactional）
```

**物件型別**：一律 Lombok class、不用 `record`；enum 放 `enums` package、不放 `model`（見第三節）。

---

**文件版本**：v1
**事實文件版本**：SASD（SA.md + SD.md）
**作者**：Claude Opus 4.8
