# ROUTE — 高併發情境導覽索引（給 AI 讀）

> 這份文件是「找哪個 package」的索引，不是「這個怎麼開發」的資料表。
> 架構演進、QPS L1~L4 斷點內容在 `75_high_concurrency_catalog_by_package.md`。
>
> 用法：使用者用白話描述遇到的狀況，AI 依下方鏡頭把它對應到一或多個 package，
> 再引導使用者去看 `src/main/java/com/concurrencylabs/<package>/`。
> 同一個 package 會在多個鏡頭下重複出現，這是刻意的（實體只有一份，邏輯分類可重疊）。

```text
實體位置
└─ src/main/java/com/concurrencylabs/<package>/
   （75 個情境全部攤平在同一層，無大分類目錄）
```

---

## 鏡頭 0：package ↔ 情境 對照總表

| # | package | 情境 |
|---:|---|---|
| 1 | `lottery` | 抽獎活動系統 |
| 2 | `redpacketrain` | 紅包雨系統 |
| 3 | `jackpotpool` | 彩金獎池累積系統 |
| 4 | `bettingentry` | 下注入口系統 |
| 5 | `payoutsettlement` | 派彩結算系統 |
| 6 | `gamewalletledger` | 玩家錢包與帳本 |
| 7 | `leaderboard` | 活動排行榜系統 |
| 8 | `gametaskreward` | 遊戲任務獎勵系統 |
| 9 | `matchmaking` | 遊戲房間配對系統 |
| 10 | `realtimebattlesync` | 即時對戰狀態同步 |
| 11 | `gameproviderapi` | 第三方遊戲商 API 串接 |
| 12 | `providertransfercallback` | 遊戲商轉入轉出 / 回調結算 |
| 13 | `flashsale` | 秒殺限量商品 |
| 14 | `couponclaim` | 優惠券搶券系統 |
| 15 | `inventorydeduction` | 庫存扣減中心 |
| 16 | `checkout` | Checkout 下單鏈路 |
| 17 | `productdetailtraffic` | 商品詳情頁爆流量 |
| 18 | `campaignhomepage` | 首頁活動頁 |
| 19 | `promotionpricing` | 促銷價格計算 |
| 20 | `shoppingcart` | 購物車系統 |
| 21 | `orderquery` | 訂單查詢中心 |
| 22 | `promotionnotification` | 大促通知推播 |
| 23 | `paymentstatemachine` | 交易狀態機系統 |
| 24 | `paymentchannelrouting` | 多支付通道路由 |
| 25 | `ledger` | Ledger 帳本系統 |
| 26 | `riskdecision` | 風控決策系統 |
| 27 | `reconciliationcompensation` | 對帳與補償平台 |
| 28 | `loginspike` | 登入尖峰系統 |
| 29 | `tokensessionrefresh` | Token / Session / Refresh 系統 |
| 30 | `apigatewayratelimit` | API Gateway / Rate Limit |
| 31 | `rbacabac` | 權限中心 RBAC / ABAC |
| 32 | `b2bmultitenantisolation` | B2B 多租戶 SaaS 隔離 |
| 33 | `backgroundjob` | 背景任務平台 |
| 34 | `delayjob` | 延遲任務系統 |
| 35 | `thirdpartyretrycompensation` | 第三方 API 重試補償 |
| 36 | `reportexport` | 批次匯出 / 大量報表任務 |
| 37 | `biqueryspike` | BI / 後台查詢爆炸 |
| 38 | `etlsync` | ETL / 大量資料同步 |
| 39 | `deviceheartbeatingestion` | IoT 裝置心跳與資料上報 |
| 40 | `fileuploadmediaprocessing` | 檔案上傳 / 圖片影片處理 |
| 41 | `matchingengine` | 鏈下撮合 Engine |
| 42 | `ordercancelentry` | 下單 / 撤單入口 |
| 43 | `marketdatapush` | Market Data 行情推送 |
| 44 | `klinegeneration` | K 線生成系統 |
| 45 | `liquidationriskengine` | 強平與風控引擎 |
| 46 | `depositcrediting` | 充值入帳系統 |
| 47 | `withdrawalreview` | 提幣審核 / 出金系統 |
| 48 | `exchangeassetwallet` | 交易所資產錢包 |
| 49 | `rtbbidding` | RTB 即時競價系統 |
| 50 | `impressionclicktracking` | 曝光 / 點擊事件追蹤 |
| 51 | `budgetpacing` | 廣告預算 pacing 系統 |
| 52 | `streamingaggregation` | 即時報表 / Streaming Aggregation |
| 53 | `devicefingerprint` | 反詐 / Device Fingerprint |
| 54 | `eventpipeline` | 事件資料管線 Pipeline |
| 55 | `feed` | 追蹤動態牆 Feed |
| 56 | `shortvideorecommendation` | 短影音首頁推薦流 |
| 57 | `autocomplete` | 搜尋 Autocomplete |
| 58 | `search` | 商品 / 內容搜尋系統 |
| 59 | `merchantinventorysync` | Marketplace 商家庫存同步 |
| 60 | `cdnmediadelivery` | 媒體內容分發 / CDN 整合 |
| 61 | `logingestion` | Log Ingestion 系統 |
| 62 | `metricsaggregation` | Metrics Aggregation 系統 |
| 63 | `traceingestion` | Distributed Trace Ingestion |
| 64 | `alertingincident` | Alerting / Incident 通知 |
| 65 | `multitenantobservability` | 多租戶 Observability 隔離 |
| 66 | `configfeatureflag` | Config Center / Feature Flag |
| 67 | `globalratelimit` | 全域 Rate Limit Service |
| 68 | `circuitbreakerdegradation` | 熔斷 / 降級平台 |
| 69 | `workflowjobplatform` | Workflow / Job Platform |
| 70 | `objectstoragemetadata` | Object Storage Metadata 系統 |
| 71 | `nearbysearch` | 地理位置查詢 / Nearby Search |
| 72 | `dispatchmatching` | 外送 / 派單 / 供需匹配 |
| 73 | `notificationcenter` | 大型 Notification Center |
| 74 | `datalakeingestion` | Data Lake Ingestion / 冷熱資料分層 |
| 75 | `featurestoreonlineserving` | Feature Store / Online ML Serving |

---

## 鏡頭 1：by 業務 domain（我做的是哪個產業）

```text
博弈 / 遊戲
├─ lottery redpacketrain jackpotpool bettingentry payoutsettlement
├─ gamewalletledger leaderboard gametaskreward matchmaking
└─ realtimebattlesync gameproviderapi providertransfercallback

電商 / 促銷
├─ flashsale couponclaim inventorydeduction checkout productdetailtraffic
└─ campaignhomepage promotionpricing shoppingcart orderquery promotionnotification

支付 / 金融
└─ paymentstatemachine paymentchannelrouting ledger riskdecision reconciliationcompensation

會員 / Gateway / 多租戶
└─ loginspike tokensessionrefresh apigatewayratelimit rbacabac b2bmultitenantisolation

任務 / 排程 / 第三方整合
└─ backgroundjob delayjob thirdpartyretrycompensation reportexport

企業資料
└─ biqueryspike etlsync fileuploadmediaprocessing

IoT
└─ deviceheartbeatingestion

交易所 / Web3
├─ matchingengine ordercancelentry marketdatapush klinegeneration
└─ liquidationriskengine depositcrediting withdrawalreview exchangeassetwallet

AdTech / 資料流
└─ rtbbidding impressionclicktracking budgetpacing streamingaggregation devicefingerprint eventpipeline

社群 / Feed / 搜尋 / Marketplace
└─ feed shortvideorecommendation autocomplete search merchantinventorysync cdnmediadelivery

Observability
└─ logingestion metricsaggregation traceingestion alertingincident multitenantobservability

Cloud / 平台治理
└─ configfeatureflag globalratelimit circuitbreakerdegradation workflowjobplatform objectstoragemetadata

地理 / 派單
└─ nearbysearch dispatchmatching

通知
└─ notificationcenter

ML / 資料平台
└─ datalakeingestion featurestoreonlineserving
```

---

## 鏡頭 2：by 壓力模型（面試判斷力用的 10 直覺）

> 這是最重要的鏡頭。同一直覺跨多個 domain，代表「架構套路可互相借用」。

```text
1. 搶資源型（庫存 / 名額有限，怕超賣 / 超領）
   └─ redpacketrain flashsale couponclaim inventorydeduction lottery

2. 錢包帳本型（錢不能錯，要對得起帳、冪等、可對帳）
   └─ gamewalletledger ledger paymentstatemachine exchangeassetwallet
      bettingentry payoutsettlement providertransfercallback depositcrediting withdrawalreview

3. 熱點計數型（單一 key 被打爆，累加 / 排名 / 計數）
   └─ jackpotpool leaderboard

4. 讀爆型（讀遠多於寫，靠 cache / 預生成 / CDN）
   └─ productdetailtraffic campaignhomepage promotionpricing orderquery
      feed shortvideorecommendation autocomplete search cdnmediadelivery
      biqueryspike featurestoreonlineserving nearbysearch

5. 寫爆型 / ingestion（寫入洪水，靠緩衝 / 批次 / 分區）
   └─ deviceheartbeatingestion impressionclicktracking eventpipeline
      logingestion metricsaggregation traceingestion etlsync datalakeingestion

6. 低延遲型（毫秒級，嚴格 timeout / 順序化）
   └─ matchingengine rtbbidding marketdatapush ordercancelentry

7. 長連線型（WebSocket / 狀態同步 / presence）
   └─ realtimebattlesync marketdatapush matchmaking

8. 平台防爆型（限流 / 熔斷 / 降級 / 開關）
   └─ apigatewayratelimit globalratelimit circuitbreakerdegradation
      configfeatureflag riskdecision devicefingerprint

9. 多租戶型（tenant 隔離、hot tenant、quota）
   └─ b2bmultitenantisolation multitenantobservability rbacabac

10. 任務補償型（非同步、重試、對帳、workflow、延遲觸發）
    └─ backgroundjob delayjob thirdpartyretrycompensation reportexport
       reconciliationcompensation workflowjobplatform gametaskreward
       promotionnotification notificationcenter
```

---

## 鏡頭 3：by 數據角色（「數據相關在哪」）

```text
ingestion 寫爆（收洪水資料）
└─ deviceheartbeatingestion impressionclicktracking eventpipeline
   logingestion metricsaggregation traceingestion etlsync datalakeingestion

分析 / 查詢讀爆（大量讀、聚合、報表）
└─ biqueryspike streamingaggregation orderquery reportexport featurestoreonlineserving

資料生命週期 / 冷熱分層 / schema 演進
└─ datalakeingestion etlsync objectstoragemetadata

即時聚合 / stream processing
└─ streamingaggregation klinegeneration metricsaggregation budgetpacing
```

---

## 鏡頭 4：症狀 → package（遇到狀況直接對應，AI 主要入口）

| 我遇到的狀況（白話） | 先看這些 package |
|---|---|
| 限量商品 / 名額會被搶爆、怕超賣超領 | `flashsale` `couponclaim` `inventorydeduction` `redpacketrain` |
| 錢要扣得對、不能重複扣、要能對帳 | `ledger` `gamewalletledger` `paymentstatemachine` `reconciliationcompensation` |
| 某一個熱門 key（獎池 / 排名 / 計數器）被打爆 | `jackpotpool` `leaderboard` |
| 商品頁 / 首頁 / Feed 讀流量爆炸 | `productdetailtraffic` `campaignhomepage` `feed` `cdnmediadelivery` |
| 寫入量太大、DB 扛不住寫 | `logingestion` `eventpipeline` `deviceheartbeatingestion` `etlsync` |
| 需要毫秒級低延遲、超時就要 fallback | `matchingengine` `rtbbidding` `marketdatapush` |
| 要維持大量長連線 / 即時推播狀態 | `realtimebattlesync` `marketdatapush` `matchmaking` |
| 要限流 / 熔斷 / 降級保護核心流量 | `apigatewayratelimit` `globalratelimit` `circuitbreakerdegradation` |
| 要按 tenant 隔離、防某個大客戶拖垮全部 | `b2bmultitenantisolation` `multitenantobservability` |
| 要非同步化、削峰、重試、補償、延遲觸發 | `backgroundjob` `delayjob` `thirdpartyretrycompensation` `workflowjobplatform` |
| 串第三方 API、怕它慢 / 掛掉拖垮我 | `gameproviderapi` `thirdpartyretrycompensation` `paymentchannelrouting` |
| 登入 / 認證尖峰、被攻擊、session 壓力 | `loginspike` `tokensessionrefresh` `apigatewayratelimit` |
| 大量通知 / 推播要發，交易通知不能被行銷通知拖垮 | `notificationcenter` `promotionnotification` |
| 要查附近 / 依位置匹配供需 | `nearbysearch` `dispatchmatching` |
| 要做即時報表 / streaming 聚合 | `streamingaggregation` `klinegeneration` `metricsaggregation` |

---

## 鏡頭 5：相似 / 對照組（學一個就能對照理解另一個）

```text
搶資源三兄弟：差在「資源怎麼切、能不能延遲揭曉」
└─ flashsale ↔ couponclaim ↔ redpacketrain

帳本一致性家族：差在「強一致 vs 最終一致、對帳補償在哪」
└─ ledger ↔ gamewalletledger ↔ exchangeassetwallet ↔ paymentstatemachine

熱點計數 vs 排行榜：差在「純累加 vs 需要排序」
└─ jackpotpool ↔ leaderboard

低延遲撮合 vs 競價：差在「順序化撮合 vs 嚴格超時競價」
└─ matchingengine ↔ rtbbidding

ingestion 家族：差在「資料能不能抽樣、能不能延遲入湖」
└─ logingestion ↔ traceingestion ↔ impressionclicktracking ↔ deviceheartbeatingestion

限流三層：差在「作用的網路層級」
└─ apigatewayratelimit ↔ globalratelimit ↔ circuitbreakerdegradation

通知對照：差在「業務範圍與優先級策略」
└─ promotionnotification（電商促銷）↔ notificationcenter（平台級多渠道）

派單 vs 撮合：差在「地理供需匹配 vs 金融訂單撮合」
└─ dispatchmatching ↔ matchingengine
```

---

## 維護規則（給 AI 與使用者）

```text
1. 新增情境 → 開 package + 在鏡頭 0 加一列 + 至少掛進鏡頭 2 一個壓力模型
2. package 改名 / 刪除 → 同步改本檔，ROUTE 是分類的唯一真相
3. 本檔只放「對照 / 導覽」，不放 QPS 斷點細節（那在 catalog）
```
