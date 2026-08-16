# 示例四：供应链库存补货

场景：补货执行 Agent 处理库存补货任务——从 MetaForge 获取库存/供应商/补货单三业务对象结构、补货触发规则与供应商择优规则，读取独立存放的库存快照与供应商数据，对照阈值判断补货、择优选择供应商。

## 场景概述

- **Agent 角色**：补货执行 Agent
- **业务目标**：检查库存，低于补货阈值则触发补货并择优选择供应商
- **MetaForge 角色**：提供库存/供应商/补货单结构 + 补货阈值 + 供应商择优语义

## 语义说明书（存 MetaForge）

### 业务对象结构（L3-L5）

```
库存管理域 (Domain_InventoryMgmt)
├── 库存业务对象 (BO_Inventory, L3)
│   └── Attr_SkuId / Attr_StockQty(件) / Attr_ReorderLevel(件) / Attr_DailyDemand / Attr_MaxStock
├── 供应商业务对象 (BO_Supplier, L3)
│   └── Attr_SupplierId / Attr_LeadTime(天) / Attr_ReliabilityScore(分) / Attr_UnitPrice(元)
└── 补货单业务对象 (BO_PurchaseOrder, L3)
    └── Attr_OrderId / Attr_OrderQuantity / Attr_OrderStatus
```

### 任务与流程（决策嵌套）

```
库存补货任务 (Task_Replenishment)
  读取库存(ENTRY) → 补货判断决策(DECISION) → 库存≤阈值→供应商选择 / 库存>阈值→无需补货

供应商选择子任务（决策嵌套）：
  获取供应商 → 供应商择优决策(DECISION) → 可靠性≥90→优选供应商 / 否则→性价比供应商
  → 生成补货单
```

### 规则阈值（语义化）

| 规则 | 级别 | 约束字段 | 条件 |
|------|------|---------|------|
| Rule_ReorderRule | MANDATORY | Attr_StockQty / Attr_ReorderLevel | 库存≤补货阈值 → 生成补货单 |
| Rule_SupplierPriority | RECOMMENDED | Attr_ReliabilityScore | 可靠性≥90 → 选优选供应商 |
| Rule_MaxStock | RECOMMENDED | Attr_MaxStock | 补货量≤最大库存 |

## 业务数据（独立存放）

```
examples/example-supply-chain-replenishment/data/inventory/
├── sku-A01.json   { stock_qty: 15, reorder_level: 20, max_stock: 60 }
├── sku-B02.json   { stock_qty: 45, reorder_level: 20, max_stock: 60 }
└── sku-C03.json   { stock_qty: 20, reorder_level: 20, max_stock: 80 }

examples/example-supply-chain-replenishment/data/suppliers/
├── sup-01.json   { reliability_score: 95, lead_time: 3, unit_price: 12 }   # 优选
└── sup-02.json   { reliability_score: 75, lead_time: 5, unit_price: 9 }    # 性价比
```

## 决策链路（Agent 对照说明书执行）

```
1. BRIEF 拿说明书：补货阈值规则（库存≤阈值触发）+ 供应商择优规则（可靠性≥90）
2. 读 sku-C03.json → 库存 20 == 阈值 20（等于阈值）→ 按"≤"规则触发补货
3. 读 sup-01/sup-02 → 可靠性 95≥90 → 选优选供应商 SUP-01
```

## 关键结论

| SKU | 库存 | 阈值 | 判定 | 供应商 |
|-----|------|------|------|--------|
| SKU-A01 | 15 | 20 | ✅ 补货（15≤20） | SUP-01（可靠性95 优选） |
| SKU-B02 | 45 | 20 | ❌ 无需（45>20） | — |
| SKU-C03 | 20 | 20 | ✅ **等于阈值触发** | SUP-01（可靠性95 优选） |

亮点：**等于阈值触发**（stock_qty == reorder_level 按"≤"计入）体现边界语义精确；**供应商择优**按可靠性 95/75 分流（优选 vs 性价比）。

## 快速验证

```bash
cd /data/ext/source-8/metaforge
opencode run "你是补货执行 Agent。用 metaforge_cognition（BRIEF）查 Task_Replenishment 拿说明书（库存≤阈值触发补货）。读 examples/example-supply-chain-replenishment/data/inventory/sku-C03.json，报告是否补货及依据（特别注意等于阈值）。"
```

预期：SKU-C03（20==20）→ 触发补货。
