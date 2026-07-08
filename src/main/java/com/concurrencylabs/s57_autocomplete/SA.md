# SA.md — s57_autocomplete（搜尋 Autocomplete）

> 事實文件（SASD）· 結構化分析。描述「Autocomplete 要做什麼」的**業務行為**，平台無關（P1..Pn 共用；各平台怎麼扛量在各自 SD）。
> 特性：**按鍵級查詢頻率**（每敲一鍵一次），比一般搜尋高一個量級，延遲要極低。靠「熱門前綴預生成 + 前端 debounce」把真正打到後端的量壓下來。

## 1. Context Diagram

```text
[使用者打字] --(前綴: prefix)--> (Autocomplete) --(候選詞列表)--> [使用者]

(Autocomplete) --(前綴 → 候選)--> [util.redis / 前綴索引]
(Autocomplete) --(熱門前綴預生成)--> [預生成快取]
```

## 2. DFD（精簡 box flow）

```text
[打字] --(每鍵 prefix；前端 debounce 削掉部分)--> [查候選]
   ├─(熱門前綴)──> 命中預生成快取 → 直接回（最快）
   │(非熱門)
   ▼
[前綴索引查詢]（Trie / 前綴匹配）── 取 top-K 候選（依熱度 / 個人化）
   ▼
回候選列表（極低延遲）
```

## 3. Process Spec（行為基準，decision table）

```text
[前端 debounce]（第一道削量）
└─ 打字停頓才送（如 100~200ms）；不是每個鍵都打後端

[熱門前綴預生成]
├─ 熱門前綴（如「iph」→ iphone…）候選固定 → 預先算好放快取
└─ 命中 → 直接回

[前綴查詢]
└─ 非熱門 → 前綴索引取 top-K（依熱度 / 個人化加權）

[延遲]（核心天條）
└─ 必須跟得上手速（極低延遲）；重個人化計算不放臨場
```

## 4. State Transition

```text
（查詢型，無業務狀態機；前綴查詢的 命中預生成 / 索引查詢 路徑）
```

## 5. Data Dictionary（邏輯欄位；實體 DDL 在各平台 SD）

```text
suggestion（候選詞）
├─ prefix / term / score（熱度）—— 前綴索引 / Trie

hot_prefix_cache（熱門前綴預生成，活在 Redis）
├─ prefix(PK) / candidates[]（預算好的 top-K）
```
