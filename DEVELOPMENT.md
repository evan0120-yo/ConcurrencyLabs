# DEVELOPMENT.md — ConcurrencyLabs 開發規範

> 跨情境的穩定開發規範書（所有情境共同遵守的硬規則）。
> 事實文件版本：**SASD**（規格階段產出 `SA.md` + `SD.md`）。
> 當前分支 QPS 等級：**L1（~3,000 QPS）**（此等級隨 branch 切換而改，見第一節）。

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
package = 情境      com.concurrencylabs.<scenario>（75 個 flat）
branch  = QPS 等級  L1 / L2 / L3 / L4

當前分支：L1
└─ QPS 目標：~3,000（起點級：水平擴展 + Redis + MQ + DB index 開始有感）
   ⚠ 切換到 L2 / L3 / L4 branch 時，回來改這一段的「當前分支」與「QPS 目標」
      → 四個分支各自帶不同 QPS 等級描述，其餘規範（二、三、四節）共用
```

### 【強制 4 階段】不可省略、不可合併、不可換順序

```text
討論階段：與開發者對齊「某情境 × 當前 L」的需求與壓力模型
       │
       ▼ (開發者下達指令："開始寫文件")
【規格階段：撰寫事實文件（SASD）】
(在該情境子資料夾產出 SA.md + SD.md)
├─ SA.md：Context / DFD / Process Spec / State / Data Dictionary
└─ SD.md：§0 背景+使用對象+規模估計 / 架構+Structure Chart / Table Schema / API Schema / NFR
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

每個情境 package 內部一律遵守同一套四層，跨 75 情境一致。

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
    ├── controller               （僅 HTTP 入口：*Req dto 進、*Resp dto 出）
    ├── usecase                  （流程編排＝對齊流程圖，不碰 Repository；見第六節）
    ├── service                  （領域業務邏輯 + 併發編排 + 守門；向 util / common 借共用能力）
    ├── repository               （只封裝該情境自己的 DB 持久化，不含 Redis / MQ）
    ├── model                    （僅存放 JPA / Database Entity；嚴禁放 enum / 其他）
    ├── enums                    （該情境的 enum；平行於 model，不放進 model）
    ├── object
    │   ├── dto                  （邊界資料：Controller HTTP + 第三方呼叫；Lombok class）
    │   └── bo                   （純內部業務載體：工廠輸出、Usecase 處理主體；Lombok class）
    ├── error                    （ErrorCode enum + Exception + Handler 綁一起）
    ├── README.md                （情境 user story + 痛點）
    ├── SA.md / SD.md            （事實文件，SASD；當前 branch 的 QPS 等級版本）
    └── package-info.java        （情境佔位 + 一行說明）
```

> `util` 與 `common` 是刻意的少數特例（平行於 75 情境）：
> 前者是無狀態工具、後者是有自己資料表的共用小模組；兩者都各自產 SA.md / SD.md。

### 物件類型表

| 物件類型 | 所屬 Package | 主要職責 | 套用場景 / 限制 |
|:---|:---|:---|:---|
| **Entity** | `<scenario>.model` | 映射 RDBMS 實體資料表 | **此 package 僅放 Entity，嚴禁放 enum / DTO / BO** |
| **Enum** | `<scenario>.enums` | 該情境列舉型別（狀態等） | 平行於 model；【不能】命名為 `enum`（Java 保留字） |
| **DTO** | `<scenario>.object.dto` | 邊界資料：Controller HTTP + 第三方呼叫 | 進入必為 `*Req`、輸出必為 `*Resp`（方向留在名字） |
| **BO** | `<scenario>.object.bo` | 純內部業務載體 | 僅用於工廠輸出、Usecase / Service 處理主體 |

> 一律用 **Lombok class，不用 `record`**（非 DDD 專案，且要與 75 情境保持一致；避免「bo 包裡塞 record」的名實不符）。
> `ErrorCode` 這類「與 Exception / Handler 綁定」的 enum 留在 `error` package，不散到 `enums`。

**原則**：

- 每個 package 職責**單一**，嚴禁混用。
- 兩軸正交：**package 決定「哪個情境」、branch 決定「哪個 QPS 等級」**；同一情境在 L1~L4 branch 各有一版 SA/SD + code。

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

**文件版本**：v1（當前分支 L1）
**事實文件版本**：SASD（SA.md + SD.md）
**作者**：Claude Opus 4.8
