# SA.md — s53_devicefingerprint（反詐 / Device Fingerprint）

> 事實文件（SASD）· 結構化分析。描述「反詐辨識要做什麼」的**業務行為**，平台無關（P1..Pn 共用；各平台怎麼扛量在各自 SD）。
> 與 s26 風控同一套哲學：**同步只放「快而確定」的硬規則**（明顯機器人 / 黑名單裝置），重運算的複雜模型挪到非同步事後補判，避免守門變瓶頸。

## 1. Context Diagram

```text
[請求 / 點擊] --(裝置/行為特徵)--> (反詐) --(PASS / BLOCK，同步快)--> [業務]

(反詐) --(硬規則 / 黑名單)--> [util.redis]
(反詐) --(特徵事件)--------> [MQ]（非同步：複雜模型補判）
```

## 2. DFD（精簡 box flow）

```text
[請求進] --(deviceId, 指紋特徵, 行為)-->
   ├─【同步（快）】
   │    [硬規則] 黑名單裝置 / 明顯機器人特徵 → BLOCK / PASS
   │
   └─【非同步（事後）】
        發特徵事件 → [MQ] → [複雜模型] 關聯分析 / 行為序列
             └─ 標記可疑 / 更新黑名單 / 回饋規則
```

## 3. Process Spec（行為基準，decision table）

```text
[同步硬規則]（必須快、確定）
├─ 命中黑名單裝置 / 明顯機器人特徵 → BLOCK
└─ 皆過 → PASS（放行，不拖慢正常流量）

[同步禁令]
└─ 只放「快而確定」的規則；重運算不得放同步路徑

[非同步複雜模型]
├─ 關聯分析 / 行為序列 → 事後標記可疑、更新黑名單
└─ 允許延遲（事後補判）

[對抗性]
└─ 詐騙會偽裝進化 → 規則 / 名單須能持續更新（模型回饋）
```

## 4. State Transition（一個裝置的信任狀態）

```text
【UNKNOWN】──(同步硬規則命中 / 模型補判)──> 【SUSPICIOUS / BLACKLISTED】
      └──(持續正常)──> 【TRUSTED】
```

## 5. Data Dictionary（邏輯欄位；實體 DDL 在各平台 SD）

```text
device_fingerprint（裝置指紋）
├─ deviceId(PK) / features（指紋特徵集）/ trust（UNKNOWN|TRUSTED|SUSPICIOUS|BLACKLISTED）
└─ lastSeenAt

fraud_event（特徵事件，供模型）
├─ eventId(PK) / deviceId / decision（PASS|BLOCK）/ features / ts
```
