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
| 1 | `s01_lottery` | 抽獎活動系統 |
| 2 | `s02_redpacketrain` | 紅包雨系統 |
| 3 | `s03_jackpotpool` | 彩金獎池累積系統 |
| 4 | `s04_bettingentry` | 下注入口系統 |
| 5 | `s05_payoutsettlement` | 派彩結算系統 |
| 6 | `s06_gamewalletledger` | 玩家錢包與帳本 |
| 7 | `s07_leaderboard` | 活動排行榜系統 |
| 8 | `s08_gametaskreward` | 遊戲任務獎勵系統 |
| 9 | `s09_matchmaking` | 遊戲房間配對系統 |
| 10 | `s10_realtimebattlesync` | 即時對戰狀態同步 |
| 11 | `s11_gameproviderapi` | 第三方遊戲商 API 串接 |
| 12 | `s12_providertransfercallback` | 遊戲商轉入轉出 / 回調結算 |
| 13 | `s13_flashsale` | 秒殺限量商品 |
| 14 | `s14_couponclaim` | 優惠券搶券系統 |
| 15 | `s15_inventorydeduction` | 庫存扣減中心 |
| 16 | `s16_checkout` | Checkout 下單鏈路 |
| 17 | `s17_productdetailtraffic` | 商品詳情頁爆流量 |
| 18 | `s18_campaignhomepage` | 首頁活動頁 |
| 19 | `s19_promotionpricing` | 促銷價格計算 |
| 20 | `s20_shoppingcart` | 購物車系統 |
| 21 | `s21_orderquery` | 訂單查詢中心 |
| 22 | `s22_promotionnotification` | 大促通知推播 |
| 23 | `s23_paymentstatemachine` | 交易狀態機系統 |
| 24 | `s24_paymentchannelrouting` | 多支付通道路由 |
| 25 | `s25_ledger` | Ledger 帳本系統 |
| 26 | `s26_riskdecision` | 風控決策系統 |
| 27 | `s27_reconciliationcompensation` | 對帳與補償平台 |
| 28 | `s28_loginspike` | 登入尖峰系統 |
| 29 | `s29_tokensessionrefresh` | Token / Session / Refresh 系統 |
| 30 | `s30_apigatewayratelimit` | API Gateway / Rate Limit |
| 31 | `s31_rbacabac` | 權限中心 RBAC / ABAC |
| 32 | `s32_b2bmultitenantisolation` | B2B 多租戶 SaaS 隔離 |
| 33 | `s33_backgroundjob` | 背景任務平台 |
| 34 | `s34_delayjob` | 延遲任務系統 |
| 35 | `s35_thirdpartyretrycompensation` | 第三方 API 重試補償 |
| 36 | `s36_reportexport` | 批次匯出 / 大量報表任務 |
| 37 | `s37_biqueryspike` | BI / 後台查詢爆炸 |
| 38 | `s38_etlsync` | ETL / 大量資料同步 |
| 39 | `s39_deviceheartbeatingestion` | IoT 裝置心跳與資料上報 |
| 40 | `s40_fileuploadmediaprocessing` | 檔案上傳 / 圖片影片處理 |
| 41 | `s41_matchingengine` | 鏈下撮合 Engine |
| 42 | `s42_ordercancelentry` | 下單 / 撤單入口 |
| 43 | `s43_marketdatapush` | Market Data 行情推送 |
| 44 | `s44_klinegeneration` | K 線生成系統 |
| 45 | `s45_liquidationriskengine` | 強平與風控引擎 |
| 46 | `s46_depositcrediting` | 充值入帳系統 |
| 47 | `s47_withdrawalreview` | 提幣審核 / 出金系統 |
| 48 | `s48_exchangeassetwallet` | 交易所資產錢包 |
| 49 | `s49_rtbbidding` | RTB 即時競價系統 |
| 50 | `s50_impressionclicktracking` | 曝光 / 點擊事件追蹤 |
| 51 | `s51_budgetpacing` | 廣告預算 pacing 系統 |
| 52 | `s52_streamingaggregation` | 即時報表 / Streaming Aggregation |
| 53 | `s53_devicefingerprint` | 反詐 / Device Fingerprint |
| 54 | `s54_eventpipeline` | 事件資料管線 Pipeline |
| 55 | `s55_feed` | 追蹤動態牆 Feed |
| 56 | `s56_shortvideorecommendation` | 短影音首頁推薦流 |
| 57 | `s57_autocomplete` | 搜尋 Autocomplete |
| 58 | `s58_search` | 商品 / 內容搜尋系統 |
| 59 | `s59_merchantinventorysync` | Marketplace 商家庫存同步 |
| 60 | `s60_cdnmediadelivery` | 媒體內容分發 / CDN 整合 |
| 61 | `s61_logingestion` | Log Ingestion 系統 |
| 62 | `s62_metricsaggregation` | Metrics Aggregation 系統 |
| 63 | `s63_traceingestion` | Distributed Trace Ingestion |
| 64 | `s64_alertingincident` | Alerting / Incident 通知 |
| 65 | `s65_multitenantobservability` | 多租戶 Observability 隔離 |
| 66 | `s66_configfeatureflag` | Config Center / Feature Flag |
| 67 | `s67_globalratelimit` | 全域 Rate Limit Service |
| 68 | `s68_circuitbreakerdegradation` | 熔斷 / 降級平台 |
| 69 | `s69_workflowjobplatform` | Workflow / Job Platform |
| 70 | `s70_objectstoragemetadata` | Object Storage Metadata 系統 |
| 71 | `s71_nearbysearch` | 地理位置查詢 / Nearby Search |
| 72 | `s72_dispatchmatching` | 外送 / 派單 / 供需匹配 |
| 73 | `s73_notificationcenter` | 大型 Notification Center |
| 74 | `s74_datalakeingestion` | Data Lake Ingestion / 冷熱資料分層 |
| 75 | `s75_featurestoreonlineserving` | Feature Store / Online ML Serving |

---

## 鏡頭 1：by 業務 domain（我做的是哪個產業）

```text
博弈 / 遊戲
├─ s01_lottery s02_redpacketrain s03_jackpotpool s04_bettingentry s05_payoutsettlement
├─ s06_gamewalletledger s07_leaderboard s08_gametaskreward s09_matchmaking
└─ s10_realtimebattlesync s11_gameproviderapi s12_providertransfercallback

電商 / 促銷
├─ s13_flashsale s14_couponclaim s15_inventorydeduction s16_checkout s17_productdetailtraffic
└─ s18_campaignhomepage s19_promotionpricing s20_shoppingcart s21_orderquery s22_promotionnotification

支付 / 金融
└─ s23_paymentstatemachine s24_paymentchannelrouting s25_ledger s26_riskdecision s27_reconciliationcompensation

會員 / Gateway / 多租戶
└─ s28_loginspike s29_tokensessionrefresh s30_apigatewayratelimit s31_rbacabac s32_b2bmultitenantisolation

任務 / 排程 / 第三方整合
└─ s33_backgroundjob s34_delayjob s35_thirdpartyretrycompensation s36_reportexport

企業資料
└─ s37_biqueryspike s38_etlsync s40_fileuploadmediaprocessing

IoT
└─ s39_deviceheartbeatingestion

交易所 / Web3
├─ s41_matchingengine s42_ordercancelentry s43_marketdatapush s44_klinegeneration
└─ s45_liquidationriskengine s46_depositcrediting s47_withdrawalreview s48_exchangeassetwallet

AdTech / 資料流
└─ s49_rtbbidding s50_impressionclicktracking s51_budgetpacing s52_streamingaggregation s53_devicefingerprint s54_eventpipeline

社群 / Feed / 搜尋 / Marketplace
└─ s55_feed s56_shortvideorecommendation s57_autocomplete s58_search s59_merchantinventorysync s60_cdnmediadelivery

Observability
└─ s61_logingestion s62_metricsaggregation s63_traceingestion s64_alertingincident s65_multitenantobservability

Cloud / 平台治理
└─ s66_configfeatureflag s67_globalratelimit s68_circuitbreakerdegradation s69_workflowjobplatform s70_objectstoragemetadata

地理 / 派單
└─ s71_nearbysearch s72_dispatchmatching

通知
└─ s73_notificationcenter

ML / 資料平台
└─ s74_datalakeingestion s75_featurestoreonlineserving
```

---

## 鏡頭 2：by 壓力模型（面試判斷力用的 10 直覺）

> 這是最重要的鏡頭。同一直覺跨多個 domain，代表「架構套路可互相借用」。

```text
1. 搶資源型（庫存 / 名額有限，怕超賣 / 超領）
   └─ s02_redpacketrain s13_flashsale s14_couponclaim s15_inventorydeduction s01_lottery

2. 錢包帳本型（錢不能錯，要對得起帳、冪等、可對帳）
   └─ s06_gamewalletledger s25_ledger s23_paymentstatemachine s48_exchangeassetwallet
      s04_bettingentry s05_payoutsettlement s12_providertransfercallback s46_depositcrediting s47_withdrawalreview

3. 熱點計數型（單一 key 被打爆，累加 / 排名 / 計數）
   └─ s03_jackpotpool s07_leaderboard

4. 讀爆型（讀遠多於寫，靠 cache / 預生成 / CDN）
   └─ s17_productdetailtraffic s18_campaignhomepage s19_promotionpricing s21_orderquery
      s55_feed s56_shortvideorecommendation s57_autocomplete s58_search s60_cdnmediadelivery
      s37_biqueryspike s75_featurestoreonlineserving s71_nearbysearch

5. 寫爆型 / ingestion（寫入洪水，靠緩衝 / 批次 / 分區）
   └─ s39_deviceheartbeatingestion s50_impressionclicktracking s54_eventpipeline
      s61_logingestion s62_metricsaggregation s63_traceingestion s38_etlsync s74_datalakeingestion

6. 低延遲型（毫秒級，嚴格 timeout / 順序化）
   └─ s41_matchingengine s49_rtbbidding s43_marketdatapush s42_ordercancelentry

7. 長連線型（WebSocket / 狀態同步 / presence）
   └─ s10_realtimebattlesync s43_marketdatapush s09_matchmaking

8. 平台防爆型（限流 / 熔斷 / 降級 / 開關）
   └─ s30_apigatewayratelimit s67_globalratelimit s68_circuitbreakerdegradation
      s66_configfeatureflag s26_riskdecision s53_devicefingerprint

9. 多租戶型（tenant 隔離、hot tenant、quota）
   └─ s32_b2bmultitenantisolation s65_multitenantobservability s31_rbacabac

10. 任務補償型（非同步、重試、對帳、workflow、延遲觸發）
    └─ s33_backgroundjob s34_delayjob s35_thirdpartyretrycompensation s36_reportexport
       s27_reconciliationcompensation s69_workflowjobplatform s08_gametaskreward
       s22_promotionnotification s73_notificationcenter
```

---

## 鏡頭 3：by 數據角色（「數據相關在哪」）

```text
ingestion 寫爆（收洪水資料）
└─ s39_deviceheartbeatingestion s50_impressionclicktracking s54_eventpipeline
   s61_logingestion s62_metricsaggregation s63_traceingestion s38_etlsync s74_datalakeingestion

分析 / 查詢讀爆（大量讀、聚合、報表）
└─ s37_biqueryspike s52_streamingaggregation s21_orderquery s36_reportexport s75_featurestoreonlineserving

資料生命週期 / 冷熱分層 / schema 演進
└─ s74_datalakeingestion s38_etlsync s70_objectstoragemetadata

即時聚合 / stream processing
└─ s52_streamingaggregation s44_klinegeneration s62_metricsaggregation s51_budgetpacing
```

---

## 鏡頭 4：症狀 → package（遇到狀況直接對應，AI 主要入口）

| 我遇到的狀況（白話） | 先看這些 package |
|---|---|
| 限量商品 / 名額會被搶爆、怕超賣超領 | `s13_flashsale` `s14_couponclaim` `s15_inventorydeduction` `s02_redpacketrain` |
| 錢要扣得對、不能重複扣、要能對帳 | `s25_ledger` `s06_gamewalletledger` `s23_paymentstatemachine` `s27_reconciliationcompensation` |
| 某一個熱門 key（獎池 / 排名 / 計數器）被打爆 | `s03_jackpotpool` `s07_leaderboard` |
| 商品頁 / 首頁 / Feed 讀流量爆炸 | `s17_productdetailtraffic` `s18_campaignhomepage` `s55_feed` `s60_cdnmediadelivery` |
| 寫入量太大、DB 扛不住寫 | `s61_logingestion` `s54_eventpipeline` `s39_deviceheartbeatingestion` `s38_etlsync` |
| 需要毫秒級低延遲、超時就要 fallback | `s41_matchingengine` `s49_rtbbidding` `s43_marketdatapush` |
| 要維持大量長連線 / 即時推播狀態 | `s10_realtimebattlesync` `s43_marketdatapush` `s09_matchmaking` |
| 要限流 / 熔斷 / 降級保護核心流量 | `s30_apigatewayratelimit` `s67_globalratelimit` `s68_circuitbreakerdegradation` |
| 要按 tenant 隔離、防某個大客戶拖垮全部 | `s32_b2bmultitenantisolation` `s65_multitenantobservability` |
| 要非同步化、削峰、重試、補償、延遲觸發 | `s33_backgroundjob` `s34_delayjob` `s35_thirdpartyretrycompensation` `s69_workflowjobplatform` |
| 串第三方 API、怕它慢 / 掛掉拖垮我 | `s11_gameproviderapi` `s35_thirdpartyretrycompensation` `s24_paymentchannelrouting` |
| 登入 / 認證尖峰、被攻擊、session 壓力 | `s28_loginspike` `s29_tokensessionrefresh` `s30_apigatewayratelimit` |
| 大量通知 / 推播要發，交易通知不能被行銷通知拖垮 | `s73_notificationcenter` `s22_promotionnotification` |
| 要查附近 / 依位置匹配供需 | `s71_nearbysearch` `s72_dispatchmatching` |
| 要做即時報表 / streaming 聚合 | `s52_streamingaggregation` `s44_klinegeneration` `s62_metricsaggregation` |

---

## 鏡頭 5：相似 / 對照組（學一個就能對照理解另一個）

```text
搶資源三兄弟：差在「資源怎麼切、能不能延遲揭曉」
└─ s13_flashsale ↔ s14_couponclaim ↔ s02_redpacketrain

帳本一致性家族：差在「強一致 vs 最終一致、對帳補償在哪」
└─ s25_ledger ↔ s06_gamewalletledger ↔ s48_exchangeassetwallet ↔ s23_paymentstatemachine

熱點計數 vs 排行榜：差在「純累加 vs 需要排序」
└─ s03_jackpotpool ↔ s07_leaderboard

低延遲撮合 vs 競價：差在「順序化撮合 vs 嚴格超時競價」
└─ s41_matchingengine ↔ s49_rtbbidding

ingestion 家族：差在「資料能不能抽樣、能不能延遲入湖」
└─ s61_logingestion ↔ s63_traceingestion ↔ s50_impressionclicktracking ↔ s39_deviceheartbeatingestion

限流三層：差在「作用的網路層級」
└─ s30_apigatewayratelimit ↔ s67_globalratelimit ↔ s68_circuitbreakerdegradation

通知對照：差在「業務範圍與優先級策略」
└─ s22_promotionnotification（電商促銷）↔ s73_notificationcenter（平台級多渠道）

派單 vs 撮合：差在「地理供需匹配 vs 金融訂單撮合」
└─ s72_dispatchmatching ↔ s41_matchingengine
```

---

## 維護規則（給 AI 與使用者）

```text
1. 新增情境 → 開 package + 在鏡頭 0 加一列 + 至少掛進鏡頭 2 一個壓力模型
2. package 改名 / 刪除 → 同步改本檔，ROUTE 是分類的唯一真相
3. 本檔只放「對照 / 導覽」，不放 QPS 斷點細節（那在 catalog）
```
