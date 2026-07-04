# 高併發場景學習目錄（依 package 大分類重排版）

> 版本：v1（by-package）
> 目的：建立一份以台灣面試命中率為主、兼顧台灣外商與全球外商 System Design 的高併發場景學習目錄。
> 核心不是背技術名詞，而是建立「不同業務壓力模型下，架構如何隨 QPS 成長而改變」的判斷力。
> 排版說明：大分類已改為對齊 Spring Boot 專案的 package 樹，移除原 Part 1 / Part 2 / Part 3 三個最外層分類。

---

## 0. 共用 QPS 斷點定義

```text
L1：QPS 3,000
你的履歷起點等級。服務水平擴展、Redis、MQ、DB index 開始有感。

L2：QPS 10,000
單純加機器開始不夠，要開始削峰、快取、批次、非同步化。

L3：QPS 50,000
熱點 key、DB 寫入、MQ 堆積、Redis 壓力、分片問題明顯出現。

L4：QPS 100,000～500,000+
影片那種尖峰洪水等級。要前端聚合、近似值、隔離資源池、降級、預計算。
```

> `#` 欄沿用原始編號當穩定 ID（1～75），不隨大分類重排而改號，確保底部「學習優先順序」清單仍可對照。

---

## gaming-betting（博弈／遊戲／活動／錢包）12 組

| # | 場景 | L1：3,000 QPS | L2：10,000 QPS | L3：50,000 QPS | L4：100,000～500,000+ |
|---:|---|---|---|---|---|
| 1 | **抽獎活動系統** | Redis 扣獎品庫存，DB 記抽獎結果 | 抽獎請求進 MQ，worker 消費 | 預生成中獎序列，只取 token | 活動獨立叢集，結果延遲揭曉 |
| 2 | **紅包雨系統** | Redis 紅包池，使用者搶到後落 DB | 紅包預切片，每包 token 化 | 紅包池分 shard，防重領用 Redis set | 前端限流，排隊發放，顯示可延遲 |
| 3 | **彩金獎池累積系統** | Redis counter 累加獎池 | 累加事件進 MQ，批量更新 | counter 分片，讀取時聚合 | 顯示近似值，結算走強一致流程 |
| 4 | **下注入口系統** | API 驗證餘額後下注 | Redis / ledger 預凍結金額 | 按 gameId / tableId 分區 | 熱門桌獨立資源池，部分玩法限流 |
| 5 | **派彩結算系統** | 批次計算中獎後加錢包 | 派彩事件進 MQ，consumer 冪等 | 按期號 / 桌號分片結算 | 結算與下注入口隔離，允許延遲不可錯帳 |
| 6 | **玩家錢包與帳本** | 餘額表 + 流水表，樂觀鎖扣款 | ledger append-only，餘額快照 | 分 accountId 分表，熱玩家限頻 | 錢包獨立 cell，讀快照、寫流水 |
| 7 | **活動排行榜系統** | Redis ZSet 更新分數 | 寫入先進 MQ，批量更新 ZSet | 日榜、週榜、區服榜分區 | Top N 預計算，非 Top 排名延遲 |
| 8 | **遊戲任務獎勵系統** | 任務完成後同步發獎 | 任務事件進 MQ，獎勵非同步 | 任務狀態機 + 防重事件表 | 主遊戲流程與獎勵完全解耦 |
| 9 | **遊戲房間配對系統** | Redis 查可用房間 | Redis queue 分玩法配對 | 按區域、段位、玩法拆 pool | 熱門玩法獨立配對服務，允許等待 |
| 10 | **即時對戰狀態同步** | WebSocket gateway 推狀態 | 房間狀態放記憶體，結果落 DB | 房間 actor / shard，單房間順序處理 | gateway 分區，觀戰事件降頻 |
| 11 | **第三方遊戲商 API 串接** | 同步呼叫 provider API | provider timeout 重試、熔斷 | provider 分池、限流、fallback | 熱門 provider 獨立隔離，防止拖垮主平台 |
| 12 | **遊戲商轉入轉出 / 回調結算** | 轉入轉出同步扣加款 | callback 冪等、失敗補償 | provider callback 分區消費 | 平台錢包與 provider 狀態最終一致 |

---

## ecommerce-promotion（電商／活動頁／訂單／庫存）10 組

| # | 場景 | L1：3,000 QPS | L2：10,000 QPS | L3：50,000 QPS | L4：100,000～500,000+ |
|---:|---|---|---|---|---|
| 13 | **秒殺限量商品** | Redis 預扣庫存，DB 建訂單 | 排隊 + MQ 削峰 | 商品庫存 token 分片 | 秒殺獨立系統，令牌桶分批放行 |
| 14 | **優惠券搶券系統** | Redis 扣券庫存，用戶 set 防重領 | MQ 非同步發券 | 券池分 shard，用戶資格預熱 | 大促券分批釋放，熱門券獨立 cluster |
| 15 | **庫存扣減中心** | 下單同步扣庫存 | Redis 預扣，DB 最終實扣 | 分倉 / 分片庫存，回補補償 | 庫存 token 化，避免全局鎖 |
| 16 | **Checkout 下單鏈路** | 訂單、庫存、支付同步流程 | 訂單 pending，後續非同步 | order / payment / promo / inventory 拆開 | 核心下單最小化，推薦通知全部降級 |
| 17 | **商品詳情頁爆流量** | 商品資料 Redis cache | CDN + 多層 cache | 熱門商品 local cache | 靜態頁 + 動態小接口，庫存近似顯示 |
| 18 | **首頁活動頁** | API 聚合多 module | BFF cache 聚合結果 | 頁面資料預生成 | 首頁靜態化，個人化區塊延遲載入 |
| 19 | **促銷價格計算** | checkout 即時計算 | 價格規則 cache | 熱門商品價格快照 | 活動期間價格凍結快照 |
| 20 | **購物車系統** | DB 記購物車，Redis cache | 短時間合併寫入 | userId 分區，讀寫分離 | checkout 前再校正，展示可弱一致 |
| 21 | **訂單查詢中心** | 查 DB index | CQRS read model + cache | 訂單分表，查詢走 read DB / ES | 歷史訂單冷熱分離 |
| 22 | **大促通知推播** | 直接寫通知表 / 推播 | MQ 非同步推播 | 任務分片，供應商限流 | 交易通知與行銷通知隔離 |

---

## payment-finance（支付／金融／交易可靠性）5 組

這區刻意不拆退款、callback、timeout，因為它們都屬於交易狀態機的子題。

| # | 場景 | L1：3,000 QPS | L2：10,000 QPS | L3：50,000 QPS | L4：100,000～500,000+ |
|---:|---|---|---|---|---|
| 23 | **交易狀態機系統** | order / payment 狀態機 + 冪等 key | callback、查單、補償拆流程 | callback consumer 分區，交易 read model | 核心狀態轉移最小化，周邊功能全解耦 |
| 24 | **多支付通道路由** | 根據規則選通道 | 通道健康度 cache + fallback | 通道路由策略服務化 | 熱通道限流，國家 / 幣別 / 商戶隔離 |
| 25 | **Ledger 帳本系統** | 流水表 + 餘額表 | 帳本 append-only，餘額快照 | accountId 分表，異步對帳 | 讀快照、寫流水，帳本不可被非核心流量拖垮 |
| 26 | **風控決策系統** | 同步查規則 allow / deny | 規則 cache，事件非同步記錄 | risk score 預計算 | 同步風控只保留硬規則，複雜模型非同步 |
| 27 | **對帳與補償平台** | 批次拉檔對帳 | 差異單進補償流程 | 對帳任務分片，補償狀態機 | 主交易不等對帳，對帳平台獨立擴容 |

---

## identity-gateway-saas（會員／登入／Gateway／多租戶）5 組

| # | 場景 | L1：3,000 QPS | L2：10,000 QPS | L3：50,000 QPS | L4：100,000～500,000+ |
|---:|---|---|---|---|---|
| 28 | **登入尖峰系統** | DB 查帳密，Redis cache session | 登入限流，captcha / risk check | 熱用戶、攻擊 IP 限頻 | 登入服務獨立資源池，異常流量隔離 |
| 29 | **Token / Session / Refresh 系統** | JWT / session 基本驗證 | refresh token 黑名單 cache | token introspection cache | Gateway local cache，黑名單延遲同步 |
| 30 | **API Gateway / Rate Limit** | Gateway 做路由與 auth | user / IP 限流，Redis counter | 分散式 rate limit，滑動窗口 | 多層限流：edge、gateway、service |
| 31 | **權限中心 RBAC / ABAC** | DB 查角色權限 | 權限 cache，變更後失效 | tenant / role 分區 cache | 權限快照，查詢路徑不得打主庫 |
| 32 | **B2B 多租戶 SaaS 隔離** | tenantId 隔離資料 | hot tenant 限流、quota | 大 tenant 獨立 DB / schema | cell-based tenant isolation |

---

## job-integration（任務隊列／排程／第三方整合）4 組

| # | 場景 | L1：3,000 QPS | L2：10,000 QPS | L3：50,000 QPS | L4：100,000～500,000+ |
|---:|---|---|---|---|---|
| 33 | **背景任務平台** | DB task table + worker | MQ queue，worker 水平擴展 | 任務分 priority / type | 任務平台獨立化，支援限流、暫停、重跑 |
| 34 | **延遲任務系統** | DB polling 檢查到期任務 | Redis delay queue / MQ delay | 時間輪、分片掃描 | 大量延遲任務分 bucket，避免集中爆發 |
| 35 | **第三方 API 重試補償** | 失敗直接 retry | 指數退避 + 死信 | provider 分池、熔斷 | 重試平台化，防止雪崩式重送 |
| 36 | **批次匯出 / 大量報表任務** | 同步產報表 | 任務化，產完通知下載 | 分片查詢，避免拖垮 DB | 報表走 OLAP / snapshot，不碰 OLTP 主庫 |

---

## enterprise-data（企業資料實務）3 組

| # | 場景 | L1：3,000 QPS | L2：10,000 QPS | L3：50,000 QPS | L4：100,000～500,000+ |
|---:|---|---|---|---|---|
| 37 | **BI / 後台查詢爆炸** | DB index + 分頁 | read replica，查詢限流 | 預聚合表 / materialized view | OLTP / OLAP 完全分離 |
| 38 | **ETL / 大量資料同步** | 批次拉資料寫 DB | MQ / Kafka 作蓄水池 | 分批清洗、去重、補償 | 流批一體，冷熱資料分層 |
| 40 | **檔案上傳 / 圖片影片處理** | 後端接檔後存 storage | pre-signed URL 直傳 | 掃毒、縮圖、轉碼 pipeline | 大檔 multipart，CDN 分發，轉碼任務分級 |

---

## iot-device（IoT 裝置）1 組

| # | 場景 | L1：3,000 QPS | L2：10,000 QPS | L3：50,000 QPS | L4：100,000～500,000+ |
|---:|---|---|---|---|---|
| 39 | **IoT 裝置心跳與資料上報** | API 收資料批量寫入 | Kafka / MQ 緩衝 | deviceId / tenantId 分區 | 邊緣聚合、異常優先、全量延後入湖 |

---

## exchange-web3（交易所／Web3／數位資產）8 組

| # | 場景 | L1：3,000 QPS | L2：10,000 QPS | L3：50,000 QPS | L4：100,000～500,000+ |
|---:|---|---|---|---|---|
| 41 | **鏈下撮合 Engine** | 單 symbol order book 記憶體撮合 | 按 symbol 分 worker | 熱門 symbol 獨立撮合實例 | 撮合核心單執行緒順序化，周邊全解耦 |
| 42 | **下單 / 撤單入口** | API 驗證後送撮合 | 風控、資產凍結前置 | order gateway 分區 | 下單入口與查詢隔離，爆量時保撮合 |
| 43 | **Market Data 行情推送** | WebSocket 推最新成交 | symbol channel 分組 | 熱門 symbol 分散推送 | tick、秒級、聚合價分層推送 |
| 44 | **K 線生成系統** | 成交 tick 寫 DB 後聚合 | stream processor 聚合 K 線 | symbol / time window 分區 | 熱門即時聚合，冷門延遲生成 |
| 45 | **強平與風控引擎** | 定時掃倉位與價格 | 價格事件驅動檢查倉位 | symbol / user 分片計算風險 | 強平服務獨立資源池，風控優先 |
| 46 | **充值入帳系統** | 掃鏈確認後入帳 | 鏈上事件進 MQ | 地址分片掃描，入帳冪等 | 熱鏈獨立掃描器，入帳與通知解耦 |
| 47 | **提幣審核 / 出金系統** | API 建提幣單 | 風控、冷熱錢包流程拆開 | 提幣排程分片，鏈上廣播重試 | 提幣限流，資產安全優先於即時性 |
| 48 | **交易所資產錢包** | 可用餘額 / 凍結餘額 | 下單凍結、成交扣減事件化 | account / asset 分片 | 資產核心與行情、活動、通知完全隔離 |

---

## adtech-streaming（AdTech／資料流／即時決策）6 組

| # | 場景 | L1：3,000 QPS | L2：10,000 QPS | L3：50,000 QPS | L4：100,000～500,000+ |
|---:|---|---|---|---|---|
| 49 | **RTB 即時競價系統** | 查廣告規則後回應 | 候選廣告 cache，素材預載 | bidding service 低延遲化 | 嚴格 timeout，超時 fallback |
| 50 | **曝光 / 點擊事件追蹤** | API 收事件寫 MQ | client 批次上報 | campaign / user 分區 | edge 收集，抽樣、延遲入湖 |
| 51 | **廣告預算 pacing 系統** | 即時計算消耗 | budget cache + 批次同步 | campaign 分區消耗計算 | 預算近似扣減，最終對帳修正 |
| 52 | **即時報表 / Streaming Aggregation** | 事件進 DB 後查詢 | Kafka + stream processor | window aggregation 分片 | 報表近似值，延遲修正 |
| 53 | **反詐 / Device Fingerprint** | 同步查規則 | device / user 特徵 cache | risk feature 預計算 | 同步只跑硬規則，模型非同步補判 |
| 54 | **事件資料管線 Pipeline** | API 收事件進 MQ | schema validation + DLQ | 多 topic 分流，consumer group 擴展 | 流批分層，異常資料隔離處理 |

---

## social-content-marketplace（社群／Feed／搜尋／推薦／Marketplace）6 組

| # | 場景 | L1：3,000 QPS | L2：10,000 QPS | L3：50,000 QPS | L4：100,000～500,000+ |
|---:|---|---|---|---|---|
| 55 | **追蹤動態牆 Feed** | 讀取時查 follow + posts | 小用戶 fanout-on-write | 大 V fanout-on-read，混合模式 | timeline cache，熱門作者特殊通道 |
| 56 | **短影音首頁推薦流** | 即時計算推薦結果 | 預生成 feed list 放 Redis | 召回、粗排、精排拆服務 | 離線 / 近線特徵，線上輕量排序 |
| 57 | **搜尋 Autocomplete** | 每次輸入查搜尋引擎 | 前端 debounce，熱門 prefix cache | Trie / prefix cache | edge cache，熱詞預生成 |
| 58 | **商品 / 內容搜尋系統** | OpenSearch / ES 直接查 | query cache + filter cache | 熱門搜尋結果快照 | 搜尋降級，熱門結果預計算 |
| 59 | **Marketplace 商家庫存同步** | 商家 API 同步更新 | MQ 非同步同步商品 / 庫存 | 商家分片、失敗補償 | 大商家獨立同步通道，避免拖垮平台 |
| 60 | **媒體內容分發 / CDN 整合** | 原站直接提供媒體 URL | CDN cache，縮圖預生成 | 熱門媒體多區域快取 | 熱內容 edge 分發，原站保護 |

---

## observability（Observability／Log／Metrics／Trace）5 組

| # | 場景 | L1：3,000 QPS | L2：10,000 QPS | L3：50,000 QPS | L4：100,000～500,000+ |
|---:|---|---|---|---|---|
| 61 | **Log Ingestion 系統** | API / agent 收 log 寫 storage | agent 批次傳送，Kafka buffer | tenant / service 分區 | hot tenant 限流，冷熱 log 分層 |
| 62 | **Metrics Aggregation 系統** | 寫入 time-series DB | client-side aggregation | label cardinality 控制 | 預聚合、降採樣、長期資料冷存 |
| 63 | **Distributed Trace Ingestion** | trace span 直接上報 | sampling + 批次傳送 | tail-based sampling | 高流量服務抽樣，關鍵錯誤全保留 |
| 64 | **Alerting / Incident 通知** | rule polling 查指標 | alert queue + 去重 | alert grouping / suppression | 大事故時防通知風暴 |
| 65 | **多租戶 Observability 隔離** | tenantId 查詢隔離 | quota + rate limit | hot tenant 查詢獨立資源 | 大客戶獨立 shard / cell |

---

## cloud-platform（Cloud Infra／平台治理／防爆工具）5 組

| # | 場景 | L1：3,000 QPS | L2：10,000 QPS | L3：50,000 QPS | L4：100,000～500,000+ |
|---:|---|---|---|---|---|
| 66 | **Config Center / Feature Flag** | 服務啟動時拉 config | local cache + watch 更新 | 灰度發布、按 tenant / user 生效 | edge cache，避免所有服務同時打 config |
| 67 | **全域 Rate Limit Service** | Redis counter 限流 | sliding window / token bucket | 分散式限流，本地預扣 token | edge + gateway + service 多層限流 |
| 68 | **熔斷 / 降級平台** | service client timeout | circuit breaker + fallback | bulkhead isolation | 自動降級，保核心交易流量 |
| 69 | **Workflow / Job Platform** | 單一 worker 執行任務 | DAG 任務、重試、狀態機 | 任務分片與優先級 | 大量任務隔離隊列，支援暫停與回放 |
| 70 | **Object Storage Metadata 系統** | DB 記檔案 metadata | metadata cache | 按 bucket / tenant 分片 | 大 bucket index 拆分，list 操作限流 |

> 待定位：#73 大型 Notification Center 在 CODEX package 樹中沒有對應大分類，暫掛此區，最終 package 家由你決定（見文末說明）。

| # | 場景 | L1：3,000 QPS | L2：10,000 QPS | L3：50,000 QPS | L4：100,000～500,000+ |
|---:|---|---|---|---|---|
| 73 | **大型 Notification Center** | 寫通知表 + 推播 | MQ 分渠道推送 | 用戶分片、供應商限流 | 交易通知優先，行銷通知延遲或丟棄 |

---

## geo-dispatch（地理位置／派單匹配）2 組

| # | 場景 | L1：3,000 QPS | L2：10,000 QPS | L3：50,000 QPS | L4：100,000～500,000+ |
|---:|---|---|---|---|---|
| 71 | **地理位置查詢 / Nearby Search** | DB + geo index 查附近 | Redis geo / geohash cache | 區域分片，熱門地點 cache | edge / region 分區，近似查詢 |
| 72 | **外送 / 派單 / 供需匹配** | 查附近司機後派單 | 司機位置進 stream | 區域 actor，供需池分區 | 熱區獨立調度，ETA / 價格近似計算 |

---

## ml-data-platform（ML／資料平台）2 組

| # | 場景 | L1：3,000 QPS | L2：10,000 QPS | L3：50,000 QPS | L4：100,000～500,000+ |
|---:|---|---|---|---|---|
| 74 | **Data Lake Ingestion / 冷熱資料分層** | 批次寫 object storage | Kafka / batch ingestion | schema evolution + compaction | hot data 即時查，cold data 延遲入湖 |
| 75 | **Feature Store / Online ML Serving** | 線上查用戶特徵 | 熱特徵 cache | online / nearline / offline 分層 | 缺失特徵 fallback，線上模型限時回應 |

---

# 75 組的學習優先順序

你不用平均學。正式 study 可以分三層。

---

## 第一層：必學核心 25 組

這 25 組要完整拆架構、QPS 斷點、資料一致性、Redis / MQ / DB 分工、面試回答。

```text
1. 抽獎活動系統
2. 紅包雨系統
4. 下注入口系統
6. 玩家錢包與帳本
7. 活動排行榜系統
11. 第三方遊戲商 API 串接
12. 遊戲商轉入轉出 / 回調結算

13. 秒殺限量商品
14. 優惠券搶券系統
15. 庫存扣減中心
16. Checkout 下單鏈路
17. 商品詳情頁爆流量

23. 交易狀態機系統
25. Ledger 帳本系統
27. 對帳與補償平台

28. 登入尖峰系統
30. API Gateway / Rate Limit
32. B2B 多租戶 SaaS 隔離
33. 背景任務平台
37. BI / 後台查詢爆炸
38. ETL / 大量資料同步
39. IoT 裝置心跳與資料上報

41. 鏈下撮合 Engine
43. Market Data 行情推送
55. 追蹤動態牆 Feed
```

---

## 第二層：次核心 30 組

這 30 組要理解壓力模型與典型架構，不一定每組畫到最細。

```text
3. 彩金獎池累積系統
5. 派彩結算系統
8. 遊戲任務獎勵系統
9. 遊戲房間配對系統
10. 即時對戰狀態同步

18. 首頁活動頁
19. 促銷價格計算
20. 購物車系統
21. 訂單查詢中心
22. 大促通知推播

24. 多支付通道路由
26. 風控決策系統
29. Token / Session / Refresh 系統
31. 權限中心 RBAC / ABAC

34. 延遲任務系統
35. 第三方 API 重試補償
36. 批次匯出 / 大量報表任務
40. 檔案上傳 / 圖片影片處理

42. 下單 / 撤單入口
44. K 線生成系統
45. 強平與風控引擎
46. 充值入帳系統
47. 提幣審核 / 出金系統
48. 交易所資產錢包

49. RTB 即時競價系統
50. 曝光 / 點擊事件追蹤
52. 即時報表 / Streaming Aggregation
56. 短影音首頁推薦流
57. 搜尋 Autocomplete
60. 媒體內容分發 / CDN 整合
```

---

## 第三層：延伸補洞 20 組

這 20 組是用來補完整外商與平台視野。

```text
51. 廣告預算 pacing 系統
53. 反詐 / Device Fingerprint
54. 事件資料管線 Pipeline
58. 商品 / 內容搜尋系統
59. Marketplace 商家庫存同步

61. Log Ingestion 系統
62. Metrics Aggregation 系統
63. Distributed Trace Ingestion
64. Alerting / Incident 通知
65. 多租戶 Observability 隔離

66. Config Center / Feature Flag
67. 全域 Rate Limit Service
68. 熔斷 / 降級平台
69. Workflow / Job Platform
70. Object Storage Metadata 系統

71. 地理位置查詢 / Nearby Search
72. 外送 / 派單 / 供需匹配
73. 大型 Notification Center
74. Data Lake Ingestion / 冷熱資料分層
75. Feature Store / Online ML Serving
```

---

# 核心高併發直覺分類

這 75 組最後會沉澱成 10 種直覺：

```text
1. 搶資源型：紅包、秒殺、優惠券、庫存
2. 錢包帳本型：下注、支付、ledger、轉入轉出
3. 熱點計數型：點讚、獎池、排行榜、人數
4. 讀爆型：商品頁、Feed、搜尋、推薦
5. 寫爆型：log、event tracking、IoT、ETL
6. 低延遲型：撮合、RTB、行情推送
7. 長連線型：聊天室、對戰、market data、presence
8. 平台防爆型：gateway、rate limit、熔斷、feature flag
9. 多租戶型：B2B SaaS、observability、enterprise data
10. 任務補償型：對帳、重試、排程、workflow
```

---

# 後續每章固定拆解模板

每一章後續都可以用這個格式深入：

```text
# 場景名稱

## 1. 場景定義
這個系統在業務上解決什麼問題？

## 2. 核心 API / 事件
有哪些主要入口？例如下注、點讚、下單、發文、曝光事件。

## 3. 資料特性
哪些資料可以延遲？
哪些資料不能丟？
哪些資料必須強一致？
哪些資料可以最終一致？

## 4. QPS 3000 架構
這個量級怎麼做就夠？

## 5. QPS 10000 斷點
第一個會爆的地方通常是哪裡？

## 6. QPS 50000 斷點
需要開始引入哪些削峰、分片、批次、cache？

## 7. QPS 100000+ 斷點
哪些設計必須改成專用架構？
哪些功能必須降級？
哪些資料必須近似？

## 8. 典型錯誤設計
常見八股錯在哪？
為什麼不能只說 Redis / MQ？

## 9. Senior 面試回答
怎麼把架構講成有實戰感？

## 10. 延伸比較
和支付、秒殺、直播、交易所等其他場景有什麼異同？
```

---

# 最終學習目標

看完並實際拆完這份目錄後，面試遇到任何高併發題目，先判斷：

```text
1. 是讀爆，還是寫爆？
2. 是平均高流量，還是瞬間尖峰？
3. 有沒有熱點 key？
4. 能不能丟？
5. 能不能延遲？
6. 能不能最終一致？
7. DB 能不能碰？
8. Redis 是當 cache、counter、lock、queue，還是 hot data store？
9. MQ 是削峰、解耦、順序、還是可靠投遞？
10. 哪一層應該限流、聚合、降級？
```

這才是 Senior Backend / Tech Lead 面試時真正有價值的高併發直覺。

---

# 附錄：大分類重排差異（相對原始 catalog）

```text
本次只動「大分類的切法」，75 組內容與編號一字不改。
差異全部集中在原始 F、L 兩大類被 CODEX 拆細：

原 F（台灣企業實務型 37~40）
├─ 37 BI / 後台查詢爆炸      → enterprise-data
├─ 38 ETL / 大量資料同步     → enterprise-data
├─ 39 IoT 裝置心跳與資料上報  → iot-device（獨立成一類）
└─ 40 檔案上傳 / 圖片影片處理 → enterprise-data

原 L（全球大型場景補完 71~75）
├─ 71 地理位置查詢 / Nearby Search → geo-dispatch
├─ 72 外送 / 派單 / 供需匹配        → geo-dispatch
├─ 73 大型 Notification Center      → ⚠ CODEX 樹無對應，暫掛 cloud-platform
├─ 74 Data Lake Ingestion           → ml-data-platform
└─ 75 Feature Store / Online ML      → ml-data-platform

其餘 A~E、G~K 九大類與 CODEX 一一對應，順序與內容不變。
```
