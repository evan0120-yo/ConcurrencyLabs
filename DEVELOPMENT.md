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
   ├─ 放在「該情境的子資料夾內」，與 README.md 同層
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
(嚴格遵循 SA / SD 編寫原始碼，並執行 mvn test 通過閉環驗證)
└─ 閉環 against 事實文件：code vs SA + SD（以 SA 的 Process Spec 當行為基準）
       │
       ▼ (開發者下達指令："好了")
【審查階段：程式碼審查與封裝確認】
(撰寫/更新 CODE_REVIEW.md、TECH_SUPPLEMENT.md，完成文件同步)
```

**原則**：

- **必須是 4 階段**（討論 → 規格 → 實作 → 審查），不可只寫 3 或 5。
- 規格階段必須**明文產出 `SA.md` + `SD.md`**（SASD 版本），不可用「規格文件」帶過；本專案不產 BDD/SDD/TDD/PLAN。
- 實作階段必須**明寫閉環驗證 against SA + SD** + 實際命令 `mvn test`（見第四節）。
- 審查階段必須**與 code sync**（CODE_REVIEW 是目前實作真相，不是 aspirational 設計）。

---

## 二、N-Layer 架構職責（4 層）

每個情境 package 內部一律遵守同一套四層，跨 75 情境一致。

* **Controller (控制層)**：接收 HTTP 請求，僅處理路由與 `*Req` / `*Resp` 轉換，並直接呼叫對應 Usecase。**【嚴格禁止】包含任何業務邏輯，或直接呼叫 Service / Repository。** 合法方向：`Controller → Usecase`。
* **Usecase (用例層)**：**核心流程編排者**。必須與 SA 的 DFD、SD 的 Structure Chart 對齊；負責協調呼叫不同 Service 完成業務流程。**【嚴格禁止】直接注入與呼叫 Repository，所有持久化與外部存取必須經由 Service 執行。** 合法方向：`Usecase → Service`。
* **Service (服務層)**：**領域與持久化邏輯實作者**。包含無狀態領域邏輯、計算與併發規則（如 Redis 預扣 / 分布式鎖 / 計數、MQ 發送時機、冪等判斷），以及所有與 Repository 交互的業務邏輯。**【嚴格禁止】被 Controller 越位直接呼叫。** 合法方向：`Service → Repository`。
* **Repository (倉儲層)**：專注資料與中介存取（PostgreSQL / JPA、Redis、Kafka / RocketMQ client 的最底層存取封裝），保持乾淨、無業務編排。**【嚴格禁止】被 Controller 或 Usecase 越位直接呼叫。**

---

## 三、物件劃分與封裝定義

### Package 結構

```text
com.concurrencylabs
└── <scenario>                    （一個情境一個 package，共 75 個 flat）
    ├── controller                （僅 HTTP 入口：*Req 進、*Resp 出）
    ├── usecase                   （流程編排，對齊 SA / SD，不碰 Repository）
    ├── service                   （領域 + 持久化業務邏輯 + 併發元件編排）
    ├── repository                （DB / Redis / MQ 存取封裝，無編排）
    ├── model                     （僅存放 JPA / Database Entity）
    ├── object
    │   ├── req & resp            （僅用於 Controller HTTP 通訊）
    │   ├── dto                   （僅用於 MQ / 跨情境傳送載體）
    │   └── bo                    （純內部業務物件、工廠輸出、Usecase 處理主體）
    ├── README.md                 （情境 user story + 痛點）
    ├── SA.md / SD.md             （事實文件，SASD；當前 branch 的 QPS 等級版本）
    └── package-info.java         （情境佔位 + 一行說明）
```

### 物件類型表

| 物件類型 | 所屬 Package | 主要職責 | 套用場景 / 限制 |
|:---|:---|:---|:---|
| **Entity** | `<scenario>.model` | 映射 RDBMS 實體資料表 | **此 package 嚴禁存放非 Entity 物件** |
| **Req / Resp** | `<scenario>.object.req` / `resp` | Controller HTTP API 溝通 | Controller 入口必為 `*Req`，輸出必為 `*Resp` |
| **DTO** | `<scenario>.object.dto` | MQ / 跨情境傳送載體 | 僅用於跨網絡界限的數據負載 |
| **BO** | `<scenario>.object.bo` | 純內部業務邏輯 | 僅用於工廠輸出、Usecase 處理主體 |

**原則**：

- 每個 package 職責**單一**，嚴禁混用。
- 兩軸正交：**package 決定「哪個情境」、branch 決定「哪個 QPS 等級」**；同一情境在 L1~L4 branch 各有一版 SA/SD + code。

---

## 四、測試與閉環驗證政策

**測試分層**：

* **無外部依賴單元測試**：測 Usecase 編排與 stateless 的併發規則（防重、扣減不超賣、冪等），Mock 掉所有 Service 與 Repository，不加載 Spring 容器與外部連線，保障毫秒級執行。
* **整合測試**：驗真實高併發行為時，接 `docker-compose.yml` 起的 PostgreSQL / Redis / Kafka / RocketMQ，**不 Mock** 外部依賴。

**Mock 策略**：

```text
unit test  → Mock 掉 Service / Repository / 外部中介（Redis、MQ、DB）
整合 test  → 不 Mock，用 docker compose 起的真服務驗併發正確性
```

**閉環驗證**：

* 每次編碼結束後，必須在終端執行 **`mvn test`**，通過 100% 測試（含整合），確保無 Regression。
* SASD 模式閉環比對對象 = **SA + SD**：`code vs SA`（行為對齊 SA 的 Process Spec）、`code vs SD`（架構 / Table Schema / API 對齊）。
* （目前尚無 `mvnw` wrapper，先用 `mvn test`；需要時再補 wrapper。）

---

**文件版本**：v1（當前分支 L1）
**事實文件版本**：SASD（SA.md + SD.md）
**作者**：Claude Opus 4.8
