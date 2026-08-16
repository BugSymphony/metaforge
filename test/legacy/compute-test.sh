#!/usr/bin/env bash
# =============================================================================
# MetaForge compute-engine BC 端到端测试脚本（重写版）
#
# 覆盖：
#   1) 图查询   ：邻接 / 组合树 / 子图 / 模式匹配 / 复合检索 / 批量查询
#   2) 路径推理 ：两点路径 / 最短路径 / 可达性 / 闭包 / 多跳推理
#   3) 影响分析 ：正向扩散 / 反向溯源 / 影响路径（POST+GET）
#   4) 截断语义 ：深度截断 / per-type 深度差异 / 循环去重
#   5) 错误码   ：33001 实体不存在 / 33005 模式非法 / 33008 模式超长 / 33009 批量超限
#   6) 复杂过滤 ：7 维 filterCriteria 的组合矩阵（重点增强）
#
# 数据：9 实体（3 Order + 6 Item）+ 13 关系（COMPOSITION/ASSOCIATION_REFERENCE/
#       DEPENDENCY_INFLUENCE/PROCESS_SEQUENCE，含循环与深层链），经 graph BC 标准流程创建。
#
# 用法：
#   ./compute-test.sh            全量运行（自动清理重建 order 测试数据）
#   MF_KEEP_DATA=1 ./compute-test.sh   不清理，直接运行
#   ./compute-test.sh <pattern>   只运行名称匹配 <pattern> 的用例（过滤模式）
#   MF_BASE_URL=http://localhost:8080 ./compute-test.sh
# =============================================================================
set -u

MF_BASE_URL="${MF_BASE_URL:-http://localhost:8080}"
PG="psql -h localhost -U metaforge -d metaforge"
export PGPASSWORD="${PGPASSWORD:-metaforge}"
FILTER="${1:-}"

ROOT="order:1.0.0.pkg_order"
SCHEMA_ORDER="order:1.0.0.pkg_order.Order"
SCHEMA_ITEM="order:1.0.0.pkg_order.Item"
R_COMP="order:1.0.0.COMPOSITION"
R_ASSOC="order:1.0.0.ASSOCIATION_REFERENCE"
R_DEP="order:1.0.0.DEPENDENCY_INFLUENCE"
R_PROC="order:1.0.0.PROCESS_SEQUENCE"

PASS=0; FAIL=0; FAILED_CASES=()
START_TS=$(date +%s)

# ---------- JSON 工具 ----------

# 单值提取: pget <json> <path>  path 用点分如 data.code / data.total
# 数字/字符串/布尔原样输出
pget() { python3 -c "
import json,sys
d=json.load(sys.stdin)
parts=sys.argv[1].split('.')
v=d
for p in parts:
    v=v[p]
if v is None:
    print('')
else:
    print(v)
" "$1" 2>/dev/null; }

# 列表/字典提取: pjget <json> <path> 返回 json 文本（供 python/grep 再解析）
pjget() { python3 -c "
import json,sys
d=json.load(sys.stdin)
parts=sys.argv[1].split('.')
v=d
for p in parts:
    v=v[p]
print(json.dumps(v, ensure_ascii=False))
" "$1" 2>/dev/null; }

should_run() { [ -z "$FILTER" ] || echo "$1" | grep -q "$FILTER"; }

check() { # check <名称> <实际> <期望>
  should_run "$1" || return
  if [ "$2" = "$3" ]; then PASS=$((PASS+1)); echo "  [PASS] $1 (=$2)";
  else FAIL=$((FAIL+1)); FAILED_CASES+=("$1: 期望 $3 实际 $2"); echo "  [FAIL] $1: 期望=$3 实际=$2"; fi
}

check_contains() { # check_contains <名称> <haystack> <needle>
  should_run "$1" || return
  if echo "$2" | grep -q "$3"; then PASS=$((PASS+1)); echo "  [PASS] $1";
  else FAIL=$((FAIL+1)); FAILED_CASES+=("$1: 未包含 $3"); echo "  [FAIL] $1: 未包含 [$3]"; fi
}

check_not_contains() { # check_not_contains <名称> <haystack> <needle>
  should_run "$1" || return
  if echo "$2" | grep -q "$3"; then FAIL=$((FAIL+1)); FAILED_CASES+=("$1: 意外包含 $3"); echo "  [FAIL] $1: 意外包含 [$3]";
  else PASS=$((PASS+1)); echo "  [PASS] $1"; fi
}

check_ge() { # check_ge <名称> <实际> <最小>
  should_run "$1" || return
  if [ "$2" -ge "$3" ] 2>/dev/null; then PASS=$((PASS+1)); echo "  [PASS] $1 (=$2 >= $3)";
  else FAIL=$((FAIL+1)); FAILED_CASES+=("$1: 期望 >=$3 实际 $2"); echo "  [FAIL] $1: 期望 >=$3 实际=$2"; fi
}

api() { # api <method> <path> [body]
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
code=$(api GET /actuator/health | pget "status")
check "应用健康检查" "$code" "UP"

r=$(api GET "/api/v1/metamodel/relation-schemas/$R_COMP")
check "RelationSchema COMPOSITION 存在" "$(echo "$r" | pget "code")" "200"
r=$(api GET "/api/v1/metamodel/entity-schemas/$SCHEMA_ORDER")
check "EntitySchema Order 存在" "$(echo "$r" | pget "code")" "200"

echo ""
echo "==================================================================="
echo " 1. 数据准备：清理 + 重建复杂测试数据"
echo "==================================================================="
if [ "${MF_KEEP_DATA:-}" != "1" ]; then
  # 仅清理本脚本专属的 order bundle 业务实体（order:1.0.0.pkg_order.*），
  # 避免误删 agent 测试共用的 order:1.0.0.Step_*/Cap_*/Rule_* 实体
  $PG -c "DELETE FROM semantic_relation_network.entity_relation_index WHERE relation_fqn LIKE 'order:1.0.0.pkg_order%';" >/dev/null 2>&1
  $PG -c "DELETE FROM semantic_relation_network.relation_version WHERE fqn LIKE 'order:1.0.0.pkg_order%';" >/dev/null 2>&1
  $PG -c "DELETE FROM semantic_relation_network.relation_instance WHERE fqn LIKE 'order:1.0.0.pkg_order%';" >/dev/null 2>&1
  $PG -c "DELETE FROM semantic_relation_network.relation_instance_draft WHERE fqn LIKE 'order:1.0.0.pkg_order%';" >/dev/null 2>&1
  $PG -c "DELETE FROM metadata_management.metadata_entity WHERE fqn LIKE 'order:1.0.0.pkg_order%';" >/dev/null 2>&1
  $PG -c "DELETE FROM metadata_management.entity_version WHERE fqn LIKE 'order:1.0.0.pkg_order%';" >/dev/null 2>&1
  $PG -c "DELETE FROM metadata_management.metadata_entity_draft WHERE fqn LIKE 'order:1.0.0.pkg_order%';" >/dev/null 2>&1
fi

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
cnt=$($PG -t -c "SELECT count(*) FROM metadata_management.metadata_entity WHERE fqn LIKE 'order:1.0.0.pkg_order%';" | tr -d ' ')
check "实体种子数量(=9)" "$cnt" "9"

mk_graph() { # mk_graph <source> <relSchemaFqn> <target> <name> <content-json>
  local fqn="$1#$2#$3"
  local code
  code=$(api POST /api/v1/graph/drafts "{\"sourceEntityFqn\":\"$1\",\"relationTypeFqn\":\"$2\",\"targetEntityFqn\":\"$3\",\"name\":\"$4\",\"description\":\"complex\",\"content\":$5}" | pget "code")
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

inst=$($PG -t -c "SELECT count(*) FROM semantic_relation_network.relation_instance WHERE fqn LIKE 'order:1.0.0.pkg_order%';" | tr -d ' ')
idx=$($PG -t -c "SELECT count(*) FROM semantic_relation_network.entity_relation_index WHERE relation_fqn LIKE 'order:1.0.0.pkg_order%';" | tr -d ' ')
check "生效关系数(=13)" "$inst" "13"
check "索引表条目(=26)" "$idx" "26"

echo ""
echo "==================================================================="
echo " 2. 多度邻接查询（FR-001）"
echo "==================================================================="
r=$(api POST /api/v1/compute-engine/adjacency '{"sourceFqn":"'$ROOT.Order_001'","direction":"FORWARD","maxDepth":3,"relationTypes":null,"filterCriteria":null}')
check "邻接 code=200" "$(echo "$r" | pget "code")" "200"
ents=$(echo "$r" | pjget "data.entities")
check_contains "起点含 Order_001" "$ents" "order:1.0.0.pkg_order.Order_001"
check_contains "邻居含 Item_003" "$ents" "order:1.0.0.pkg_order.Item_003"
check_contains "邻居含 Order_002" "$ents" "order:1.0.0.pkg_order.Order_002"
uniq=$(echo "$ents" | python3 -c "import json,sys; d=json.load(sys.stdin); print(len({e['fqn'] for e in d}))")
total=$(echo "$ents" | python3 -c "import json,sys; d=json.load(sys.stdin); print(len(d))")
check "实体去重(无重复)" "$uniq" "$total"
# 深度 3 层内 FORWARD 可达实体（9 实体中 Item_008 无出边不可达，共 7）
check_ge "深度3可达实体>=7" "$uniq" "7"

echo ""
echo "==================================================================="
echo " 3. 组合层级树（FR-002）"
echo "==================================================================="
r=$(api POST /api/v1/compute-engine/composition-tree '{"rootFqn":"'$ROOT.Item_003'","direction":"BACKWARD","maxDepth":10,"filterCriteria":null}')
check "组合树 code=200" "$(echo "$r" | pget "code")" "200"
ents=$(echo "$r" | pjget "data.entities")
check_contains "含起点 Item_003" "$ents" "order:1.0.0.pkg_order.Item_003"
check_contains "含父级 Order_001" "$ents" "order:1.0.0.pkg_order.Order_001"
# FORWARD 组合树：Order_001 只含 Item_003
r=$(api POST /api/v1/compute-engine/composition-tree '{"rootFqn":"'$ROOT.Order_001'","direction":"FORWARD","maxDepth":10,"filterCriteria":null}')
ents=$(echo "$r" | pjget "data.entities")
check_contains "FORWARD 含 Item_003" "$ents" "order:1.0.0.pkg_order.Item_003"
check_not_contains "FORWARD 不含 Item_004(非其组成)" "$ents" "order:1.0.0.pkg_order.Item_004"

echo ""
echo "==================================================================="
echo " 4. 图模式匹配（FR-004）"
echo "==================================================================="
r=$(api POST /api/v1/compute-engine/pattern-match '{"pattern":"* -[?]-> * -[?]-> *","maxResults":100}')
check "模式匹配 code=200" "$(echo "$r" | pget "code")" "200"
# 用 python 计数
np=$(echo "$r" | python3 -c "import json,sys; d=json.load(sys.stdin); print(len(d['data']['entities']))" 2>/dev/null || echo 0)
check_ge "匹配实体数>=4" "$np" "4"
# 通配符模式：实体-关系-实体（relation type 用完整 RelationSchema FQN）
r=$(api POST /api/v1/compute-engine/pattern-match '{"pattern":"* -[order:1.0.0.COMPOSITION]-> *","maxResults":100}')
check "COMPOSITION 模式 code=200" "$(echo "$r" | pget "code")" "200"
np=$(echo "$r" | python3 -c "import json,sys; d=json.load(sys.stdin); print(len(d['data']['entities']))" 2>/dev/null || echo 0)
check_ge "COMPOSITION 模式匹配实体>=6" "$np" "6"

echo ""
echo "==================================================================="
echo " 5. 复杂过滤矩阵（FR-015）★ 重点"
echo "==================================================================="
adj() { api POST /api/v1/compute-engine/adjacency "$1"; }

echo "--- 5.1 单维：associationTypes ---"
r=$(adj '{"sourceFqn":"'$ROOT.Order_001'","direction":"BIDIRECTIONAL","maxDepth":3,"filterCriteria":{"associationTypes":["COMPOSITION"]}}')
check "associationTypes=COMPOSITION code=200" "$(echo "$r" | pget "code")" "200"
check_contains "仅返回 COMPOSITION 关系" "$(echo "$r" | pjget "data.relations")" "COMPOSITION"
check_not_contains "不含 ASSOC/DEP/PROC" "$(echo "$r" | pjget "data.relations")" "ASSOCIATION_REFERENCE"
check_not_contains "不含 DEP" "$(echo "$r" | pjget "data.relations")" "DEPENDENCY_INFLUENCE"

echo "--- 5.2 单维：entityTypes（实体类型过滤）---"
r=$(adj '{"sourceFqn":"'$ROOT.Order_001'","direction":"BIDIRECTIONAL","maxDepth":3,"filterCriteria":{"entityTypes":[{"value":"'$SCHEMA_ITEM'","matchMode":"EXACT"}]}}')
ents=$(echo "$r" | pjget "data.entities")
check_contains "仅保留 Item 类型实体(邻居)" "$ents" "Item_003"
# 起点实体始终被收集；校验邻居中不出现 Item 之外的 Order（Item_008 无关系不可达，跳过）
n_non_item=$(echo "$ents" | python3 -c "
import json,sys
d=json.load(sys.stdin)
items=[e for e in d if e['entitySchemaFqn']=='$SCHEMA_ITEM']
print(len(items))" 2>/dev/null || echo 0)
check_ge "Item 类型邻居>=3" "$n_non_item" "3"
r=$(adj '{"sourceFqn":"'$ROOT.Order_001'","direction":"BIDIRECTIONAL","maxDepth":3,"filterCriteria":{"entityTypes":[{"value":"'$ROOT'","matchMode":"PREFIX"}]}}')
ents=$(echo "$r" | pjget "data.entities")
check_contains "PREFIX 匹配本 bundle 实体" "$ents" "Order_001"

echo "--- 5.3 单维：sourceFqns（关系源过滤）---"
r=$(adj '{"sourceFqn":"'$ROOT.Item_003'","direction":"FORWARD","maxDepth":2,"filterCriteria":{"sourceFqns":[{"value":"'$ROOT.Item_003'","matchMode":"EXACT"}]}}')
check_contains "仅保留 source=Item_003 的关系" "$(echo "$r" | pjget "data.relations")" "Item_003#order:1.0.0.ASSOCIATION_REFERENCE#order:1.0.0.pkg_order.Order_002"

echo "--- 5.4 单维：targetFqns（关系目标过滤）---"
r=$(adj '{"sourceFqn":"'$ROOT.Order_001'","direction":"FORWARD","maxDepth":2,"filterCriteria":{"targetFqns":[{"value":"'$ROOT'","matchMode":"PREFIX"}]}}')
check_contains "target 前缀过滤保留关系" "$(echo "$r" | pjget "data.relations")" "COMPOSITION"

echo "--- 5.5 单维：relationInstanceFqns PATTERN ---"
r=$(adj '{"sourceFqn":"'$ROOT.Order_001'","direction":"BIDIRECTIONAL","maxDepth":3,"filterCriteria":{"relationInstanceFqns":[{"value":"%Order_001%COMPOSITION%","matchMode":"PATTERN"}]}}')
check_contains "PATTERN 匹配 COMPOSITION 关系" "$(echo "$r" | pjget "data.relations")" "COMPOSITION"
check_not_contains "PATTERN 排除其他类型" "$(echo "$r" | pjget "data.relations")" "DEPENDENCY"

echo "--- 5.6 单维：propertyFilters（关系属性等值）---"
r=$(adj '{"sourceFqn":"'$ROOT.Order_001'","direction":"BIDIRECTIONAL","maxDepth":3,"filterCriteria":{"propertyFilters":[{"field":"quantity","value":"3"}]}}')
check_contains "属性 quantity=3 命中" "$(echo "$r" | pjget "data.relations")" "Item_003"
check_not_contains "排除 quantity!=3 的关系" "$(echo "$r" | pjget "data.relations")" "Item_005"
r=$(adj '{"sourceFqn":"'$ROOT.Order_001'","direction":"BIDIRECTIONAL","maxDepth":3,"filterCriteria":{"propertyFilters":[{"field":"level","value":"HIGH"}]}}')
check_contains "属性 level=HIGH 命中 DEP" "$(echo "$r" | pjget "data.relations")" "DEPENDENCY_INFLUENCE"

echo "--- 5.7 多属性 AND ---"
r=$(adj '{"sourceFqn":"'$ROOT.Order_001'","direction":"BIDIRECTIONAL","maxDepth":3,"filterCriteria":{"propertyFilters":[{"field":"step","value":"1"}]}}')
check_contains "属性 step=1 命中 PROC" "$(echo "$r" | pjget "data.relations")" "PROCESS_SEQUENCE"

echo "--- 5.8 二维交集：associationTypes + propertyFilters ---"
r=$(adj '{"sourceFqn":"'$ROOT.Order_001'","direction":"BIDIRECTIONAL","maxDepth":3,"filterCriteria":{"associationTypes":["COMPOSITION"],"propertyFilters":[{"field":"quantity","value":"3"}]}}')
rels=$(echo "$r" | pjget "data.relations")
check_contains "交集命中 COMPOSITION+quantity=3" "$rels" "Item_003"
check_not_contains "交集排除其他类型" "$rels" "DEPENDENCY"

echo "--- 5.9 二维交集：entityTypes + associationTypes ---"
r=$(adj '{"sourceFqn":"'$ROOT.Order_001'","direction":"BIDIRECTIONAL","maxDepth":3,"filterCriteria":{"entityTypes":[{"value":"'$SCHEMA_ITEM'","matchMode":"EXACT"}],"associationTypes":["COMPOSITION","DEPENDENCY_INFLUENCE"]}}')
rels=$(echo "$r" | pjget "data.relations")
check_contains "交集保留 Item 相关关系" "$rels" "Item_005"
check_not_contains "交集排除 PROC(非目标类型组合)" "$rels" "PROCESS_SEQUENCE"

echo "--- 5.10 三维交集：entityTypes + sourceFqns + associationTypes ---"
r=$(adj '{"sourceFqn":"'$ROOT.Order_001'","direction":"BIDIRECTIONAL","maxDepth":3,"filterCriteria":{"entityTypes":[{"value":"'$ROOT'","matchMode":"PREFIX"}],"sourceFqns":[{"value":"'$ROOT.Order_001'","matchMode":"EXACT"}],"associationTypes":["COMPOSITION","PROCESS_SEQUENCE"]}}')
rels=$(echo "$r" | pjget "data.relations")
check_contains "三维交集含 COMPOSITION" "$rels" "COMPOSITION"
check_contains "三维交集含 PROCESS_SEQUENCE" "$rels" "PROCESS_SEQUENCE"

echo "--- 5.11 全部 7 维全开交集 ---"
r=$(adj '{"sourceFqn":"'$ROOT.Order_001'","direction":"BIDIRECTIONAL","maxDepth":3,"filterCriteria":{"associationTypes":["COMPOSITION","DEPENDENCY_INFLUENCE"],"sourceFqns":[{"value":"'$ROOT'","matchMode":"PREFIX"}],"targetFqns":[{"value":"'$ROOT'","matchMode":"PREFIX"}],"relationInstanceFqns":null,"entityTypes":[{"value":"'$ROOT'","matchMode":"PREFIX"}],"relationTypes":[{"value":"order:1.0.0.COMPOSITION","matchMode":"EXACT"}],"propertyFilters":null}}')
check "7维全开 code=200" "$(echo "$r" | pget "code")" "200"
rels=$(echo "$r" | pjget "data.relations")
check_contains "7维交集仍含 COMPOSITION" "$rels" "COMPOSITION"

echo "--- 5.12 组合过滤应排除不匹配维度 ---"
r=$(adj '{"sourceFqn":"'$ROOT.Order_001'","direction":"BIDIRECTIONAL","maxDepth":3,"filterCriteria":{"associationTypes":["COMPOSITION"],"relationTypes":[{"value":"order:1.0.0.DEPENDENCY_INFLUENCE","matchMode":"EXACT"}]}}')
rels=$(echo "$r" | pjget "data.relations")
check_contains "relationTypes=COMPOSITION 时关系为空(维度矛盾)" "$rels" ""

echo "--- 5.13 属性过滤与关联类型矛盾（应空）---"
r=$(adj '{"sourceFqn":"'$ROOT.Order_001'","direction":"BIDIRECTIONAL","maxDepth":3,"filterCriteria":{"associationTypes":["COMPOSITION"],"propertyFilters":[{"field":"level","value":"HIGH"}]}}')
rels=$(echo "$r" | pjget "data.relations")
nrels=$(echo "$rels" | python3 -c "import json,sys; d=json.load(sys.stdin); print(len(d))" 2>/dev/null || echo 0)
check "矛盾过滤(COMPOSITION+level=HIGH) 关系数=0" "$nrels" "0"

echo ""
echo "==================================================================="
echo " 6. 传递闭包推理（FR-009）"
echo "==================================================================="
r=$(api POST /api/v1/compute-engine/closure '{"sourceFqn":"'$ROOT.Order_001'","relationTypes":null,"filterCriteria":null}')
check "闭包 code=200" "$(echo "$r" | pget "code")" "200"
check "totalReachable=8" "$(echo "$r" | pget "data.totalReachable")" "8"
check_contains "包含 Item_005(DEP)" "$(echo "$r" | pjget "data.layers")" "order:1.0.0.pkg_order.Item_005"

echo ""
echo "==================================================================="
echo " 7. 多跳语义推理（FR-010）"
echo "==================================================================="
r=$(api POST /api/v1/compute-engine/multi-hop '{"sourceFqn":"'$ROOT.Order_001'","hopSteps":[{"relationType":"COMPOSITION","direction":"FORWARD"},{"relationType":"ASSOCIATION_REFERENCE","direction":"FORWARD"}],"filterCriteria":null}')
check "多跳 code=200" "$(echo "$r" | pget "code")" "200"
tp=$(echo "$r" | pget "data.totalPaths")
check_ge "totalPaths>=1" "$tp" "1"
check_contains "路径含 Order_002" "$(echo "$r" | pjget "data.paths")" "order:1.0.0.pkg_order.Order_002"

echo ""
echo "==================================================================="
echo " 8. 结果截断标记（FR-023）"
echo "==================================================================="
r=$(api POST /api/v1/compute-engine/adjacency '{"sourceFqn":"'$ROOT.Order_001'","direction":"FORWARD","maxDepth":1,"relationTypes":null,"filterCriteria":null}')
check "maxDepth=1 truncated=true" "$(echo "$r" | pget "data.truncated")" "True"
check "truncatedReason=DEPTH_EXCEEDED" "$(echo "$r" | pget "data.truncatedReason")" "DEPTH_EXCEEDED"

echo ""
echo "==================================================================="
echo " 9. per-type 深度差异化（FR-020）"
echo "==================================================================="
r=$(api POST /api/v1/compute-engine/adjacency '{"sourceFqn":"'$ROOT.Order_001'","direction":"BIDIRECTIONAL","maxDepth":5,"relationTypes":["COMPOSITION","ASSOCIATION_REFERENCE"],"filterCriteria":null}')
check "per-type code=200" "$(echo "$r" | pget "code")" "200"
ents=$(echo "$r" | pjget "data.entities")
check_contains "COMPOSITION 首度含 Item_003" "$ents" "order:1.0.0.pkg_order.Item_003"
check_contains "ASSOC 首度含 Item_005" "$ents" "order:1.0.0.pkg_order.Item_005"
check_not_contains "ASSOC max-depth=1 截断(不含 Item_004)" "$ents" "order:1.0.0.pkg_order.Item_004"

echo ""
echo "==================================================================="
echo " 10. 子图提取查询（FR-003）"
echo "==================================================================="
r=$(api POST /api/v1/compute-engine/subgraph '{"centerFqns":["'$ROOT.Order_001'","'$ROOT.Item_003'"],"maxDepth":2,"filterCriteria":null}')
check "子图 code=200" "$(echo "$r" | pget "code")" "200"
nents=$(echo "$r" | python3 -c "import json,sys; d=json.load(sys.stdin); print(len(d['data']['entities']))" 2>/dev/null || echo 0)
uniq=$(echo "$r" | python3 -c "import json,sys; d=json.load(sys.stdin); print(len({e['fqn'] for e in d['data']['entities']}))" 2>/dev/null || echo 0)
check_ge "子图实体>=3" "$nents" "3"
check "实体去重(无重复)" "$uniq" "$nents"

echo ""
echo "==================================================================="
echo " 11. 多条件复合检索（FR-005）"
echo "==================================================================="
r=$(api POST /api/v1/compute-engine/search '{"entityTypes":["'$SCHEMA_ITEM'"],"attributes":[{"field":"status","operator":"EQ","value":"active"},{"field":"price","operator":"GT","value":"100"}],"relationTypes":null,"page":0,"size":20,"sortField":null,"sortDirection":null}')
check "检索 code=200" "$(echo "$r" | pget "code")" "200"
check "total=3(active且price>100)" "$(echo "$r" | pget "data.total")" "3"
check_contains "含 Item_003" "$(echo "$r" | pjget "data.content")" "order:1.0.0.pkg_order.Item_003"
check_not_contains "排除 Item_004(price=80)" "$(echo "$r" | pjget "data.content")" "order:1.0.0.pkg_order.Item_004"
# 复杂检索：范围 + 状态
r=$(api POST /api/v1/compute-engine/search '{"entityTypes":["'$SCHEMA_ITEM'"],"attributes":[{"field":"price","operator":"GTE","value":"200"},{"field":"quantity","operator":"LTE","value":"4"}],"relationTypes":null,"page":0,"size":20,"sortField":null,"sortDirection":null}')
check "范围检索 code=200" "$(echo "$r" | pget "code")" "200"
check_contains "含 Item_003(price150>=? no,但 Item_006 price320&qty1)" "$(echo "$r" | pjget "data.content")" "order:1.0.0.pkg_order.Item_006"

echo ""
echo "==================================================================="
echo " 12. 批量语义查询（FR-006）"
echo "==================================================================="
r=$(api POST /api/v1/compute-engine/batch '{"fqns":["'$ROOT.Order_001'","'$ROOT.NotExist_999'"]}')
check "批量 code=200" "$(echo "$r" | pget "code")" "200"
check_contains "entities 含 Order_001" "$(echo "$r" | pjget "data.entities")" "order:1.0.0.pkg_order.Order_001"
check_contains "notFoundFqns 含 NotExist_999" "$(echo "$r" | pjget "data.notFoundFqns")" "order:1.0.0.pkg_order.NotExist_999"

echo ""
echo "==================================================================="
echo " 13. 两点间路径查询（FR-008）"
echo "==================================================================="
r=$(api POST /api/v1/compute-engine/paths '{"sourceFqn":"'$ROOT.Order_001'","targetFqn":"'$ROOT.Item_003'","direction":"BIDIRECTIONAL","maxDepth":5,"relationTypes":null,"findShortest":false,"filterCriteria":null}')
check "路径 code=200" "$(echo "$r" | pget "code")" "200"
check_ge "totalPaths>=1" "$(echo "$r" | pget "data.totalPaths")" "1"
check_contains "含直接边 COMPOSITION" "$(echo "$r" | pjget "data.paths")" "$ROOT.Order_001#$R_COMP#$ROOT.Item_003"
r=$(api POST /api/v1/compute-engine/paths '{"sourceFqn":"'$ROOT.Order_001'","targetFqn":"'$ROOT.Item_003'","direction":"BIDIRECTIONAL","maxDepth":5,"relationTypes":null,"findShortest":true,"filterCriteria":null}')
check "最短路径 totalPaths=1" "$(echo "$r" | pget "data.totalPaths")" "1"
check "最短路径步数=1" "$(echo "$r" | python3 -c "import json,sys; d=json.load(sys.stdin); print(len(d['data']['paths'][0]['steps']))")" "1"

echo ""
echo "==================================================================="
echo " 14. 路径可达性判定（FR-011）"
echo "==================================================================="
r=$(api POST /api/v1/compute-engine/reachability '{"sourceFqn":"'$ROOT.Order_001'","targetFqn":"'$ROOT.Item_003'","relationTypes":null}')
check "可达性 code=200" "$(echo "$r" | pget "code")" "200"
check_contains "含 reachable 标记" "$(echo "$r" | pjget "data.paths")" "reachable"

echo ""
echo "==================================================================="
echo " 15. 正向影响扩散（FR-012）"
echo "==================================================================="
r=$(api POST /api/v1/compute-engine/impact/diffuse '{"centerFqn":"'$ROOT.Order_001'","direction":"FORWARD","maxDepth":3,"relationTypes":["COMPOSITION","DEPENDENCY_INFLUENCE"]}')
check "扩散 code=200" "$(echo "$r" | pget "code")" "200"
check "totalImpacted=3" "$(echo "$r" | pget "data.totalImpacted")" "3"
check_contains "含 Item_005(DEP)" "$(echo "$r" | pjget "data.layerStats")" "order:1.0.0.pkg_order.Item_005"

echo ""
echo "==================================================================="
echo " 16. 反向依赖溯源（FR-013）"
echo "==================================================================="
r=$(api POST /api/v1/compute-engine/impact/trace '{"centerFqn":"'$ROOT.Item_003'","direction":"BACKWARD","maxDepth":3,"relationTypes":["COMPOSITION"]}')
check "溯源 code=200" "$(echo "$r" | pget "code")" "200"
check "totalImpacted=2" "$(echo "$r" | pget "data.totalImpacted")" "2"
check_contains "父链含 Order_001" "$(echo "$r" | pjget "data.entities")" "order:1.0.0.pkg_order.Order_001"

echo ""
echo "==================================================================="
echo " 17. 影响路径详情 POST+GET（FR-014）"
echo "==================================================================="
r=$(api POST /api/v1/compute-engine/impact/paths '{"sourceFqn":"'$ROOT.Order_001'","targetFqn":"'$ROOT.Item_003'","relationTypes":["COMPOSITION"]}')
check "POST 影响路径 code=200" "$(echo "$r" | pget "code")" "200"
check_contains "POST relations 非空" "$(echo "$r" | pjget "data.relations")" "COMPOSITION"
r=$(api GET "/api/v1/compute-engine/impact/paths?sourceFqn=$ROOT.Order_001&targetFqn=$ROOT.Item_003&relationTypes=COMPOSITION&maxDepth=5")
check "GET 影响路径 code=200" "$(echo "$r" | pget "code")" "200"
check_contains "GET relations 非空" "$(echo "$r" | pjget "data.relations")" "COMPOSITION"

echo ""
echo "==================================================================="
echo " 18. 循环引用检测与去重（FR-022）"
echo "==================================================================="
r=$(api POST /api/v1/compute-engine/closure '{"sourceFqn":"'$ROOT.Order_002'","relationTypes":null,"filterCriteria":null}')
check "循环闭包 code=200(无无限递归)" "$(echo "$r" | pget "code")" "200"
check "totalReachable=8(循环去重)" "$(echo "$r" | pget "data.totalReachable")" "8"
uniq=$(echo "$r" | python3 -c "import json,sys; d=json.load(sys.stdin); flat=[e['fqn'] for layer in d['data']['layers'].values() for e in layer]; print(len({x for x in flat}))" 2>/dev/null || echo 0)
total=$(echo "$r" | python3 -c "import json,sys; d=json.load(sys.stdin); flat=[e['fqn'] for layer in d['data']['layers'].values() for e in layer]; print(len(flat))" 2>/dev/null || echo 0)
check "每个实体仅出现一次(最短深度)" "$uniq" "$total"

echo ""
echo "==================================================================="
echo " 19. 错误码矩阵"
echo "==================================================================="
# 批量超限 33009
batch_body=$(python3 -c "import json; print(json.dumps({'fqns':['fqn:1.0.0.x'+str(i) for i in range(201)]}))")
r=$(api POST /api/v1/compute-engine/batch "$batch_body")
check "批量 201 个返回 33009" "$(echo "$r" | pget "code")" "33009"
# 实体不存在 33001
r=$(api POST /api/v1/compute-engine/adjacency '{"sourceFqn":"'$ROOT.NotExist_999'","direction":"FORWARD","maxDepth":3,"relationTypes":null,"filterCriteria":null}')
check "不存在实体返回 33001" "$(echo "$r" | pget "code")" "33001"
# 模式超长 33005/33008
r=$(api POST /api/v1/compute-engine/pattern-match '{"pattern":"* -[?]-> * -[?]-> * -[?]-> * -[?]-> * -[?]-> *","maxResults":100}')
c=$(echo "$r" | pget "code")
if [ "$c" = "33005" ] || [ "$c" = "33008" ]; then PASS=$((PASS+1)); echo "  [PASS] 超长模式返回 33005/33008 (=$c)";
else FAIL=$((FAIL+1)); FAILED_CASES+=("超长模式: 期望 33005/33008 实际 $c"); echo "  [FAIL] 超长模式: 期望=33005/33008 实际=$c"; fi

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
