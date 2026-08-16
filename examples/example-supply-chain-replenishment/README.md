# 供应链库存补货（example-supply-chain-replenishment）

**场景概述**：补货执行 Agent 处理库存补货任务——从 MetaForge 获取库存/供应商/补货单三业务对象结构、补货触发规则（库存≤补货阈值）与供应商择优规则（可靠性≥90 选优选，否则按性价比），读取独立存放的库存快照与供应商数据（sku-A01/B02/C03 + sup-01/02），对照阈值判断是否补货、择优选择供应商并生成补货单。展示"说明书存 MetaForge、业务数据独立、Agent 结合执行"。

核心验证：**MetaForge 存储"数据说明书"（3 个业务对象表结构 + 任务流程 + 补货/供应商规则 + 决策语义），业务数据（库存快照/供应商）独立存放**，Agent 结合两者执行多分支补货决策。

## 目录结构

```
example-supply-chain-replenishment/
├── README.md                  # 本文件
├── seed-supply-chain.sql      # 供应链域元数据（3 业务对象 + 决策嵌套 + 3 规则）
├── data/
│   ├── inventory/             # 库存快照（独立存放）
│   │   ├── sku-A01.json       #   需补货（15≤20）
│   │   ├── sku-B02.json       #   充足（45>20）
│   │   └── sku-C03.json       #   临界（20==20）
│   └── suppliers/             # 供应商
│       ├── sup-01.json        #   可靠性95（优选）
│       └── sup-02.json        #   可靠性75（性价比）
└── test-cases.md              # 测试用例（SC-1 ~ SC-10）
```

## 复杂度特性

| 维度 | 实现 |
|------|------|
| **3 个业务对象** | 库存表 + 供应商表 + 补货单表（L3-L5） |
| **决策嵌套** | 补货判断 → 供应商择优（可靠性门槛） |
| **补货阈值规则** | 库存≤补货阈值 触发补货（含等于阈值触发） |
| **供应商优先规则** | 可靠性≥90 选优选，否则按性价比 |
| **最大库存约束** | 补货量≤最大库存 |

## 数据模型

### 业务对象结构（L3-L5 元数据）

```
供应链域组 (Group_SupplyChain, L1)
└── 库存管理域 (Domain_InventoryMgmt, L2)
    ├── 库存业务对象 (BO_Inventory, L3)
    │   └── LE_Inventory (L4)
    │       ├── Attr_SkuId / Attr_StockQty / Attr_ReorderLevel / Attr_DailyDemand / Attr_MaxStock
    ├── 供应商业务对象 (BO_Supplier, L3)
    │   └── LE_Supplier (L4)
    │       ├── Attr_SupplierId / Attr_LeadTime / Attr_ReliabilityScore / Attr_UnitPrice
    └── 补货单业务对象 (BO_PurchaseOrder, L3)
        └── LE_PurchaseOrder (L4)
            ├── Attr_OrderId / Attr_OrderQuantity / Attr_OrderStatus
```

### 流程拓扑

```
Task_Replenishment（库存补货主任务）
  Step_ReadInventory(ENTRY)
    → DecisionStep_StockCheck（决策1：补货判断）
         ├─ 库存≤补货阈值 → Task_SupplierSelection（供应商选择子任务）
         └─ 库存>阈值 → Step_NoReplenish(EXIT)

Task_SupplierSelection（供应商选择，决策嵌套）
  Step_FetchSuppliers → DecisionStep_SupplierPick（决策2：供应商择优）
       ├─ 可靠性≥90 → Step_SelectPreferredSupplier（优选）
       └─ 可靠性<90 → Step_SelectBestValueSupplier（性价比）
  → Step_GeneratePurchaseOrder（生成补货单，→BO_PurchaseOrder）
```

### 规则（3 条）

| 规则 | 级别 | 约束字段 | 触发 |
|------|------|---------|------|
| Rule_ReorderRule | MANDATORY | Attr_StockQty / Attr_ReorderLevel | 库存≤补货阈值 → 生成补货单 |
| Rule_SupplierPriority | RECOMMENDED | Attr_ReliabilityScore | 可靠性≥90 → 选优选供应商 |
| Rule_MaxStock | RECOMMENDED | Attr_MaxStock | 补货量≤最大库存 |

## 快速开始

```bash
# 1. 应用 seed（V4 关系 schema 已就绪）
export PGPASSWORD=metaforge
psql -h localhost -U metaforge -d metaforge -f examples/example-supply-chain-replenishment/seed-supply-chain.sql

# 2. 端到端执行（多 SKU 补货 + 供应商择优）
cd /data/ext/source-8/metaforge
opencode run "你是补货执行 Agent。用 metaforge_cognition（BRIEF）查 Task_Replenishment 拿说明书（库存字段/补货阈值规则 库存≤阈值触发）；了解 Task_SupplierSelection 的供应商择优规则（可靠性≥90 选优选）。依次读 examples/example-supply-chain-replenishment/data/inventory/ 下 sku-A01/B02/C03 对照阈值判断补货（含等于阈值触发）；对需补货 SKU 读 suppliers/sup-01、sup-02 按择优规则选供应商。表格汇总：SKU、库存、阈值、是否补货、选择供应商及原因。"
```

## 预期结果（多 SKU）

| SKU | 库存 | 阈值 | 是否补货 | 供应商 |
|-----|------|------|---------|--------|
| SKU-A01 | 15 | 20 | ✅（15≤20） | SUP-01（可靠性95 优选） |
| SKU-B02 | 45 | 20 | ❌（45>20） | — |
| SKU-C03 | 20 | 20 | ✅（等于阈值） | SUP-01（可靠性95 优选） |

详见 [test-cases.md](test-cases.md)。
