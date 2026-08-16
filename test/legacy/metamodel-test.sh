#!/usr/bin/env bash
# =============================================================================
# quickstart 场景测试脚本（排除场景 3: FQN 统一生成器）— 复杂测试数据版
#
# 覆盖:
#   场景 1: 完整 Bundle 生命周期（User Story 1 + 2）
#   场景 2: 属性名冲突校验（Edge Case）
#   场景 4: 发布校验 — 升级等级不匹配（User Story 2.4）
#   场景 5: FQN 版本省略解析（Edge Case）
#
# 测试数据复杂度:
#   - 3 个 Package（含 1 个多级子 Package 链）
#   - 3 个 AttributeTemplate（审计 / 数据库 / 价格）
#   - 5 个 EntitySchema（含丰富 constraints: pattern/minimum/format/enum）
#   - 4 个 RelationSchema（含全部 5 种关联类型中的 4 种 + 跨 Package 关系）
#   - jsonSchema 平铺合并断言（原生 + 多模板 共 9+ 属性）
#
# 前置条件:
#   - PostgreSQL 容器 metaforge-postgres 已运行（RESET_DB=1 时会自动重置其数据）
#   - metaforge-boot 已构建（RESET_DB=1 时会自动重启应用，见下）
# 用法:
#   bash metamodel-test.sh [BASE_URL]   # 应用需已启动且连干净库
#   RESET_DB=1 bash metamodel-test.sh.sh   # 自动: 重置 postgres 容器 → 重启应用(动态定位 jar) → 执行
#   LOG_REQ=0 bash metamodel-test.sh.sh    # 关闭请求/响应日志（默认开启）
#   JAR_PATH=/path/to/app.jar                 # 覆盖 jar 路径（默认由仓库目录动态推导）
# =============================================================================

set -u

BASE_URL="${1:-http://localhost:8080}"
API="$BASE_URL/api/v1/metamodel"
CT="Content-Type: application/json"
LOG_REQ="${LOG_REQ:-1}"

# 动态定位 metaforge-boot jar：从脚本所在目录向上定位仓库根，再进兄弟模块 target 通配匹配。
# 不写死版本号/绝对路径；可用环境变量 JAR_PATH 覆盖。
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
resolve_jar() {
  local dir="$SCRIPT_DIR"
  while [ "$dir" != "/" ]; do
    for cand in "$dir/metaforge-boot"/target/metaforge-boot-*.jar; do
      [ -f "$cand" ] && { printf '%s' "$cand"; return 0; }
    done
    dir="$(dirname "$dir")"
  done
  return 1
}
JAR_PATH="${JAR_PATH:-$(resolve_jar 2>/dev/null || true)}"

# RESET_DB=1: 重置 postgres 容器数据并重启应用（清旧连接、重新跑 Flyway），使脚本可重复执行。
# 注意：应用持有到旧库的连接，重置后必须重启应用，否则所有请求 10000。
if [ "${RESET_DB:-0}" = "1" ]; then
  if command -v docker >/dev/null 2>&1 && docker ps --format '{{.Names}}' | grep -q '^metaforge-postgres$'; then
    echo ">> 重置数据库容器 (metaforge-postgres)..."
    docker restart metaforge-postgres >/dev/null
    sleep 6
  else
    echo "!! 未检测到 metaforge-postgres 容器，跳过数据库重置。" >&2
  fi
  if [ -n "$JAR_PATH" ] && [ -f "$JAR_PATH" ]; then
    echo ">> 停止运行中的 metaforge-boot..."
    pkill -f 'metaforge-boot-[0-9].*\.jar' 2>/dev/null
    sleep 3
    echo ">> 启动 metaforge-boot ($JAR_PATH)..."
    LOG_FILE="${LOG_FILE:-/tmp/opencode/metaforge-boot.log}"
    nohup java -jar "$JAR_PATH" >"$LOG_FILE" 2>&1 &
    echo ">> 等待应用就绪..."
    UP=0
    for _ in $(seq 1 60); do
      if curl -sf "$BASE_URL/actuator/health" >/dev/null 2>&1; then UP=1; break; fi
      sleep 3
    done
    if [ "$UP" != "1" ]; then
      echo "!! 应用启动超时，请检查日志: $LOG_FILE" >&2
      exit 1
    fi
    echo ">> 应用已就绪。"
  else
    echo "!! 未找到 metaforge-boot jar（可用 JAR_PATH 指定），请手动启动应用后重试。" >&2
    exit 1
  fi
fi

PASS=0
FAIL=0
FAILED_NAMES=()

log()  { printf '%-8s %s\n' "$1" "$2"; }
ok()   { PASS=$((PASS+1)); log "PASS" "$1"; }
bad()  { FAIL=$((FAIL+1)); FAILED_NAMES+=("$1"); log "FAIL" "$1  [$2]"; }

# assert_json <name> <path> <expected> <actual-json>
# 点路径取值并转字符串比较。
assert_json() {
  local name="$1" path="$2" expected="$3" json="$4"
  local actual
  actual="$(python3 -c "
import json,sys
d=json.load(sys.stdin)
for k in '$path'.split('.'):
    d = d[k] if isinstance(d,dict) else None
print('' if d is None else d)
" <<<"$json" 2>/dev/null)"
  if [ "$actual" = "$expected" ]; then
    ok "$name"
  else
    bad "$name" "expected $path=$expected, got '$actual'"
  fi
}

assert_code() { assert_json "$1" "code" "$2" "$3"; }

# api_call <method> <url> [json-body]  → 响应 JSON（LOG_REQ=1 时打印请求/响应日志到 stderr）
api_call() {
  local method="$1" url="$2" body="${3:-}"
  local resp
  if [ -n "$body" ]; then
    resp="$(curl -s -X "$method" "$url" -H "$CT" -d "$body")"
  else
    resp="$(curl -s -X "$method" "$url")"
  fi
  if [ "$LOG_REQ" = "1" ]; then
    {
      printf '\n>> REQ  %s %s\n' "$method" "$url"
      if [ -n "$body" ]; then
        printf '>> BODY %s\n' "$(printf '%s' "$body" | python3 -m json.tool --compact 2>/dev/null || printf '%s' "$body")"
      fi
      printf '>> RESP %s\n' "$(printf '%s' "$resp" | python3 -m json.tool --compact 2>/dev/null || printf '%s' "$resp")"
    } >&2
  fi
  printf '%s' "$resp"
}

echo "============================================================"
echo " quickstart 场景测试 (排除场景 3) — 复杂数据版  BASE_URL=$BASE_URL"
echo "============================================================"

# ---------------------------------------------------------------------------
# 场景 1: 完整 Bundle 生命周期
# ---------------------------------------------------------------------------
echo ""
echo "── 场景 1: 完整 Bundle 生命周期 ──"

# 1. 创建 Bundle
R=$(api_call POST "$API/bundles" '{
  "fqn": "order",
  "name": "订单领域",
  "description": "覆盖电商订单的完整生命周期建模，含订单创建、支付、履约、售后、客户管理。",
  "owner": "zhangsan"
}')
assert_code "S1.1 创建 Bundle" "200" "$R"

# 2a. 根 Package pkg_order
R=$(api_call POST "$API/packages" '{
  "bundleVersionFqn": "order:0.0.1",
  "parentPackageFqn": null,
  "segment": "pkg_order",
  "description": "订单领域命名空间（订单/履约核心包）"
}')
assert_code "S1.2 创建根 Package pkg_order" "200" "$R"
assert_json  "S1.2 Package FQN" "data.fqn" "order:0.0.1.pkg_order" "$R"

# 2b. 多级子 Package pkg_order.fulfillment
R=$(api_call POST "$API/packages" '{
  "bundleVersionFqn": "order:0.0.1",
  "parentPackageFqn": "order:0.0.1.pkg_order",
  "segment": "fulfillment",
  "description": "履约子命名空间（物流/仓库）"
}')
assert_code "S1.2b 创建子 Package fulfillment" "200" "$R"
assert_json  "S1.2b 子 Package FQN" "data.fqn" "order:0.0.1.pkg_order.fulfillment" "$R"

# 2c. 第二个根 Package pkg_customer
R=$(api_call POST "$API/packages" '{
  "bundleVersionFqn": "order:0.0.1",
  "parentPackageFqn": null,
  "segment": "pkg_customer",
  "description": "客户领域命名空间（客户/地址）"
}')
assert_code "S1.2c 创建根 Package pkg_customer" "200" "$R"

# 3a. AttributeTemplate: AuditFields（审计字段，4 属性）
R=$(api_call POST "$API/attribute-templates" '{
  "bundleVersionFqn": "order:0.0.1",
  "segment": "AuditFields",
  "name": "审计字段模板",
  "attributeDefinitions": [
    {"name": "createdBy", "type": "string", "required": true, "description": "创建人"},
    {"name": "createdAt", "type": "string", "required": true, "description": "创建时间", "constraints": {"format": "date-time"}},
    {"name": "updatedBy", "type": "string", "description": "更新人"},
    {"name": "updatedAt", "type": "string", "description": "更新时间", "constraints": {"format": "date-time"}}
  ]
}')
assert_code "S1.3a 创建 AuditFields 模板" "200" "$R"
assert_json  "S1.3a Template FQN" "data.fqn" "order:0.0.1.AuditFields" "$R"

# 3b. AttributeTemplate: DbFields（数据库字段模板）
R=$(api_call POST "$API/attribute-templates" '{
  "bundleVersionFqn": "order:0.0.1",
  "segment": "DbFields",
  "name": "数据库字段模板",
  "attributeDefinitions": [
    {"name": "dbVersion", "type": "integer", "required": true, "description": "乐观锁版本", "constraints": {"minimum": 0}},
    {"name": "deleted", "type": "boolean", "description": "逻辑删除标记"}
  ]
}')
assert_code "S1.3b 创建 DbFields 模板" "200" "$R"

# 3c. AttributeTemplate: PriceFields（价格字段模板）
R=$(api_call POST "$API/attribute-templates" '{
  "bundleVersionFqn": "order:0.0.1",
  "segment": "PriceFields",
  "name": "价格字段模板",
  "attributeDefinitions": [
    {"name": "currency", "type": "string", "required": true, "description": "币种", "constraints": {"enum": ["CNY","USD","EUR"]}},
    {"name": "amount", "type": "number", "required": true, "description": "金额", "constraints": {"minimum": 0}}
  ]
}')
assert_code "S1.3c 创建 PriceFields 模板" "200" "$R"

# 4a. EntitySchema: Order（挂 3 模板 + 原生属性）
R=$(api_call POST "$API/entity-schemas" '{
  "packageFqn": "order:0.0.1.pkg_order",
  "segment": "Order",
  "name": "订单实体",
  "description": "描述电商订单的核心概念。适用场景：订单创建、支付、履约。",
  "nativeAttributes": [
    {"name": "orderNo", "type": "string", "required": true, "description": "订单号", "constraints": {"pattern": "^ORD-[0-9]{8}$"}},
    {"name": "orderStatus", "type": "string", "required": true, "description": "订单状态", "constraints": {"enum": ["CREATED","PAID","SHIPPED","COMPLETED","CANCELLED"]}},
    {"name": "orderAmount", "type": "number", "required": true, "description": "订单总金额", "constraints": {"minimum": 0, "maximum": 999999.99}}
  ],
  "mountedTemplateFqns": ["order:0.0.1.AuditFields", "order:0.0.1.DbFields", "order:0.0.1.PriceFields"]
}')
assert_code "S1.4a 创建 EntitySchema Order" "200" "$R"
assert_json  "S1.4a Entity FQN" "data.fqn" "order:0.0.1.pkg_order.Order" "$R"
assert_json  "S1.4a DRAFT enabled=false" "data.enabled" "False" "$R"

# 4b. EntitySchema: Item（挂 2 模板）
R=$(api_call POST "$API/entity-schemas" '{
  "packageFqn": "order:0.0.1.pkg_order",
  "segment": "Item",
  "name": "订单项",
  "description": "订单内单个商品项。适用场景：商品下单。",
  "nativeAttributes": [
    {"name": "sku", "type": "string", "required": true, "description": "商品 SKU", "constraints": {"pattern": "^[A-Z]{2}-[0-9]{4,10}$"}},
    {"name": "quantity", "type": "integer", "required": true, "description": "数量", "constraints": {"minimum": 1, "maximum": 999}}
  ],
  "mountedTemplateFqns": ["order:0.0.1.AuditFields", "order:0.0.1.PriceFields"]
}')
assert_code "S1.4b 创建 EntitySchema Item" "200" "$R"

# 4c. EntitySchema: Shipment（子 Package 下）
R=$(api_call POST "$API/entity-schemas" '{
  "packageFqn": "order:0.0.1.pkg_order.fulfillment",
  "segment": "Shipment",
  "name": "发货单",
  "description": "履约发货单。适用场景：出库、物流跟踪。",
  "nativeAttributes": [
    {"name": "trackingNo", "type": "string", "description": "物流单号", "constraints": {"pattern": "^[A-Z0-9]{10,20}$"}},
    {"name": "shippedAt", "type": "string", "description": "发货时间", "constraints": {"format": "date-time"}}
  ],
  "mountedTemplateFqns": ["order:0.0.1.AuditFields"]
}')
assert_code "S1.4c 创建 EntitySchema Shipment(子包)" "200" "$R"
assert_json  "S1.4c Shipment FQN" "data.fqn" "order:0.0.1.pkg_order.fulfillment.Shipment" "$R"

# 4d. EntitySchema: Customer
R=$(api_call POST "$API/entity-schemas" '{
  "packageFqn": "order:0.0.1.pkg_customer",
  "segment": "Customer",
  "name": "客户",
  "description": "下单客户。适用场景：客户管理。",
  "nativeAttributes": [
    {"name": "name", "type": "string", "required": true, "description": "客户姓名"},
    {"name": "phone", "type": "string", "description": "手机号", "constraints": {"pattern": "^1[3-9][0-9]{9}$"}}
  ],
  "mountedTemplateFqns": ["order:0.0.1.AuditFields"]
}')
assert_code "S1.4d 创建 EntitySchema Customer" "200" "$R"

# 4e. EntitySchema: Address
R=$(api_call POST "$API/entity-schemas" '{
  "packageFqn": "order:0.0.1.pkg_customer",
  "segment": "Address",
  "name": "地址",
  "description": "收货地址。适用场景：配送。",
  "nativeAttributes": [
    {"name": "province", "type": "string", "required": true, "description": "省份"},
    {"name": "city", "type": "string", "required": true, "description": "城市"},
    {"name": "detail", "type": "string", "description": "详细地址"}
  ],
  "mountedTemplateFqns": ["order:0.0.1.AuditFields"]
}')
assert_code "S1.4e 创建 EntitySchema Address" "200" "$R"

# 5a. RelationSchema: Order_contains_Item（组成 1 : 1..*）
R=$(api_call POST "$API/relation-schemas" '{
  "packageFqn": "order:0.0.1.pkg_order",
  "segment": "Order_contains_Item",
  "name": "订单包含商品",
  "description": "订单与订单项的组成关系",
  "sourceFqn": "order:0.0.1.pkg_order.Order",
  "targetFqn": "order:0.0.1.pkg_order.Item",
  "associationType": "组成",
  "cardinalitySource": "1",
  "cardinalityTarget": "1..*"
}')
assert_code "S1.5a 创建 RelationSchema 组成关系" "200" "$R"
assert_json  "S1.5a Relation FQN" "data.fqn" "order:0.0.1.pkg_order.Order_contains_Item" "$R"
assert_json  "S1.5a associationType" "data.associationType" "COMPOSITION" "$R"

# 5b. RelationSchema: Order_placedBy_Customer（关联引用 1 : 1，跨 Package）
R=$(api_call POST "$API/relation-schemas" '{
  "packageFqn": "order:0.0.1.pkg_order",
  "segment": "Order_placedBy_Customer",
  "name": "订单由客户下单",
  "description": "订单与客户的关联引用",
  "sourceFqn": "order:0.0.1.pkg_order.Order",
  "targetFqn": "order:0.0.1.pkg_customer.Customer",
  "associationType": "关联引用",
  "cardinalitySource": "1",
  "cardinalityTarget": "1"
}')
assert_code "S1.5b 创建 RelationSchema 关联引用(跨包)" "200" "$R"
assert_json  "S1.5b associationType" "data.associationType" "ASSOCIATION_REFERENCE" "$R"

# 5c. RelationSchema: Order_shipsTo_Address（依赖影响 1 : 1）
R=$(api_call POST "$API/relation-schemas" '{
  "packageFqn": "order:0.0.1.pkg_order",
  "segment": "Order_shipsTo_Address",
  "name": "订单配送到地址",
  "description": "订单到配送地址的依赖影响",
  "sourceFqn": "order:0.0.1.pkg_order.Order",
  "targetFqn": "order:0.0.1.pkg_customer.Address",
  "associationType": "依赖影响",
  "cardinalitySource": "1",
  "cardinalityTarget": "1"
}')
assert_code "S1.5c 创建 RelationSchema 依赖影响" "200" "$R"
assert_json  "S1.5c associationType" "data.associationType" "DEPENDENCY_INFLUENCE" "$R"

# 5d. RelationSchema: Customer_owns_Address（映射对应 1 : 1..*）
R=$(api_call POST "$API/relation-schemas" '{
  "packageFqn": "order:0.0.1.pkg_customer",
  "segment": "Customer_owns_Address",
  "name": "客户拥有地址",
  "description": "客户与地址的映射对应",
  "sourceFqn": "order:0.0.1.pkg_customer.Customer",
  "targetFqn": "order:0.0.1.pkg_customer.Address",
  "associationType": "映射对应",
  "cardinalitySource": "1",
  "cardinalityTarget": "1..*"
}')
assert_code "S1.5d 创建 RelationSchema 映射对应" "200" "$R"
assert_json  "S1.5d associationType" "data.associationType" "MAPPING_CORRESPONDENCE" "$R"

# 6. 导出清单（含 3 个 Package，含子 Package）
R=$(api_call PUT "$API/versions/order:0.0.1/export-manifest" '{
  "packageFqns": ["order:0.0.1.pkg_order", "order:0.0.1.pkg_order.fulfillment", "order:0.0.1.pkg_customer"]
}')
assert_code "S1.6 配置导出清单(多包)" "200" "$R"
if python3 -c "
import json,sys
d=json.load(sys.stdin)
arr=d['data']['exportedPackageFqns']
assert arr==['order:0.0.1.pkg_order','order:0.0.1.pkg_order.fulfillment','order:0.0.1.pkg_customer'], f'unexpected: {arr}'
print('ok')
" <<<"$R" 2>/dev/null; then
  ok "S1.6 导出清单包含 3 个包（含子包）"
else
  bad "S1.6 导出清单包含 3 个包" "导出包列表不符合预期"
fi

# 7. 发布版本
R=$(api_call POST "$API/versions/order:0.0.1/publish")
assert_code "S1.7 发布版本" "200" "$R"
assert_json  "S1.7 status=PUBLISHED" "data.status" "PUBLISHED" "$R"

# 8. 验证 Order json_schema 已固化（含 3 原生 + 4审计 + 2数据库 + 2价格 = 9 属性）
R=$(api_call GET "$API/entity-schemas/order:0.0.1.pkg_order.Order")
assert_code "S1.8 查询 Order" "200" "$R"
if python3 -c "
import json,sys
d=json.load(sys.stdin)['data']
assert d['enabled'] is True, 'enabled should be True after publish'
assert d['embedding'] is None, 'embedding should be null (MVP placeholder)'
assert d['name']=='订单实体' and d['fqn'].endswith('.Order'), 'name independent from fqn shortname'
js=d.get('jsonSchema') or ''
expect=['orderNo','orderStatus','orderAmount','createdBy','createdAt','updatedBy','updatedAt','dbVersion','deleted','currency','amount']
missing=[a for a in expect if a not in js]
assert not missing, f'missing in jsonSchema: {missing}'
print('ok')
" <<<"$R" 2>/dev/null; then
  ok "S1.8 验证要点（jsonSchema 平铺合并 9 属性 / name 独立 / embedding null / enabled=True）"
else
  bad "S1.8 验证要点" "jsonSchema 合并/embedding/enabled/name 校验未通过"
fi

# 9. 验证 Customer（仅 1 模板）json_schema
R=$(api_call GET "$API/entity-schemas/order:0.0.1.pkg_customer.Customer")
assert_code "S1.9 查询 Customer" "200" "$R"
if python3 -c "
import json,sys
d=json.load(sys.stdin)['data']
js=d.get('jsonSchema') or ''
for a in ['name','phone','createdBy','createdAt']:
    assert a in js, f'missing {a} in Customer jsonSchema'
print('ok')
" <<<"$R" 2>/dev/null; then
  ok "S1.9 Customer jsonSchema（原生+审计模板）"
else
  bad "S1.9 Customer jsonSchema" "属性缺失"
fi

# ---------------------------------------------------------------------------
# 场景 2: 属性名冲突校验
# ---------------------------------------------------------------------------
echo ""
echo "── 场景 2: 属性名冲突校验 ──"

# 1. 创建草稿版本 v0.1.0（MINOR，从已发布 v0.0.1 复制全部内容）
R=$(api_call POST "$API/bundles/order/versions" '{"upgradeLevel": "MINOR"}')
assert_code "S2.1 创建草稿版本 v0.1.0" "200" "$R"
assert_json  "S2.1 新版本 FQN" "data.fqn" "order:0.1.0" "$R"
assert_json  "S2.1 sourceVersionFqn" "data.sourceVersionFqn" "order:0.0.1" "$R"

# 2. FR-020 验证：草稿复制了源版本内容（Shipment 子包实体被复制到 v0.1.0）
R=$(api_call GET "$API/entity-schemas/order:0.1.0.pkg_order.fulfillment.Shipment")
assert_code "S2.2 草稿复制 Shipment(FR-020)" "200" "$R"

# 3. 创建 v0.1.0 模板 DbFields2（含同名 createdBy 用于冲突）
R=$(api_call POST "$API/attribute-templates" '{
  "bundleVersionFqn": "order:0.1.0",
  "segment": "DbFields2",
  "name": "数据库字段模板二",
  "attributeDefinitions": [
    {"name": "createdBy", "type": "string", "required": true, "description": "数据库创建人"},
    {"name": "rowVersion", "type": "integer", "description": "行版本号"}
  ]
}')
assert_code "S2.3 创建 DbFields2 模板" "200" "$R"

# 4. 挂载 AuditFields + DbFields2（createdBy 冲突）→ 30106
R=$(api_call POST "$API/entity-schemas" '{
  "packageFqn": "order:0.1.0.pkg_order",
  "segment": "ConflictTest",
  "name": "冲突测试实体",
  "description": "测试属性名冲突校验。适用场景：测试。",
  "mountedTemplateFqns": ["order:0.0.1.AuditFields", "order:0.1.0.DbFields2"]
}')
assert_code "S2.4 冲突被拦截(30106)" "30106" "$R"
if python3 -c "
import json,sys
d=json.load(sys.stdin)
assert 'createdBy' in d.get('message',''), 'message should mention createdBy'
print('ok')
" <<<"$R" 2>/dev/null; then
  ok "S2.4 错误信息包含冲突字段名 createdBy"
else
  bad "S2.4 错误信息包含冲突字段名" "message 缺少 createdBy"
fi

# 5. 原生属性与模板属性冲突（原生 amount + PriceFields.amount）→ 30106
R=$(api_call POST "$API/entity-schemas" '{
  "packageFqn": "order:0.1.0.pkg_order",
  "segment": "AmountConflict",
  "name": "金额冲突实体",
  "description": "原生属性与模板属性重名。适用场景：测试。",
  "nativeAttributes": [
    {"name": "amount", "type": "number", "required": true, "description": "原生金额"}
  ],
  "mountedTemplateFqns": ["order:0.0.1.PriceFields"]
}')
assert_code "S2.5 原生vs模板冲突(30106)" "30106" "$R"

# ---------------------------------------------------------------------------
# 场景 4: 发布校验 — 升级等级不匹配
# ---------------------------------------------------------------------------
echo ""
echo "── 场景 4: 发布校验 — 升级等级不匹配 ──"

# 1. 删除草稿中的 EntitySchema（复制过来的 Order 与 Item）
R=$(api_call DELETE "$API/entity-schemas/order:0.1.0.pkg_order.Order")
assert_code "S4.1 删除草稿 Order" "200" "$R"
R=$(api_call DELETE "$API/entity-schemas/order:0.1.0.pkg_order.Item")
assert_code "S4.2 删除草稿 Item" "200" "$R"

# 2. 尝试发布 → MINOR 含删除 → 30104
R=$(api_call POST "$API/versions/order:0.1.0/publish")
assert_code "S4.3 MINOR 含删除被阻止(30104)" "30104" "$R"
if python3 -c "
import json,sys
d=json.load(sys.stdin)
assert 'MINOR' in d.get('message','') and ('删除' in d.get('message','') or '不匹配' in d.get('message','')), 'message should mention MINOR + deletion'
print('ok')
" <<<"$R" 2>/dev/null; then
  ok "S4.3 错误提示声明 MINOR 升级与删除不匹配"
else
  bad "S4.3 错误提示声明 MINOR 升级与删除不匹配" "message 不符合预期"
fi

# 3. 发布校验失败后版本仍为 DRAFT（未被错误修改）
R=$(api_call GET "$API/versions/order:0.1.0")
assert_code "S4.4 版本仍可查询" "200" "$R"
assert_json  "S4.4 失败后仍为 DRAFT" "data.status" "DRAFT" "$R"

# ---------------------------------------------------------------------------
# 场景 5: FQN 版本省略解析
# ---------------------------------------------------------------------------
echo ""
echo "── 场景 5: FQN 版本省略解析 ──"

# 1. 普通实体（版本省略）
R=$(api_call POST "$API/tools/resolve-fqn" '{"fqn": "order.pkg_order.Order"}')
assert_code "S5.1 resolve-fqn Order" "200" "$R"
assert_json  "S5.1 resolvedFqn 为最新已发布版本" "data.resolvedFqn" "order:0.0.1.pkg_order.Order" "$R"

# 2. 子 Package 实体
R=$(api_call POST "$API/tools/resolve-fqn" '{"fqn": "order.pkg_order.fulfillment.Shipment"}')
assert_code "S5.2 resolve-fqn Shipment" "200" "$R"
assert_json  "S5.2 子包 resolvedFqn" "data.resolvedFqn" "order:0.0.1.pkg_order.fulfillment.Shipment" "$R"

# 3. 第二个 Package 实体
R=$(api_call POST "$API/tools/resolve-fqn" '{"fqn": "order.pkg_customer.Customer"}')
assert_code "S5.3 resolve-fqn Customer" "200" "$R"
assert_json  "S5.3 跨包 resolvedFqn" "data.resolvedFqn" "order:0.0.1.pkg_customer.Customer" "$R"

# 4. Bundle 级（无 path 段）
R=$(api_call POST "$API/tools/resolve-fqn" '{"fqn": "order"}')
assert_code "S5.4 resolve-fqn Bundle" "200" "$R"
assert_json  "S5.4 Bundle resolvedFqn" "data.resolvedFqn" "order:0.0.1" "$R"

# 5. 类型前缀剥离 + 版本省略
R=$(api_call POST "$API/tools/resolve-fqn" '{"fqn": "entity:order.pkg_order.Item"}')
assert_code "S5.5 resolve-fqn 类型前缀" "200" "$R"
assert_json  "S5.5 前缀剥离后解析" "data.resolvedFqn" "order:0.0.1.pkg_order.Item" "$R"

# ---------------------------------------------------------------------------
# 汇总
# ---------------------------------------------------------------------------
echo ""
echo "============================================================"
echo " 结果: PASS=$PASS FAIL=$FAIL"
if [ ${#FAILED_NAMES[@]} -gt 0 ]; then
  printf ' 失败项: %s\n' "${FAILED_NAMES[*]}"
fi
echo "============================================================"
[ "$FAIL" -eq 0 ]
