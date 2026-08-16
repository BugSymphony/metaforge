# example-supply-chain-replenishment 测试用例：供应链库存补货

## 测试环境

- MetaForge 服务端：`http://localhost:8080`（V4 含 `TaskProcessesBusinessObject` + `RuleConstrainsAttribute`）
- seed 已应用：`test/cognition/seed/agent-library-seed.sql` + `examples/example-supply-chain-replenishment/seed-supply-chain.sql`
- 业务数据：`examples/example-supply-chain-replenishment/data/inventory/{sku-A01,B02,C03}.json` + `data/suppliers/{sup-01,sup-02}.json`
- 测试目录：`/data/ext/source-8/metaforge`

---

## SC-1 域定位：供应链域进入域树（含 3 业务对象）

**命令**：
```bash
cd /data/ext/source-8/metaforge
opencode run "用 metaforge_cognition（ORIENT）下钻『供应链域组』报告下面有哪些域，再下钻库存管理域报告其成员。"
```

**预期**：库存管理域下含 **库存 + 供应商 + 补货单** 3 个业务对象（L3）+ Agent + 库存补货任务。

**通过标准**：3 个 L3 业务对象出现在域树下钻结果。

---

## SC-2 任务 → 业务对象关系（TaskProcessesBusinessObject）

**命令**：
```bash
cd /data/ext/source-8/metaforge
opencode run "用 metaforge_cognition（BRIEF）分别查询『库存补货任务』『供应商选择子任务』，报告各自处理的业务对象。"
```

**预期**：补货任务→BO_Inventory；供应商选择子任务→BO_Supplier + BO_PurchaseOrder。

**通过标准**：任务正确处理各自业务对象。

---

## SC-3 业务对象字段结构（3 张表 L3-L5）

**命令**：
```bash
cd /data/ext/source-8/metaforge
opencode run "用 metaforge_cognition 展开『库存』『供应商』『补货单』3 个业务对象的结构（BO→LE→字段），报告字段名、类型、单位。"
```

**预期**：
- 库存：sku_id / stock_qty(件) / reorder_level(件) / daily_demand / max_stock
- 供应商：supplier_id / lead_time(天) / reliability_score(分) / unit_price(元)
- 补货单：order_id / quantity(件) / status

**通过标准**：3 张表完整字段结构来自 L3-L5 元数据。

---

## SC-4 规则 → 字段关系（RuleConstrainsAttribute）

**命令**：
```bash
cd /data/ext/source-8/metaforge
opencode run "用 metaforge_cognition（GUIDE）查询『补货触发规则』『供应商优先规则』，报告各自约束哪些字段。"
```

**预期**：Rule_ReorderRule→stock_qty+reorder_level；Rule_SupplierPriority→reliability_score。

**通过标准**：规则绑定到具体字段。

---

## SC-5 补货判断多分支决策

**命令**：
```bash
cd /data/ext/source-8/metaforge
opencode run "用 metaforge_cognition（BRIEF）查『库存补货任务』流程，再（GUIDE）查『补货判断决策』的分支。报告流程和分支。"
```

**预期**：流程 读取库存→补货判断决策→…；分支：供应商选择（库存≤阈值）、无需补货（库存>阈值）。

**通过标准**：补货判断决策正确。

---

## SC-6 供应商择优决策嵌套

**命令**：
```bash
cd /data/ext/source-8/metaforge
opencode run "用 metaforge_cognition（BRIEF）查『供应商选择子任务』流程，再（GUIDE）查『供应商择优决策』的分支。报告流程和分支。"
```

**预期**：流程 获取供应商→供应商择优决策→优选/性价比→生成补货单；分支：可靠性≥90→优选、<90→性价比。

**通过标准**：决策嵌套（子任务内再嵌决策）正确。

---

## SC-7 需补货 SKU（SKU-A01，选优选供应商）

**命令**：
```bash
cd /data/ext/source-8/metaforge
opencode run "你是补货执行 Agent。用 metaforge_cognition 了解补货触发规则（库存≤补货阈值触发）与供应商择优规则（可靠性≥90 选优选）。读 examples/example-supply-chain-replenishment/data/inventory/sku-A01.json 判断是否补货；若补货读 suppliers/sup-01.json（可靠性95）、sup-02.json（可靠性75）选供应商。报告结论与依据。"
```

**预期**：库存 15 ≤ 阈值 20 → **补货**；选 SUP-01（可靠性 95 ≥ 90 优选）。

---

## SC-8 充足 SKU（SKU-B02，无需补货）

**命令**：
```bash
cd /data/ext/source-8/metaforge
opencode run "同上了解规则，读 examples/example-supply-chain-replenishment/data/inventory/sku-B02.json，报告是否补货及依据。"
```

**预期**：库存 45 > 阈值 20 → **无需补货**。

---

## SC-9 临界 SKU（SKU-C03，等于阈值触发）—— 核心边界用例

**命令**：
```bash
cd /data/ext/source-8/metaforge
opencode run "同上流程，读 examples/example-supply-chain-replenishment/data/inventory/sku-C03.json，报告是否补货及依据。特别注意：库存是否等于阈值，等于是否触发补货？"
```

**预期**：库存 20 **==** 阈值 20（等于阈值）→ 按"≤"规则 **触发补货**（体现边界语义）。

**通过标准**：正确区分"等于阈值触发"。

---

## SC-10 供应商择优对照（SUP-01 优选 vs SUP-02 性价比）

**命令**：
```bash
cd /data/ext/source-8/metaforge
opencode run "用 metaforge_cognition 了解供应商择优规则（可靠性≥90 选优选，否则按性价比）。读 examples/example-supply-chain-replenishment/data/suppliers/sup-01.json 和 sup-02.json，报告两个供应商各自对应的选择策略。"
```

**预期**：SUP-01（可靠性95）→ 优选供应商；SUP-02（可靠性75）→ 性价比供应商（价格/交期权衡）。

**通过标准**：可靠性与性价比两种策略正确区分。

---

## 测试记录表

| 用例 | 结果（PASS/FAIL） | 备注 |
|------|------------------|------|
| SC-1 域定位（3 业务对象） | | |
| SC-2 任务→业务对象 | | |
| SC-3 字段结构（3 表） | | |
| SC-4 规则→字段 | | |
| SC-5 补货判断决策 | | |
| SC-6 供应商择优嵌套 | | |
| SC-7 SKU-A01 补货→优选 | | |
| SC-8 SKU-B02 充足 | | |
| SC-9 SKU-C03 临界触发（核心） | | |
| SC-10 供应商策略对照 | | |

---

## 验收结论（写在此处）

> 本测试证明：MetaForge 存储 3 个业务对象结构（L3-L5）+ 补货/供应商规则的**说明书**，
> 业务数据（库存快照/供应商）独立存放；Agent 依据说明书执行补货决策——
> **同一说明书对 3 个 SKU 产生 需补货/无需补货/等于阈值触发 三种判定**，
> 并支持决策嵌套（供应商择优按可靠性 95/75 分流），所有阈值/字段/关系来自 metaforge 而非模型先验。
