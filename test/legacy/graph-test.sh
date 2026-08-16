#!/usr/bin/env bash
# =============================================================================
# MetaForge graph BC 复杂数据端到端测试脚本
#
# 设计依据: specs/001-relation-instance-lifecycle/quickstart.md
#   覆盖场景 1/2/3/4/5/7/8/9（场景 6 事件发布为集成测试，跳过），
#   并针对当前实现的实际 REST 端点编写。
#
# 与 quickstart 的差异（实现 vs 文档）:
#   * 所有响应恒为 HTTP 200，业务错误码在 body.code 字段
#   * POST /drafts/from-active 使用 query 参数 fqn，而非 JSON body
#   * 实际错误码: FQN_CONFLICT=32001（重复草稿）、RELATION_NOT_FOUND=32002、
#     ENDPOINT_INVALID=32007、SCHEMA_NOT_PUBLISHED=32010、
#     CARDINALITY_EXCEEDED=32006、DEPENDENCY_BLOCKING=32008、ILLEGAL_STATE=32015
#   * 新草稿 baseVersion 为 null 时 JSON 字段被省略（非显式 null）
#
# 数据复杂度:
#   * 元模型: erp bundle v1.0.0，2 个 package，5 类 EntitySchema，
#     5 种关联类型（组成/关联引用/映射对应/依赖影响/流程时序），
#     多种基数组合（1:1、1:N、N:N）
#   * 元数据: 6 个生效实体（跨 3 类 schema，含 pattern/enum 约束内容）
#   * 图: 多条跨类型关系、多版本（v1..v3）、导出/导入、拓扑校验、依赖阻塞
#
# 前置: 应用已在 8080 运行（metamodel/metadata/graph BC），PostgreSQL 在 5432
# 用法:
#   ./graph-test.sh           全量运行
#   MF_KEEP_DATA=1 ./graph-test.sh   运行前不清理旧数据（默认清理 erp bundle）
# =============================================================================
set -u

MF_BASE_URL="${MF_BASE_URL:-http://localhost:8080}"
PG="psql -h localhost -U metaforge -d metaforge"
export PGPASSWORD="${PGPASSWORD:-metaforge}"

BUNDLE="erp"
BV0="erp:0.0.1"
BV="erp:1.0.0"
PKG_CORE="erp:0.0.1.pkg_core"
PKG_SALES="erp:0.0.1.pkg_sales"
SCHEMA_ORDER="erp:0.0.1.pkg_sales.Order"
SCHEMA_ITEM="erp:0.0.1.pkg_sales.OrderItem"
SCHEMA_CUSTOMER="erp:0.0.1.pkg_core.Customer"
SCHEMA_SUPPLIER="erp:0.0.1.pkg_core.Supplier"
SCHEMA_WAREHOUSE="erp:0.0.1.pkg_sales.Warehouse"

PASS=0; FAIL=0; FAILED_CASES=()

jqget() { python3 -c "import json,sys; d=json.load(sys.stdin); print(d$1)" 2>/dev/null; }

check() { # check <名称> <实际> <期望>
  if [ "$2" = "$3" ]; then PASS=$((PASS+1)); echo "  [PASS] $1 (=$2)";
  else FAIL=$((FAIL+1)); FAILED_CASES+=("$1: 期望 $3 实际 $2"); echo "  [FAIL] $1: 期望=$3 实际=$2"; fi
}

check_contains() { # check_contains <名称> <haystack> <needle>
  if echo "$2" | grep -q "$3"; then PASS=$((PASS+1)); echo "  [PASS] $1";
  else FAIL=$((FAIL+1)); FAILED_CASES+=("$1: 未包含 $3"); echo "  [FAIL] $1: 未包含 [$3]"; fi
}

check_ge() { # check_ge <名称> <实际数值> <最小期望>
  if [ "$2" -ge "$3" ] 2>/dev/null; then PASS=$((PASS+1)); echo "  [PASS] $1 (=$2 >= $3)";
  else FAIL=$((FAIL+1)); FAILED_CASES+=("$1: 期望 >=$3 实际 $2"); echo "  [FAIL] $1: 期望 >=$3 实际=$2"; fi
}

check_not_contains() { # check_not_contains <名称> <haystack> <needle>
  if echo "$2" | grep -q "$3"; then FAIL=$((FAIL+1)); FAILED_CASES+=("$1: 意外包含 $3"); echo "  [FAIL] $1: 意外包含 [$3]";
  else PASS=$((PASS+1)); echo "  [PASS] $1"; fi
}

api() { # api <method> <path> [body]  -> 输出 body
  local method="$1" path="$2" body="${3:-}"
  if [ -n "$body" ]; then
    curl -s -X "$method" "$MF_BASE_URL$path" -H "Content-Type: application/json" -d "$body"
  else
    curl -s -X "$method" "$MF_BASE_URL$path"
  fi
}

urlenc() { python3 -c "import urllib.parse,sys; print(urllib.parse.quote(sys.argv[1], safe=''))" "$1"; }

echo "==================================================================="
echo " 0. 环境探测"
echo "==================================================================="
code=$(curl -s "$MF_BASE_URL/actuator/health" | jqget "['status']")
check "应用健康检查" "$code" "UP"

echo ""
echo "==================================================================="
echo " 1. 元模型前置：erp bundle 全量建模（v1.0.0）"
echo "==================================================================="

if [ "${MF_KEEP_DATA:-}" != "1" ]; then
  echo "  [清理] 清理测试数据（graph/metadata/metamodel 三域）..."
  $PG -c "DELETE FROM semantic_relation_network.entity_relation_index WHERE relation_fqn LIKE 'Order_10%' OR relation_fqn LIKE 'Order_20%' OR relation_fqn LIKE 'Customer_00%' OR relation_fqn LIKE 'Supplier_00%' OR relation_fqn LIKE 'OrderItem_10%' OR relation_fqn LIKE 'Warehouse_30%' OR relation_fqn LIKE 'Seeder#%';" >/dev/null 2>&1
  $PG -c "DELETE FROM semantic_relation_network.relation_version WHERE fqn LIKE 'Order_10%' OR fqn LIKE 'Order_20%' OR fqn LIKE 'Customer_00%' OR fqn LIKE 'Supplier_00%' OR fqn LIKE 'OrderItem_10%' OR fqn LIKE 'Warehouse_30%' OR fqn LIKE 'Seeder#%';" >/dev/null 2>&1
  $PG -c "DELETE FROM semantic_relation_network.relation_instance WHERE fqn LIKE 'Order_10%' OR fqn LIKE 'Order_20%' OR fqn LIKE 'Customer_00%' OR fqn LIKE 'Supplier_00%' OR fqn LIKE 'OrderItem_10%' OR fqn LIKE 'Warehouse_30%' OR fqn LIKE 'Seeder#%';" >/dev/null 2>&1
  $PG -c "DELETE FROM semantic_relation_network.relation_instance_draft WHERE fqn LIKE 'Order_10%' OR fqn LIKE 'Order_20%' OR fqn LIKE 'Customer_00%' OR fqn LIKE 'Supplier_00%' OR fqn LIKE 'OrderItem_10%' OR fqn LIKE 'Warehouse_30%' OR fqn LIKE 'Seeder#%';" >/dev/null 2>&1
  $PG -c "DELETE FROM metadata_management.metadata_entity_draft WHERE fqn IN ('Customer_001','Customer_002','Supplier_001','Order_100','Order_200','OrderItem_101','OrderItem_102','Warehouse_301');" >/dev/null 2>&1
  $PG -c "DELETE FROM metadata_management.entity_version WHERE fqn IN ('Customer_001','Customer_002','Supplier_001','Order_100','Order_200','OrderItem_101','OrderItem_102','Warehouse_301');" >/dev/null 2>&1
  $PG -c "DELETE FROM metadata_management.metadata_entity WHERE fqn IN ('Customer_001','Customer_002','Supplier_001','Order_100','Order_200','OrderItem_101','OrderItem_102','Warehouse_301');" >/dev/null 2>&1
  $PG -c "DELETE FROM metamodel_governance.relation_schema WHERE fqn LIKE 'erp%';" >/dev/null 2>&1
  $PG -c "DELETE FROM metamodel_governance.entity_schema WHERE fqn LIKE 'erp%';" >/dev/null 2>&1
  $PG -c "DELETE FROM metamodel_governance.package WHERE fqn LIKE 'erp%';" >/dev/null 2>&1
  $PG -c "DELETE FROM metamodel_governance.export_manifest WHERE bundle_version_fqn LIKE 'erp%';" >/dev/null 2>&1
  $PG -c "DELETE FROM metamodel_governance.bundle_dependency WHERE source_version_fqn LIKE 'erp%' OR target_version_fqn LIKE 'erp%';" >/dev/null 2>&1
  $PG -c "DELETE FROM metamodel_governance.bundle_version WHERE fqn LIKE 'erp%';" >/dev/null 2>&1
  $PG -c "DELETE FROM metamodel_governance.bundle WHERE fqn='erp';" >/dev/null 2>&1
fi

# 1.1 创建 bundle
r=$(api POST /api/v1/metamodel/bundles "{\"fqn\":\"$BUNDLE\",\"name\":\"企业资源计划\",\"description\":\"含订单/库存/采购的复杂域\",\"owner\":\"lisi\"}")
check "创建 bundle $BUNDLE" "$(echo "$r" | jqget "['code']")" "200"
# bundle 创建失败（已存在）时兜底
if [ "$(echo "$r" | jqget "['code']")" != "200" ]; then
  echo "  [跳过] bundle 已存在，复用"
fi

# 1.2 创建 package（2 个，挂到初始草稿 erp:0.0.1）
r=$(api POST /api/v1/metamodel/packages "{\"bundleVersionFqn\":\"$BV0\",\"parentPackageFqn\":null,\"segment\":\"pkg_core\",\"description\":\"主数据\"}")
check "创建 pkg_core" "$(echo "$r" | jqget "['code']")" "200"
r=$(api POST /api/v1/metamodel/packages "{\"bundleVersionFqn\":\"$BV0\",\"parentPackageFqn\":null,\"segment\":\"pkg_sales\",\"description\":\"交易域\"}")
check "创建 pkg_sales" "$(echo "$r" | jqget "['code']")" "200"

# 1.3 创建 5 个 EntitySchema（含 pattern/enum/min 约束）
mk_entity() { # mk_entity <pkg> <seg> <name> <attrs-json>
  api POST /api/v1/metamodel/entity-schemas "{\"packageFqn\":\"$1\",\"segment\":\"$2\",\"name\":\"$3\",\"description\":\"$3 schema\",\"nativeAttributes\":$4}"
}
r=$(mk_entity "$PKG_CORE" "Customer" "客户" '[{"name":"customerCode","type":"string","required":true,"constraints":{"pattern":"^C-[0-9]{4,}$"}},{"name":"name","type":"string","required":true},{"name":"vipLevel","type":"string","required":true,"constraints":{"enum":["GOLD","SILVER","BRONZE"]}}]')
check "EntitySchema Customer" "$(echo "$r" | jqget "['code']")" "200"
r=$(mk_entity "$PKG_CORE" "Supplier" "供应商" '[{"name":"supplierCode","type":"string","required":true,"constraints":{"pattern":"^S-[0-9]{4,}$"}},{"name":"name","type":"string","required":true}]')
check "EntitySchema Supplier" "$(echo "$r" | jqget "['code']")" "200"
r=$(mk_entity "$PKG_SALES" "Order" "订单" '[{"name":"orderNo","type":"string","required":true,"constraints":{"pattern":"^SO-[0-9]{6,}$"}},{"name":"status","type":"string","required":true,"constraints":{"enum":["active","cancelled","shipped"]}},{"name":"amount","type":"number","required":true,"constraints":{"minimum":0}}]')
check "EntitySchema Order" "$(echo "$r" | jqget "['code']")" "200"
r=$(mk_entity "$PKG_SALES" "OrderItem" "订单项" '[{"name":"sku","type":"string","required":true,"constraints":{"pattern":"^[A-Z]{2}-[0-9]{4,}$"}},{"name":"quantity","type":"integer","required":true,"constraints":{"minimum":1}}]')
check "EntitySchema OrderItem" "$(echo "$r" | jqget "['code']")" "200"
r=$(mk_entity "$PKG_SALES" "Warehouse" "仓库" '[{"name":"warehouseCode","type":"string","required":true,"constraints":{"pattern":"^WH-[0-9]{3,}$"}},{"name":"capacity","type":"integer","required":true,"constraints":{"minimum":100}}]')
check "EntitySchema Warehouse" "$(echo "$r" | jqget "['code']")" "200"

# 1.4 创建 5 种关联类型的 RelationSchema（根级 package = bundle 版本直接加段名）
mk_rel() { # mk_rel <seg> <source> <target> <assoc> <srcCard> <tgtCard>
  api POST /api/v1/metamodel/relation-schemas "{\"packageFqn\":\"$BV0\",\"segment\":\"$1\",\"name\":\"$1\",\"description\":\"$1 schema\",\"sourceFqn\":\"$2\",\"targetFqn\":\"$3\",\"associationType\":\"$4\",\"cardinalitySource\":\"$5\",\"cardinalityTarget\":\"$6\"}"
}
r=$(mk_rel "COMPOSITION"            "$SCHEMA_ORDER"    "$SCHEMA_ITEM"      "组成"       "1"   "1..*")
check "RelationSchema COMPOSITION" "$(echo "$r" | jqget "['code']")" "200"
r=$(mk_rel "ASSOCIATION_REFERENCE"  "$SCHEMA_CUSTOMER" "$SCHEMA_ORDER"     "关联引用"   "1"   "0..*")
check "RelationSchema ASSOCIATION_REFERENCE" "$(echo "$r" | jqget "['code']")" "200"
r=$(mk_rel "MAPPING_CORRESPONDENCE" "$SCHEMA_ITEM"     "$SCHEMA_WAREHOUSE" "映射对应"   "0..*" "0..*")
check "RelationSchema MAPPING_CORRESPONDENCE" "$(echo "$r" | jqget "['code']")" "200"
r=$(mk_rel "DEPENDENCY_INFLUENCE"   "$SCHEMA_ORDER"    "$SCHEMA_SUPPLIER"  "依赖影响"   "0..*" "0..*")
check "RelationSchema DEPENDENCY_INFLUENCE" "$(echo "$r" | jqget "['code']")" "200"
r=$(mk_rel "PROCESS_SEQUENCE"       "$SCHEMA_ORDER"    "$SCHEMA_ORDER"     "流程时序"   "0..*" "0..*")
check "RelationSchema PROCESS_SEQUENCE" "$(echo "$r" | jqget "['code']")" "200"

# 1.5 配置导出清单 + 发布（v0.0.1 -> MAJOR -> v1.0.0，内容随版本复制自动重键）
r=$(api PUT /api/v1/metamodel/versions/erp:0.0.1/export-manifest "{\"packageFqns\":[\"$PKG_CORE\",\"$PKG_SALES\"]}")
check "配置 export-manifest 0.0.1" "$(echo "$r" | jqget "['code']")" "200"
r=$(api POST /api/v1/metamodel/versions/erp:0.0.1/publish)
check "发布 erp:0.0.1" "$(echo "$r" | jqget "['code']")" "200"
r=$(api POST /api/v1/metamodel/bundles/$BUNDLE/versions "{\"upgradeLevel\":\"MAJOR\"}")
check "创建 erp:1.0.0 草稿（内容全量复制）" "$(echo "$r" | jqget "['code']")" "200"
r=$(api POST /api/v1/metamodel/versions/erp:1.0.0/publish)
check "发布 erp:1.0.0" "$(echo "$r" | jqget "['code']")" "200"
r=$(api GET /api/v1/metamodel/relation-schemas/erp:1.0.0.COMPOSITION)
check "COMPOSITION schema enabled" "$(echo "$r" | jqget "['data']['enabled']")" "True"
r=$(api GET /api/v1/metamodel/entity-schemas/erp:1.0.0.pkg_sales.Order)
check "1.0.0 实体 schema 存在" "$(echo "$r" | jqget "['code']")" "200"

# 后续所有元数据/图操作针对已发布的 1.0.0 版本 FQN
SCHEMA_ORDER="erp:1.0.0.pkg_sales.Order"
SCHEMA_ITEM="erp:1.0.0.pkg_sales.OrderItem"
SCHEMA_CUSTOMER="erp:1.0.0.pkg_core.Customer"
SCHEMA_SUPPLIER="erp:1.0.0.pkg_core.Supplier"
SCHEMA_WAREHOUSE="erp:1.0.0.pkg_sales.Warehouse"

echo ""
echo "==================================================================="
echo " 2. 元数据前置：6 个生效实体（跨 3 类 schema）"
echo "==================================================================="
mk_entity_instance() { # mk_entity_instance <fqn> <schemaFqn> <content-json>
  local code act
  code=$(api POST /api/v1/metadata/drafts "{\"fqn\":\"$1\",\"name\":\"$1\",\"entitySchemaFqn\":\"$2\",\"content\":$3}" | jqget "['code']")
  act=$(api POST /api/v1/metadata/entities/$1/activate | jqget "['code']")
  if [ "$code" != "200" ] || [ "$act" != "200" ]; then
    echo "  [WARN] 实体 $1 创建(draft=$code)或激活(act=$act)失败" >&2
    echo "failed"
  else
    echo "$code"
  fi
}
r=$(mk_entity_instance "Customer_001" "$SCHEMA_CUSTOMER" '{"customerCode":"C-1001","name":"张三","vipLevel":"GOLD"}')
check "实体 Customer_001" "$r" "200"
r=$(mk_entity_instance "Customer_002" "$SCHEMA_CUSTOMER" '{"customerCode":"C-1002","name":"李四","vipLevel":"SILVER"}')
check "实体 Customer_002" "$r" "200"
r=$(mk_entity_instance "Supplier_001" "$SCHEMA_SUPPLIER" '{"supplierCode":"S-2001","name":"原料供应商"}')
check "实体 Supplier_001" "$r" "200"
r=$(mk_entity_instance "Order_100" "$SCHEMA_ORDER" '{"orderNo":"SO-100001","status":"active","amount":1999.5}')
check "实体 Order_100" "$r" "200"
r=$(mk_entity_instance "Order_200" "$SCHEMA_ORDER" '{"orderNo":"SO-100002","status":"shipped","amount":899.0}')
check "实体 Order_200" "$r" "200"
r=$(mk_entity_instance "OrderItem_101" "$SCHEMA_ITEM" '{"sku":"AB-12345","quantity":3}')
check "实体 OrderItem_101" "$r" "200"
r=$(mk_entity_instance "OrderItem_102" "$SCHEMA_ITEM" '{"sku":"CD-67890","quantity":5}')
check "实体 OrderItem_102" "$r" "200"
r=$(mk_entity_instance "Warehouse_301" "$SCHEMA_WAREHOUSE" '{"warehouseCode":"WH-301","capacity":5000}')
check "实体 Warehouse_301" "$r" "200"

echo ""
echo "==================================================================="
echo " 3. 场景 1：草稿创建与编辑（复杂内容 + 重复拦截 + from-active）"
echo "==================================================================="
# Step 1: 创建 COMPOSITION 草稿（Order_100 -> OrderItem_101）
r=$(api POST /api/v1/graph/drafts '{
  "sourceEntityFqn":"Order_100","relationTypeFqn":"erp:1.0.0.COMPOSITION","targetEntityFqn":"OrderItem_101",
  "name":"订单100包含条目101","description":"复杂组成关系","content":{"quantity":3,"unitPrice":199.5,"lineNote":"首发"} }')
DRAFT_FQN="Order_100#erp:1.0.0.COMPOSITION#OrderItem_101"
check "创建 COMPOSITION 草稿" "$(echo "$r" | jqget "['code']")" "200"
check "草稿 FQN 格式" "$(echo "$r" | jqget "['data']['fqn']")" "$DRAFT_FQN"
check_not_contains "baseVersion=null(字段省略)" "$r" "baseVersion"

# Step 2: 更新草稿内容（增加字段）
r=$(api PUT "/api/v1/graph/drafts/$(urlenc "$DRAFT_FQN")/content" '{"content":{"quantity":3,"unitPrice":299.9,"discount":0.9,"memo":{"inner":"嵌套对象","tags":["a","b"]}}}')
check "更新草稿内容" "$(echo "$r" | jqget "['code']")" "200"
check "content 更新成功(quantity)" "$(echo "$r" | jqget "['data']['content']['quantity']")" "3"
check "content 新增嵌套字段" "$(echo "$r" | jqget "['data']['content']['memo']['inner']")" "嵌套对象"

# Step 3: 重复创建（草稿已存在或实例已存在 -> 32001 FQN_CONFLICT）
r=$(api POST /api/v1/graph/drafts '{
  "sourceEntityFqn":"Order_100","relationTypeFqn":"erp:1.0.0.COMPOSITION","targetEntityFqn":"OrderItem_101",
  "name":"重复草稿","description":"dup","content":{"x":1} }')
check "重复草稿拦截(32001)" "$(echo "$r" | jqget "['code']")" "32001"

echo ""
echo "==================================================================="
echo " 4. 场景 2：草稿生效（四步原子事务）"
echo "==================================================================="
r=$(api POST /api/v1/graph/relations/activate "{\"fqn\":\"$DRAFT_FQN\"}")
check "激活草稿" "$(echo "$r" | jqget "['code']")" "200"
check "激活后 currentVersion=1" "$(echo "$r" | jqget "['data']['currentVersion']")" "1"

inst=$($PG -t -c "SELECT count(*) FROM semantic_relation_network.relation_instance WHERE fqn='$DRAFT_FQN';")
draft=$($PG -t -c "SELECT count(*) FROM semantic_relation_network.relation_instance_draft WHERE fqn='$DRAFT_FQN';")
ver=$($PG -t -c "SELECT count(*) FROM semantic_relation_network.relation_version WHERE fqn='$DRAFT_FQN';")
idx=$($PG -t -c "SELECT count(*) FROM semantic_relation_network.entity_relation_index WHERE relation_fqn='$DRAFT_FQN';")
check "主表有记录(=1)" "$(echo "$inst" | tr -d ' ')" "1"
check "草稿表已删除(=0)" "$(echo "$draft" | tr -d ' ')" "0"
check "历史表 v1(=1)" "$(echo "$ver" | tr -d ' ')" "1"
check "索引表 2 条(OUT+IN)" "$(echo "$idx" | tr -d ' ')" "2"

# 出边/入边验证
r=$(api GET "/api/v1/graph/relations/outbound?entityFqn=Order_100")
check "Order_100 出边包含" "$(echo "$r" | jqget "['data'][0]['fqn']")" "$DRAFT_FQN"
r=$(api GET "/api/v1/graph/relations/inbound?entityFqn=OrderItem_101")
check "OrderItem_101 入边包含" "$(echo "$r" | jqget "['data'][0]['fqn']")" "$DRAFT_FQN"

# from-active（现在有生效版本）
r=$(api POST "/api/v1/graph/drafts/from-active?fqn=$(urlenc "$DRAFT_FQN")")
check "from-active 创建草稿" "$(echo "$r" | jqget "['code']")" "200"
check "from-active baseVersion=1" "$(echo "$r" | jqget "['data']['baseVersion']")" "1"
r=$(api DELETE "/api/v1/graph/drafts/$(urlenc "$DRAFT_FQN")")
check "清理 from-active 草稿" "$(echo "$r" | jqget "['code']")" "200"

echo ""
echo "==================================================================="
echo " 5. 场景 3：多维查询（精准/出边过滤/多维过滤/空结果）"
echo "==================================================================="
r=$(api GET "/api/v1/graph/relations/$(urlenc "$DRAFT_FQN")")
check "FQN 精准查询" "$(echo "$r" | jqget "['code']")" "200"
check "精准查询 currentVersion" "$(echo "$r" | jqget "['data']['currentVersion']")" "1"
r=$(api GET "/api/v1/graph/relations/outbound?entityFqn=Order_100&relationType=COMPOSITION")
check "出边按类型过滤" "$(echo "$r" | jqget "['code']")" "200"
r=$(api POST /api/v1/graph/relations/filter '{
  "relationTypes":["COMPOSITION"],"nameKeyword":"包含","pageRequest":{"page":1,"size":20}}')
check "多维过滤(nameKeyword)" "$(echo "$r" | jqget "['code']")" "200"
check "多维过滤 total>=1" "$(echo "$r" | jqget "['data']['total']")" "1"
r=$(api POST /api/v1/graph/relations/filter '{
  "relationTypes":["NONEXISTENT"],"pageRequest":{"page":1,"size":20}}')
check "空结果不报错" "$(echo "$r" | jqget "['code']")" "200"
check "空结果 total=0" "$(echo "$r" | jqget "['data']['total']")" "0"

echo ""
echo "==================================================================="
echo " 6. 场景 4+5：多关系建网、下线依赖校验、多版本追溯"
echo "==================================================================="
# 6.1 建多条不同关联类型的关系（组成 + 关联引用 + 映射对应 + 流程时序 + 依赖影响）
mk_graph() { # mk_graph <source> <relTypeFqn> <target> <name> <content-json>
  local code fqn
  fqn="$1#$2#$3"
  code=$(api POST /api/v1/graph/drafts "{\"sourceEntityFqn\":\"$1\",\"relationTypeFqn\":\"$2\",\"targetEntityFqn\":\"$3\",\"name\":\"$4\",\"description\":\"complex\",\"content\":$5}" | jqget "['code']")
  api POST /api/v1/graph/relations/activate "{\"fqn\":\"$fqn\"}" >/dev/null
  echo "$code"
}
R_COMP="Order_100#erp:1.0.0.COMPOSITION#OrderItem_101"    # 已建
R_REF="Customer_001#erp:1.0.0.ASSOCIATION_REFERENCE#Order_100"
R_MAP="OrderItem_101#erp:1.0.0.MAPPING_CORRESPONDENCE#Warehouse_301"
R_SEQ="Order_100#erp:1.0.0.PROCESS_SEQUENCE#Order_200"
R_DEP="Order_100#erp:1.0.0.DEPENDENCY_INFLUENCE#Supplier_001"

c=$(mk_graph "Customer_001" "erp:1.0.0.ASSOCIATION_REFERENCE" "Order_100" "客户1下单100" '{"source":"direct"}')
check "建 关联引用关系" "$c" "200"
c=$(mk_graph "OrderItem_101" "erp:1.0.0.MAPPING_CORRESPONDENCE" "Warehouse_301" "条目101入仓301" '{"bin":"A-03"}')
check "建 映射对应关系" "$c" "200"
c=$(mk_graph "Order_100" "erp:1.0.0.PROCESS_SEQUENCE" "Order_200" "100后处理200" '{"step":1}')
check "建 流程时序关系" "$c" "200"
c=$(mk_graph "Order_100" "erp:1.0.0.DEPENDENCY_INFLUENCE" "Supplier_001" "100依赖供应商1" '{"level":"HIGH"}')
check "建 依赖影响关系" "$c" "200"

# 6.2 依赖阻塞：DEPENDENCY_INFLUENCE 关系依赖 R_COMP -> 下线 R_COMP 应被 32008 阻塞
# 注: 当前实现的 DependencyCheckService 以「目标实体FQN == 待下线关系FQN 且类型为 DEPENDENCY_INFLUENCE」判定强依赖。
#     由于图关系端点只允许指向生效元数据实体，该阻塞路径需通过 DB 种子模拟（见下方 check-deprecation）。
echo "  [注] 依赖阻塞种子：插入一条 DEPENDENCY_INFLUENCE 关系，其 target_entity_fqn 指向 R_COMP"
$PG -c "INSERT INTO semantic_relation_network.relation_instance(fqn,name,description,source_entity_fqn,target_entity_fqn,relation_type,relation_schema_fqn,content,current_version,created_time,updated_time)
        VALUES ('Seeder#erp:1.0.0.DEPENDENCY_INFLUENCE#$R_COMP','依赖种子','seed','Order_100','$R_COMP','DEPENDENCY_INFLUENCE','erp:1.0.0.DEPENDENCY_INFLUENCE','{}',1,now(),now()) ON CONFLICT DO NOTHING;" >/dev/null
r=$(api POST /api/v1/graph/relations/check-deprecation "{\"fqn\":\"$R_COMP\"}")
check "check-deprecation canDeprecate=false" "$(echo "$r" | jqget "['data']['canDeprecate']")" "False"
check_contains "blockingRelations 含 R_COMP" "$(echo "$r" | jqget "['data']['blockingRelations']")" "$R_COMP"
r=$(api POST /api/v1/graph/relations/deprecate "{\"fqn\":\"$R_COMP\"}")
check "下线被依赖阻塞(32008)" "$(echo "$r" | jqget "['code']")" "32008"

# 清理种子，恢复可下线
$PG -c "DELETE FROM semantic_relation_network.relation_instance WHERE fqn LIKE 'Seeder#%';" >/dev/null
r=$(api POST /api/v1/graph/relations/check-deprecation "{\"fqn\":\"$R_COMP\"}")
check "清理后 canDeprecate=true" "$(echo "$r" | jqget "['data']['canDeprecate']")" "True"

# 6.3 下线 R_COMP + 查询不存在 + 历史保留
r=$(api POST /api/v1/graph/relations/deprecate "{\"fqn\":\"$R_COMP\"}")
check "执行下线" "$(echo "$r" | jqget "['code']")" "200"
r=$(api GET "/api/v1/graph/relations/$(urlenc "$R_COMP")")
check "下线后查询=32002" "$(echo "$r" | jqget "['code']")" "32002"
inst=$($PG -t -c "SELECT count(*) FROM semantic_relation_network.relation_instance WHERE fqn='$R_COMP';")
ver=$($PG -t -c "SELECT count(*) FROM semantic_relation_network.relation_version WHERE fqn='$R_COMP';")
idx=$($PG -t -c "SELECT count(*) FROM semantic_relation_network.entity_relation_index WHERE relation_fqn='$R_COMP';")
check "下线后主表=0" "$(echo "$inst" | tr -d ' ')" "0"
check "下线后历史保留>=1" "$(echo "$ver" | tr -d ' ')" "1"
check "下线后索引=0" "$(echo "$idx" | tr -d ' ')" "0"

# 6.4 重新生效（reactivate，应恢复 v1）+ 基于历史多次修改产生多版本
r=$(api POST /api/v1/graph/relations/reactivate "{\"fqn\":\"$R_COMP\"}")
check "reactivate 恢复" "$(echo "$r" | jqget "['code']")" "200"
check "reactivate currentVersion=1" "$(echo "$r" | jqget "['data']['currentVersion']")" "1"
r=$(api POST /api/v1/graph/relations/reactivate "{\"fqn\":\"$R_COMP\"}")
check "已生效再 reactivate 拒绝(32015)" "$(echo "$r" | jqget "['code']")" "32015"

# v2: from-active -> 改内容 -> activate
api POST "/api/v1/graph/drafts/from-active?fqn=$(urlenc "$R_COMP")" >/dev/null
api PUT "/api/v1/graph/drafts/$(urlenc "$R_COMP")/content" '{"content":{"quantity":5,"unitPrice":399.0}}' >/dev/null
r=$(api POST /api/v1/graph/relations/activate "{\"fqn\":\"$R_COMP\"}")
check "激活 v2" "$(echo "$r" | jqget "['data']['currentVersion']")" "2"
# v3
api POST "/api/v1/graph/drafts/from-active?fqn=$(urlenc "$R_COMP")" >/dev/null
api PUT "/api/v1/graph/drafts/$(urlenc "$R_COMP")/content" '{"content":{"quantity":9,"unitPrice":499.0,"note":"final"}}' >/dev/null
r=$(api POST /api/v1/graph/relations/activate "{\"fqn\":\"$R_COMP\"}")
check "激活 v3" "$(echo "$r" | jqget "['data']['currentVersion']")" "3"

# 版本列表（倒序 v3,v2,v1）+ diff v1 vs v3
r=$(api GET "/api/v1/graph/versions/$(urlenc "$R_COMP")")
check "版本列表首条=v3" "$(echo "$r" | jqget "['data'][0]['version']")" "3"
check "版本列表次条=v2" "$(echo "$r" | jqget "['data'][1]['version']")" "2"
check "版本列表尾条=v1" "$(echo "$r" | jqget "['data'][2]['version']")" "1"
r=$(api POST /api/v1/graph/versions/diff "{\"fqn\":\"$R_COMP\",\"versionA\":1,\"versionB\":3}")
check "diff 返回" "$(echo "$r" | jqget "['code']")" "200"
check_contains "diff 识别新增 note" "$(echo "$r" | jqget "['data']['addedFields']")" "note"
check_contains "diff 识别修改 unitPrice" "$(echo "$r" | jqget "['data']['modifiedFields']")" "unitPrice"

echo ""
echo "==================================================================="
echo " 7. 场景 7：批量导入导出（JSON/SKIP + 前缀导出）"
echo "==================================================================="
# 导入一条新关系 Order_200 -> OrderItem_102
r=$(api POST /api/v1/graph/import '{
  "content":"[{\"sourceEntityFqn\":\"Order_200\",\"relationTypeFqn\":\"erp:1.0.0.COMPOSITION\",\"targetEntityFqn\":\"OrderItem_102\",\"name\":\"200包含102\",\"content\":{\"qty\":7}}]",
  "format":"JSON","strategy":"SKIP"}')
check "导入 1 条" "$(echo "$r" | jqget "['code']")" "200"
check "导入 success=1" "$(echo "$r" | jqget "['data']['successCount']")" "1"
check_contains "导入 FQN 正确" "$(echo "$r" | jqget "['data']['items']")" "Order_200#erp:1.0.0.COMPOSITION#OrderItem_102"
# 导入数据仅进草稿表，对外不可见
R_IMP="Order_200#erp:1.0.0.COMPOSITION#OrderItem_102"
r=$(api GET "/api/v1/graph/relations/$(urlenc "$R_IMP")")
check "导入草稿对外不可见(32002)" "$(echo "$r" | jqget "['code']")" "32002"
# 再次导入相同 FQN -> SKIP
r=$(api POST /api/v1/graph/import '{
  "content":"[{\"sourceEntityFqn\":\"Order_200\",\"relationTypeFqn\":\"erp:1.0.0.COMPOSITION\",\"targetEntityFqn\":\"OrderItem_102\",\"name\":\"dup\",\"content\":{}}]",
  "format":"JSON","strategy":"SKIP"}')
check "重复导入 SKIP=1" "$(echo "$r" | jqget "['data']['skipCount']")" "1"
# 混合导入：1 合法 + 1 非法（目标实体不存在）-> 合法成功、非法进失败清单且事务不回滚
r=$(api POST /api/v1/graph/import '{
  "content":"[{\"sourceEntityFqn\":\"Order_100\",\"relationTypeFqn\":\"erp:1.0.0.COMPOSITION\",\"targetEntityFqn\":\"Ghost_999\",\"name\":\"无效\",\"content\":{}},{\"sourceEntityFqn\":\"Order_200\",\"relationTypeFqn\":\"erp:1.0.0.COMPOSITION\",\"targetEntityFqn\":\"OrderItem_101\",\"name\":\"200包含101\",\"content\":{\"qty\":2}}]",
  "format":"JSON","strategy":"SKIP"}')
check "混合导入 code=200" "$(echo "$r" | jqget "['code']")" "200"
check "混合导入 success=1" "$(echo "$r" | jqget "['data']['successCount']")" "1"
check "混合导入 fail=1" "$(echo "$r" | jqget "['data']['failureCount']")" "1"
check_contains "失败原因含端点实体无效" "$(echo "$r" | jqget "['data']['items']")" "端点实体无效"
# 导出：前缀 Order_100#（生效关系：R_COMP/R_SEQ/R_DEP 共 3 条）
r=$(api POST /api/v1/graph/export '{"fqnPrefixes":["Order_100#"],"format":"YAML"}')
check "导出 code=200" "$(echo "$r" | jqget "['code']")" "200"
check "导出 total=3" "$(echo "$r" | jqget "['data']['totalCount']")" "3"
check_contains "导出内容含 R_COMP" "$(echo "$r" | jqget "['data']['content']")" "$R_COMP"

echo ""
echo "==================================================================="
echo " 8. 场景 8：拓扑完整性校验 + 关系计数"
echo "==================================================================="
r=$(api POST /api/v1/graph/topology/validate '{"fqnPrefix":"Order_100#","relationType":"COMPOSITION"}')
check "拓扑校验" "$(echo "$r" | jqget "['code']")" "200"
check "totalChecked=3(源出边生效关系数)" "$(echo "$r" | jqget "['data']['totalChecked']")" "3"
check "issuesFound=0" "$(echo "$r" | jqget "['data']['issuesFound']")" "0"
r=$(api GET "/api/v1/graph/topology/relation-count?entityFqn=Order_100")
check "关系计数" "$(echo "$r" | jqget "['code']")" "200"

echo ""
echo "==================================================================="
echo " 9. 场景 9：边界与异常"
echo "==================================================================="
# 端点未生效
r=$(api POST /api/v1/graph/drafts '{"sourceEntityFqn":"Ghost_Entity","relationTypeFqn":"erp:1.0.0.COMPOSITION","targetEntityFqn":"OrderItem_101","name":"x","description":"x","content":{}}')
check "端点未生效(32007)" "$(echo "$r" | jqget "['code']")" "32007"
# Schema 未发布
r=$(api POST /api/v1/graph/drafts '{"sourceEntityFqn":"Order_100","relationTypeFqn":"erp:1.0.0.NOT_EXIST","targetEntityFqn":"OrderItem_101","name":"x","description":"x","content":{}}')
check "Schema 不存在(32010)" "$(echo "$r" | jqget "['code']")" "32010"
# 基数约束（COMPOSITION 源端=1，Order_100 已有 1 条 COMPOSITION -> 第二条应拦截）
r=$(api POST /api/v1/graph/drafts '{"sourceEntityFqn":"Order_100","relationTypeFqn":"erp:1.0.0.COMPOSITION","targetEntityFqn":"OrderItem_102","name":"第二组成","description":"x","content":{"q":1}}')
check "创建第二条 COMPOSITION 草稿" "$(echo "$r" | jqget "['code']")" "200"
r=$(api POST /api/v1/graph/relations/activate '{"fqn":"Order_100#erp:1.0.0.COMPOSITION#OrderItem_102"}')
check "基数超限拦截(32006)" "$(echo "$r" | jqget "['code']")" "32006"
# 分页越界
r=$(api POST /api/v1/graph/relations/filter '{"relationTypes":["COMPOSITION"],"pageRequest":{"page":9999,"size":20}}')
check "分页越界返回空" "$(echo "$r" | jqget "['data']['content']")" "[]"
# SQL 注入关键字
r=$(api POST /api/v1/graph/relations/filter '{"nameKeyword":"%'"'"' OR 1=1 --","pageRequest":{"page":1,"size":20}}')
check "SQL 注入免疫" "$(echo "$r" | jqget "['code']")" "200"

echo ""
echo "==================================================================="
echo " 汇总：PASS=$PASS FAIL=$FAIL"
echo "==================================================================="
if [ "$FAIL" -gt 0 ]; then
  echo "失败用例:"
  for f in "${FAILED_CASES[@]}"; do echo "  - $f"; done
  exit 1
fi
echo "全部通过。"
