#!/usr/bin/env bash
# =============================================================================
# MetaForge compute-engine BC 复杂数据端到端测试脚本
#
# 设计依据: specs/001-semantic-query-reasoning/quickstart.md
#   覆盖全部 20 个验证场景（FR-001~FR-024），并针对当前实现的实际 REST 端点编写。
#
# 与 quickstart 文档的差异（实现 vs 文档）:
#   * 所有响应恒为 HTTP 200，业务错误码在 body.code 字段
#   * 图遍历 direction 取值: FORWARD/BACKWARD/BIDIRECTIONAL（文档示例的 BOTH 不存在）
#   * 7 维过滤 filterCriteria 中 sourceFqns/targetFqns/entityTypes 等 FQN 维度
#     使用 [{value, matchMode}] 列表形态（非文档示例的 {"values": [...]}）
#   * metadata_entity 实体 FQN 含版本段（order:1.0.0.pkg_order.X）无法经 metadata API 创建
#     （校验拒绝，错误码 31001），故实体经 SQL 直插 metadata_entity 表
#   * 关系经 graph BC 标准流程创建: POST /drafts -> POST /relations/activate
#   * 传导规则（transitivity-rules）在 application-metaforge-compute-engine.yml 配置:
#     COMPOSITION(depth5)/DEPENDENCY_INFLUENCE(depth2)/PROCESS_SEQUENCE(depth5) 可传递，
#     ASSOCIATION_REFERENCE(depth1) 不可传递
#
# 数据复杂度:
#   * 元数据: 9 个生效实体（3 个 Order + 6 个 Item，跨 2 类 EntitySchema，
#     含 status/price/quantity 多属性，覆盖 active/inactive/cancelled 三态）
#   * 图: 13 条关系，覆盖全部 4 种关联类型（组成/关联引用/依赖影响/流程时序），
#     含 PROCESS_SEQUENCE 循环（A→B→C→A，验证循环去重）、深层链、跨类型桥接
#
# 前置: 应用已在 8080 运行（metamodel/metadata/graph/compute-engine BC），PostgreSQL 在 5432。
#       metamodel 需已包含 order bundle v1.0.0（Order/Item schema 及 4 种 RelationSchema 已发布）
# 用法:
#   ./compute-test.sh          全量运行（自动清理并重建 order 测试数据）
#   MF_KEEP_DATA=1 ./compute-test.sh  不清理旧数据，直接运行（需数据已就绪）
# =============================================================================
set -u

MF_BASE_URL="${MF_BASE_URL:-http://localhost:8080}"
PG="psql -h localhost -U metaforge -d metaforge"
export PGPASSWORD="${PGPASSWORD:-metaforge}"

# 数据 FQN 常量（与 quickstart 场景保持一致）
ROOT="order:1.0.0.pkg_order"
SCHEMA_ORDER="order:1.0.0.pkg_order.Order"
SCHEMA_ITEM="order:1.0.0.pkg_order.Item"
R_COMP="order:1.0.0.COMPOSITION"
R_ASSOC="order:1.0.0.ASSOCIATION_REFERENCE"
R_DEP="order:1.0.0.DEPENDENCY_INFLUENCE"
R_PROC="order:1.0.0.PROCESS_SEQUENCE"

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

# 元模型前置：order bundle 已发布 + 4 种 RelationSchema 存在
r=$(api GET "/api/v1/metamodel/relation-schemas/$R_COMP")
check "RelationSchema COMPOSITION 存在" "$(echo "$r" | jqget "['code']")" "200"
r=$(api GET "/api/v1/metamodel/entity-schemas/$SCHEMA_ORDER")
check "EntitySchema Order 存在" "$(echo "$r" | jqget "['code']")" "200"

echo ""
echo "==================================================================="
echo " 1. 数据准备：清理 + 重建复杂测试数据"
echo "==================================================================="
if [ "${MF_KEEP_DATA:-}" != "1" ]; then
  echo "  [清理] 删除 order 测试数据（图/元数据两域）..."
  $PG -c "DELETE FROM semantic_relation_network.entity_relation_index WHERE relation_fqn LIKE 'order:%';" >/dev/null 2>&1
  $PG -c "DELETE FROM semantic_relation_network.relation_version WHERE fqn LIKE 'order:%';" >/dev/null 2>&1
  $PG -c "DELETE FROM semantic_relation_network.relation_instance WHERE fqn LIKE 'order:%';" >/dev/null 2>&1
  $PG -c "DELETE FROM semantic_relation_network.relation_instance_draft WHERE fqn LIKE 'order:%';" >/dev/null 2>&1
  $PG -c "DELETE FROM metadata_management.metadata_entity WHERE fqn LIKE 'order:%';" >/dev/null 2>&1
  $PG -c "DELETE FROM metadata_management.entity_version WHERE fqn LIKE 'order:%';" >/dev/null 2>&1
  $PG -c "DELETE FROM metadata_management.metadata_entity_draft WHERE fqn LIKE 'order:%';" >/dev/null 2>&1
fi

echo "  [种子] 9 个元数据实体（SQL 直插 metadata_entity）..."
$PG -c "INSERT INTO metadata_management.metadata_entity(fqn,name,description,entity_schema_fqn,content,current_version,created_by,updated_by) VALUES
('$ROOT.Order_001','订单1','主订单','$SCHEMA_ORDER','{\"status\":\"active\",\"price\":1999.5}',1,'system','system'),
('$ROOT.Order_002','订单2','履约中','$SCHEMA_ORDER','{\"status\":\"shipped\",\"price\":899.0}',1,'system','system'),
('$ROOT.Order_003','订单3','促销订单','$SCHEMA_ORDER','{\"status\":\"active\",\"price\":2400.0}',1,'system','system'),
('$ROOT.Item_003','商品3','电子品类','$SCHEMA_ITEM','{\"status\":\"active\",\"price\":150.0,\"quantity\":3}',1,'system','system'),
('$ROOT.Item_004','商品4','图书品类','$SCHEMA_ITEM','{\"status\":\"active\",\"price\":80.0,\"quantity\":5}',1,'system','system'),
('$ROOT.Item_005','商品5','服装品类','$SCHEMA_ITEM','{\"status\":\"cancelled\",\"price\":50.0,\"quantity\":2}',1,'system','system'),
('$ROOT.Item_006','商品6','家居品类','$SCHEMA_ITEM','{\"status\":\"active\",\"price\":320.0,\"quantity\":1}',1,'system','system'),
('$ROOT.Item_007','商品7','美妆品类','$SCHEMA_ITEM','{\"status\":\"active\",\"price\":210.0,\"quantity\":4}',1,'system','system'),
('$ROOT.Item_008','商品8','食品品类','$SCHEMA_ITEM','{\"status\":\"inactive\",\"price\":30.0,\"quantity\":9}',1,'system','system')
ON CONFLICT (fqn) DO UPDATE SET content=EXCLUDED.content;" >/dev/null 2>&1
cnt=$($PG -t -c "SELECT count(*) FROM metadata_management.metadata_entity WHERE fqn LIKE 'order:%';" | tr -d ' ')
check "实体种子数量(=9)" "$cnt" "9"

echo "  [种子] 13 条关系（graph 标准流程: 草稿 -> 激活）..."
mk_graph() { # mk_graph <source> <relTypeFqn> <target> <content-json>
  local code fqn
  fqn="$1#$2#$3"
  code=$(api POST /api/v1/graph/drafts "{\"sourceEntityFqn\":\"$1\",\"relationTypeFqn\":\"$2\",\"targetEntityFqn\":\"$3\",\"name\":\"$4\",\"description\":\"complex\",\"content\":$5}" | jqget "['code']")
  if [ "$code" = "200" ]; then
    api POST /api/v1/graph/relations/activate "{\"fqn\":\"$fqn\"}" >/dev/null
  fi
  echo "$code"
}
r=$(mk_graph "$ROOT.Order_001" "$R_COMP"  "$ROOT.Item_003" "订单1包含商品3" '{"quantity":3}')
check "COMPOSITION Order_001->Item_003" "$r" "200"
r=$(mk_graph "$ROOT.Order_002" "$R_COMP"  "$ROOT.Item_004" "订单2包含商品4" '{"quantity":5}')
check "COMPOSITION Order_002->Item_004" "$r" "200"
r=$(mk_graph "$ROOT.Order_003" "$R_COMP"  "$ROOT.Item_005" "订单3包含商品5" '{"quantity":2}')
check "COMPOSITION Order_003->Item_005" "$r" "200"
r=$(mk_graph "$ROOT.Item_003" "$R_ASSOC" "$ROOT.Order_002" "商品3关联订单2" '{"source":"direct"}')
check "ASSOCIATION_REFERENCE Item_003->Order_002" "$r" "200"
r=$(mk_graph "$ROOT.Item_004" "$R_ASSOC" "$ROOT.Order_003" "商品4关联订单3" '{"source":"direct"}')
check "ASSOCIATION_REFERENCE Item_004->Order_003" "$r" "200"
r=$(mk_graph "$ROOT.Item_005" "$R_ASSOC" "$ROOT.Order_001" "商品5关联订单1" '{"source":"direct"}')
check "ASSOCIATION_REFERENCE Item_005->Order_001" "$r" "200"
r=$(mk_graph "$ROOT.Order_001" "$R_DEP"   "$ROOT.Item_005" "订单1依赖商品5" '{"level":"HIGH"}')
check "DEPENDENCY_INFLUENCE Order_001->Item_005" "$r" "200"
r=$(mk_graph "$ROOT.Order_002" "$R_DEP"   "$ROOT.Item_006" "订单2依赖商品6" '{"level":"MEDIUM"}')
check "DEPENDENCY_INFLUENCE Order_002->Item_006" "$r" "200"
r=$(mk_graph "$ROOT.Item_007" "$R_DEP"   "$ROOT.Order_001" "商品7依赖订单1" '{"level":"LOW"}')
check "DEPENDENCY_INFLUENCE Item_007->Order_001" "$r" "200"
r=$(mk_graph "$ROOT.Order_001" "$R_PROC"  "$ROOT.Order_002" "订单1后置订单2" '{"step":1}')
check "PROCESS_SEQUENCE Order_001->Order_002" "$r" "200"
r=$(mk_graph "$ROOT.Order_002" "$R_PROC"  "$ROOT.Order_003" "订单2后置订单3" '{"step":2}')
check "PROCESS_SEQUENCE Order_002->Order_003" "$r" "200"
r=$(mk_graph "$ROOT.Order_003" "$R_PROC"  "$ROOT.Order_001" "订单3后置订单1(循环)" '{"step":3}')
check "PROCESS_SEQUENCE Order_003->Order_001(循环)" "$r" "200"
r=$(mk_graph "$ROOT.Order_001" "$R_PROC"  "$ROOT.Order_003" "订单1后置订单3(直达)" '{"step":4}')
check "PROCESS_SEQUENCE Order_001->Order_003(直达)" "$r" "200"

inst=$($PG -t -c "SELECT count(*) FROM semantic_relation_network.relation_instance WHERE fqn LIKE 'order:%';" | tr -d ' ')
idx=$($PG -t -c "SELECT count(*) FROM semantic_relation_network.entity_relation_index WHERE relation_fqn LIKE 'order:%';" | tr -d ' ')
check "生效关系数(=13)" "$inst" "13"
check "索引表条目(=26)" "$idx" "26"

echo ""
echo "==================================================================="
echo " 2. 场景 1：多度邻接查询（FR-001）"
echo "==================================================================="
r=$(api POST /api/v1/compute-engine/adjacency '{
  "sourceFqn":"order:1.0.0.pkg_order.Order_001","direction":"FORWARD","maxDepth":3,
  "relationTypes":null,"filterCriteria":null}')
check "邻接 code=200" "$(echo "$r" | jqget "['code']")" "200"
check "truncated=false" "$(echo "$r" | jqget "['data']['truncated']")" "False"
ents=$(echo "$r" | jqget "['data']['entities']")
check_contains "起点含 Order_001" "$ents" "order:1.0.0.pkg_order.Order_001"
check_contains "邻居含 Item_003" "$ents" "order:1.0.0.pkg_order.Item_003"
check_contains "邻居含 Order_002" "$ents" "order:1.0.0.pkg_order.Order_002"
uniq=$(echo "$ents" | python3 -c "import json,sys; d=json.load(sys.stdin); print(len({e['fqn'] for e in d}))")
total=$(echo "$ents" | python3 -c "import json,sys; d=json.load(sys.stdin); print(len(d))")
check "实体去重(无重复)" "$uniq" "$total"

echo ""
echo "==================================================================="
echo " 3. 场景 2：组合层级树上溯父链（FR-002）"
echo "==================================================================="
r=$(api POST /api/v1/compute-engine/composition-tree '{
  "rootFqn":"order:1.0.0.pkg_order.Item_003","direction":"BACKWARD","maxDepth":10,
  "filterCriteria":null}')
check "组合树 code=200" "$(echo "$r" | jqget "['code']")" "200"
ents=$(echo "$r" | jqget "['data']['entities']")
check_contains "含起点 Item_003" "$ents" "order:1.0.0.pkg_order.Item_003"
check_contains "含父级 Order_001" "$ents" "order:1.0.0.pkg_order.Order_001"

echo ""
echo "==================================================================="
echo " 4. 场景 3：图模式匹配通配符（FR-004）"
echo "==================================================================="
r=$(api POST /api/v1/compute-engine/pattern-match '{
  "pattern":"* -[?]-> * -[?]-> *","maxResults":100}')
check "模式匹配 code=200" "$(echo "$r" | jqget "['code']")" "200"
nents=$(echo "$r" | python3 -c "import json,sys; d=json.load(sys.stdin); print(len(d['data']['entities']))")
nrels=$(echo "$r" | python3 -c "import json,sys; d=json.load(sys.stdin); print(len(d['data']['relations']))")
check_ge "匹配实体数>=4" "$nents" "4"
check_ge "匹配关系数>=3" "$nrels" "3"

echo ""
echo "==================================================================="
echo " 5. 场景 4：7 维过滤交集（FR-015）"
echo "==================================================================="
r=$(api POST /api/v1/compute-engine/adjacency '{
  "sourceFqn":"order:1.0.0.pkg_order.Order_001","direction":"BIDIRECTIONAL","maxDepth":5,
  "relationTypes":["COMPOSITION"],
  "filterCriteria":{
    "associationTypes":["COMPOSITION"],
    "sourceFqns":[{"value":"order:1.0.0.","matchMode":"PREFIX"}],
    "targetFqns":null,
    "relationInstanceFqns":null,
    "entityTypes":[{"value":"order:1.0.0.pkg_order.Order","matchMode":"EXACT"},
                   {"value":"order:1.0.0.pkg_order.Item","matchMode":"EXACT"}],
    "relationTypes":null,
    "propertyFilters":null}}')
check "7维过滤 code=200" "$(echo "$r" | jqget "['code']")" "200"
ents=$(echo "$r" | jqget "['data']['entities']")
check_contains "过滤后含 Order_001" "$ents" "order:1.0.0.pkg_order.Order_001"
check_contains "过滤后含 Item_003" "$ents" "order:1.0.0.pkg_order.Item_003"
check_not_contains "排除非 COMPOSITION 邻居 Item_006" "$ents" "order:1.0.0.pkg_order.Item_006"

echo ""
echo "==================================================================="
echo " 6. 场景 5：传递闭包推理（FR-009）"
echo "==================================================================="
r=$(api POST /api/v1/compute-engine/closure '{
  "sourceFqn":"order:1.0.0.pkg_order.Order_001","relationTypes":null,"filterCriteria":null}')
check "闭包 code=200" "$(echo "$r" | jqget "['code']")" "200"
check "totalReachable=8" "$(echo "$r" | jqget "['data']['totalReachable']")" "8"
check_contains "包含 Item_005(DEP)" "$(echo "$r" | jqget "['data']['layers']")" "order:1.0.0.pkg_order.Item_005"

echo ""
echo "==================================================================="
echo " 7. 场景 6：多跳语义推理（FR-010）"
echo "==================================================================="
r=$(api POST /api/v1/compute-engine/multi-hop '{
  "sourceFqn":"order:1.0.0.pkg_order.Order_001",
  "hopSteps":[{"relationType":"COMPOSITION","direction":"FORWARD"},
              {"relationType":"ASSOCIATION_REFERENCE","direction":"FORWARD"}],
  "filterCriteria":null}')
check "多跳 code=200" "$(echo "$r" | jqget "['code']")" "200"
check "totalPaths>=1" "$(echo "$r" | jqget "['data']['totalPaths']" | awk '{print ($1>=1?"OK":"FAIL")}')" "OK"
check_contains "路径含 Order_002" "$(echo "$r" | jqget "['data']['paths']")" "order:1.0.0.pkg_order.Order_002"

echo ""
echo "==================================================================="
echo " 8. 场景 7：结果截断标记（FR-023）"
echo "==================================================================="
r=$(api POST /api/v1/compute-engine/adjacency '{
  "sourceFqn":"order:1.0.0.pkg_order.Order_001","direction":"FORWARD","maxDepth":1,
  "relationTypes":null,"filterCriteria":null}')
check "maxDepth=1 truncated=true" "$(echo "$r" | jqget "['data']['truncated']")" "True"
check "truncatedReason=DEPTH_EXCEEDED" "$(echo "$r" | jqget "['data']['truncatedReason']")" "DEPTH_EXCEEDED"

echo ""
echo "==================================================================="
echo " 9. 场景 8：per-type 深度差异化（FR-020）"
echo "==================================================================="
r=$(api POST /api/v1/compute-engine/adjacency '{
  "sourceFqn":"order:1.0.0.pkg_order.Order_001","direction":"BIDIRECTIONAL","maxDepth":5,
  "relationTypes":["COMPOSITION","ASSOCIATION_REFERENCE"],"filterCriteria":null}')
check "per-type code=200" "$(echo "$r" | jqget "['code']")" "200"
ents=$(echo "$r" | jqget "['data']['entities']")
check_contains "COMPOSITION 首度含 Item_003" "$ents" "order:1.0.0.pkg_order.Item_003"
check_contains "ASSOC 首度含 Item_005" "$ents" "order:1.0.0.pkg_order.Item_005"
check_not_contains "ASSOC max-depth=1 截断(不含 Item_004)" "$ents" "order:1.0.0.pkg_order.Item_004"

echo ""
echo "==================================================================="
echo " 10. 场景 9：子图提取查询（FR-003）"
echo "==================================================================="
r=$(api POST /api/v1/compute-engine/subgraph '{
  "centerFqns":["order:1.0.0.pkg_order.Order_001","order:1.0.0.pkg_order.Item_003"],
  "maxDepth":2,"filterCriteria":null}')
check "子图 code=200" "$(echo "$r" | jqget "['code']")" "200"
nents=$(echo "$r" | python3 -c "import json,sys; d=json.load(sys.stdin); print(len(d['data']['entities']))")
uniq=$(echo "$r" | python3 -c "import json,sys; d=json.load(sys.stdin); print(len({e['fqn'] for e in d['data']['entities']}))")
check_ge "子图实体>=3" "$nents" "3"
check "实体去重(无重复)" "$uniq" "$nents"
nadj=$(echo "$r" | python3 -c "import json,sys; d=json.load(sys.stdin); print(len(d['data']['adjacencyMap']))")
check_ge "adjacencyMap 覆盖所有实体" "$nadj" "$uniq"

echo ""
echo "==================================================================="
echo " 11. 场景 10：多条件复合检索（FR-005）"
echo "==================================================================="
r=$(api POST /api/v1/compute-engine/search '{
  "entityTypes":["order:1.0.0.pkg_order.Item"],
  "attributes":[{"field":"status","operator":"EQ","value":"active"},
                {"field":"price","operator":"GT","value":"100"}],
  "relationTypes":null,"page":0,"size":20,"sortField":null,"sortDirection":null}')
check "检索 code=200" "$(echo "$r" | jqget "['code']")" "200"
check "total=3(active且price>100)" "$(echo "$r" | jqget "['data']['total']")" "3"
check_contains "含 Item_003" "$(echo "$r" | jqget "['data']['content']")" "order:1.0.0.pkg_order.Item_003"
check_not_contains "排除 Item_004(price=80)" "$(echo "$r" | jqget "['data']['content']")" "order:1.0.0.pkg_order.Item_004"

echo ""
echo "==================================================================="
echo " 12. 场景 11：批量语义查询（FR-006）"
echo "==================================================================="
r=$(api POST /api/v1/compute-engine/batch '{
  "fqns":["order:1.0.0.pkg_order.Order_001","order:1.0.0.pkg_order.NotExist_999"]}')
check "批量 code=200" "$(echo "$r" | jqget "['code']")" "200"
check_contains "entities 含 Order_001" "$(echo "$r" | jqget "['data']['entities']")" "order:1.0.0.pkg_order.Order_001"
check_contains "notFoundFqns 含 NotExist_999" "$(echo "$r" | jqget "['data']['notFoundFqns']")" "order:1.0.0.pkg_order.NotExist_999"

echo ""
echo "==================================================================="
echo " 13. 场景 12：两点间路径查询（FR-008）"
echo "==================================================================="
r=$(api POST /api/v1/compute-engine/paths '{
  "sourceFqn":"order:1.0.0.pkg_order.Order_001","targetFqn":"order:1.0.0.pkg_order.Item_003",
  "direction":"BIDIRECTIONAL","maxDepth":5,"relationTypes":null,"findShortest":false,
  "filterCriteria":null}')
check "路径 code=200" "$(echo "$r" | jqget "['code']")" "200"
check "totalPaths>=1" "$(echo "$r" | jqget "['data']['totalPaths']" | awk '{print ($1>=1?"OK":"FAIL")}')" "OK"
check_contains "含直接边 COMPOSITION" "$(echo "$r" | jqget "['data']['paths']")" "$ROOT.Order_001#$R_COMP#$ROOT.Item_003"
r=$(api POST /api/v1/compute-engine/paths '{
  "sourceFqn":"order:1.0.0.pkg_order.Order_001","targetFqn":"order:1.0.0.pkg_order.Item_003",
  "direction":"BIDIRECTIONAL","maxDepth":5,"relationTypes":null,"findShortest":true,
  "filterCriteria":null}')
check "最短路径 totalPaths=1" "$(echo "$r" | jqget "['data']['totalPaths']")" "1"
check "最短路径步数=1" "$(echo "$r" | python3 -c "import json,sys; d=json.load(sys.stdin); print(len(d['data']['paths'][0]['steps']))")" "1"

echo ""
echo "==================================================================="
echo " 14. 场景 13：路径可达性判定（FR-011）"
echo "==================================================================="
r=$(api POST /api/v1/compute-engine/reachability '{
  "sourceFqn":"order:1.0.0.pkg_order.Order_001","targetFqn":"order:1.0.0.pkg_order.Item_003",
  "relationTypes":null}')
check "可达性 code=200" "$(echo "$r" | jqget "['code']")" "200"
check "totalPaths=1(可达标记)" "$(echo "$r" | jqget "['data']['totalPaths']")" "1"
check_contains "含 reachable 标记" "$(echo "$r" | jqget "['data']['paths']")" "reachable"

echo ""
echo "==================================================================="
echo " 15. 场景 14：正向影响扩散（FR-012）"
echo "==================================================================="
r=$(api POST /api/v1/compute-engine/impact/diffuse '{
  "centerFqn":"order:1.0.0.pkg_order.Order_001","direction":"FORWARD","maxDepth":3,
  "relationTypes":["COMPOSITION","DEPENDENCY_INFLUENCE"]}')
check "扩散 code=200" "$(echo "$r" | jqget "['code']")" "200"
check "totalImpacted=3" "$(echo "$r" | jqget "['data']['totalImpacted']")" "3"
check_contains "含 Item_005(DEP)" "$(echo "$r" | jqget "['data']['layerStats']")" "order:1.0.0.pkg_order.Item_005"

echo ""
echo "==================================================================="
echo " 16. 场景 15：反向依赖溯源（FR-013）"
echo "==================================================================="
r=$(api POST /api/v1/compute-engine/impact/trace '{
  "centerFqn":"order:1.0.0.pkg_order.Item_003","direction":"BACKWARD","maxDepth":3,
  "relationTypes":["COMPOSITION"]}')
check "溯源 code=200" "$(echo "$r" | jqget "['code']")" "200"
check "totalImpacted=2" "$(echo "$r" | jqget "['data']['totalImpacted']")" "2"
check_contains "父链含 Order_001" "$(echo "$r" | jqget "['data']['entities']")" "order:1.0.0.pkg_order.Order_001"

echo ""
echo "==================================================================="
echo " 17. 场景 16：影响路径详情 POST+GET 双形态（FR-014）"
echo "==================================================================="
r=$(api POST /api/v1/compute-engine/impact/paths '{
  "sourceFqn":"order:1.0.0.pkg_order.Order_001","targetFqn":"order:1.0.0.pkg_order.Item_003",
  "relationTypes":["COMPOSITION"]}')
check "POST 影响路径 code=200" "$(echo "$r" | jqget "['code']")" "200"
check_contains "POST relations 非空" "$(echo "$r" | jqget "['data']['relations']")" "COMPOSITION"
r=$(api GET "/api/v1/compute-engine/impact/paths?sourceFqn=$ROOT.Order_001&targetFqn=$ROOT.Item_003&relationTypes=COMPOSITION&maxDepth=5")
check "GET 影响路径 code=200" "$(echo "$r" | jqget "['code']")" "200"
check_contains "GET relations 非空" "$(echo "$r" | jqget "['data']['relations']")" "COMPOSITION"

echo ""
echo "==================================================================="
echo " 18. 场景 17：循环引用检测与去重（FR-022）"
echo "==================================================================="
r=$(api POST /api/v1/compute-engine/closure '{
  "sourceFqn":"order:1.0.0.pkg_order.Order_002","relationTypes":null,"filterCriteria":null}')
check "循环闭包 code=200(无无限递归)" "$(echo "$r" | jqget "['code']")" "200"
check "totalReachable=8(循环去重)" "$(echo "$r" | jqget "['data']['totalReachable']")" "8"
uniq=$(echo "$r" | python3 -c "import json,sys; d=json.load(sys.stdin); flat=[e['fqn'] for layer in d['data']['layers'].values() for e in layer]; print(len({x for x in flat}))")
total=$(echo "$r" | python3 -c "import json,sys; d=json.load(sys.stdin); flat=[e['fqn'] for layer in d['data']['layers'].values() for e in layer]; print(len(flat))")
check "每个实体仅出现一次(最短深度)" "$uniq" "$total"

echo ""
echo "==================================================================="
echo " 19. 场景 18：批量查询超限错误（33009）"
echo "==================================================================="
batch_body=$(python3 -c "import json; print(json.dumps({'fqns':['fqn:1.0.0.x'+str(i) for i in range(201)]}))")
r=$(api POST /api/v1/compute-engine/batch "$batch_body")
check "批量 201 个返回 33009" "$(echo "$r" | jqget "['code']")" "33009"

echo ""
echo "==================================================================="
echo " 20. 场景 19：实体不存在错误（33001）"
echo "==================================================================="
r=$(api POST /api/v1/compute-engine/adjacency '{
  "sourceFqn":"order:1.0.0.pkg_order.NotExist_999","direction":"FORWARD","maxDepth":3,
  "relationTypes":null,"filterCriteria":null}')
check "不存在实体返回 33001" "$(echo "$r" | jqget "['code']")" "33001"

echo ""
echo "==================================================================="
echo " 21. 场景 20：图模式语法/长度错误（33005/33008）"
echo "==================================================================="
r=$(api POST /api/v1/compute-engine/pattern-match '{
  "pattern":"* -[?]-> * -[?]-> * -[?]-> * -[?]-> * -[?]-> *","maxResults":100}')
c=$(echo "$r" | jqget "['code']")
if [ "$c" = "33005" ] || [ "$c" = "33008" ]; then PASS=$((PASS+1)); echo "  [PASS] 超长模式返回 33005/33008 (=$c)";
else FAIL=$((FAIL+1)); FAILED_CASES+=("场景20: 期望 33005/33008 实际 $c"); echo "  [FAIL] 场景20: 期望=33005/33008 实际=$c"; fi

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
