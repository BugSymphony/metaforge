package com.metaforge.agent.cognition.operator.common;

import com.metaforge.agent.cognition.api.dto.request.Scope;
import com.metaforge.agent.cognition.api.port.ComputeEngineReadPort;
import com.metaforge.agent.cognition.api.port.GraphReadPort;
import com.metaforge.agent.cognition.api.port.MetadataReadPort;
import com.metaforge.agent.cognition.api.port.MetamodelReadPort;
import com.metaforge.agent.cognition.api.spi.CognitionOperator;
import com.metaforge.agent.cognition.api.spi.CognitionResult;
import com.metaforge.common.dto.PageRequest;
import com.metaforge.common.dto.PageResult;
import com.metaforge.graph.api.dto.RelationInstanceDto;
import com.metaforge.graph.api.dto.RelationQueryRequest;
import com.metaforge.metadata.api.dto.response.MetadataEntityDto;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

public abstract class AbstractCognitionOperator implements CognitionOperator {

    @Autowired
    protected MetamodelReadPort metamodelReadPort;

    @Autowired
    protected MetadataReadPort metadataReadPort;

    @Autowired
    protected GraphReadPort graphReadPort;

    @Autowired
    protected ComputeEngineReadPort computeEngineReadPort;

    public record ScopeFilterResult(
            List<Map<String, Object>> inScopeItems,
            List<String> skippedFqns
    ) {}

    /** 关系查询方向。 */
    public enum RelationDirection {
        OUTBOUND,
        INBOUND
    }

    protected ScopeFilterResult applyScope(List<Map<String, Object>> data, Scope scope) {
        if (data == null || data.isEmpty()) {
            return new ScopeFilterResult(Collections.emptyList(), Collections.emptyList());
        }
        if (scope == null || scope.isEmpty()) {
            return new ScopeFilterResult(data, Collections.emptyList());
        }

        List<Map<String, Object>> inScope = new ArrayList<>();
        List<String> skipped = new ArrayList<>();

        for (Map<String, Object> item : data) {
            if (matchesScope(item, scope)) {
                inScope.add(item);
            } else {
                String fqn = resolveFqn(item);
                if (fqn != null) {
                    skipped.add(fqn);
                }
            }
        }

        return new ScopeFilterResult(inScope, skipped);
    }

    private boolean matchesScope(Map<String, Object> item, Scope scope) {
        if (isNonEmpty(scope.bundles()) && !matchesAny(item, scope.bundles(), "fqn", "bundleFqn", "bundle")) {
            return false;
        }
        if (isNonEmpty(scope.packages()) && !matchesAny(item, scope.packages(), "packageFqn", "package")) {
            return false;
        }
        if (isNonEmpty(scope.domainGroups()) && !matchesAny(item, scope.domainGroups(), "domainGroup")) {
            return false;
        }
        if (isNonEmpty(scope.domains()) && !matchesAny(item, scope.domains(), "domain")) {
            return false;
        }
        if (isNonEmpty(scope.entitySchemas()) && !matchesAny(item, scope.entitySchemas(), "entitySchemaFqn", "schemaFqn")) {
            return false;
        }
        return true;
    }

    private boolean matchesAny(Map<String, Object> item, List<String> allowed, String... keys) {
        for (String key : keys) {
            Object value = item.get(key);
            if (value instanceof String str && allowed.contains(str)) {
                return true;
            }
        }
        return false;
    }

    private static boolean isNonEmpty(List<String> list) {
        return list != null && !list.isEmpty();
    }

    private String resolveFqn(Map<String, Object> item) {
        for (String key : new String[]{"fqn", "bundleFqn", "entityFqn", "packageFqn", "domainFqn"}) {
            Object value = item.get(key);
            if (value instanceof String str && !str.isBlank()) {
                return str;
            }
        }
        return String.valueOf(item.hashCode());
    }

    protected Map<String, Object> buildLazyNode(Object data, boolean hasChildren, String suggestedNextCall) {
        Map<String, Object> node = new LinkedHashMap<>();
        node.put("data", data);
        node.put("has_children", hasChildren);
        if (suggestedNextCall != null && !suggestedNextCall.isBlank()) {
            node.put("suggested_next_call", suggestedNextCall);
        }
        return node;
    }

    /**
     * 从 MetadataEntityDto 的 content 属性中读取字段值（属性值存于 content Map，非 DTO 顶层）。
     */
    protected Object resolveContentValue(MetadataEntityDto dto, String key) {
        if (dto == null || dto.getContent() == null) {
            return null;
        }
        return dto.getContent().get(key);
    }

    protected Map<String, Object> toContentMap(MetadataEntityDto dto) {
        Map<String, Object> map = new LinkedHashMap<>();
        if (dto == null) {
            return map;
        }
        map.put("fqn", dto.getFqn());
        map.put("name", dto.getName());
        map.put("description", dto.getDescription() != null ? dto.getDescription() : "");
        map.put("parentFqn", dto.getParentFqn());
        map.put("entitySchemaFqn", dto.getEntitySchemaFqn());
        if (dto.getContent() != null) {
            map.putAll(dto.getContent());
        }
        return map;
    }

    protected Map<String, Object> toEntityMap(Object dto) {
        if (dto == null) {
            return new LinkedHashMap<>();
        }
        Map<String, Object> map = new LinkedHashMap<>();
        try {
            for (java.lang.reflect.Method m : dto.getClass().getMethods()) {
                if (m.getParameterCount() == 0 && m.getName().startsWith("get")
                        && !m.getName().equals("getClass")) {
                    String key = Character.toLowerCase(m.getName().charAt(3)) + m.getName().substring(4);
                    map.put(key, m.invoke(dto));
                } else if (m.getParameterCount() == 0 && m.getName().startsWith("is")) {
                    String key = Character.toLowerCase(m.getName().charAt(2)) + m.getName().substring(3);
                    map.put(key, m.invoke(dto));
                }
            }
        } catch (Exception e) {
            map.put("fqn", dto.toString());
        }
        return map;
    }

    protected CognitionResult wrapFailure(String error) {
        return CognitionResult.failure(operatorId(), category(), error);
    }

    protected Object executeWithPort(Supplier<?> portCall) {
        try {
            return portCall.get();
        } catch (Exception e) {
            return wrapFailure("PORT_CALL_FAILED: " + e.getMessage());
        }
    }

    // ========================================================================
    // operatorConfig 读取助手（config.operators.{operatorId}）
    // ========================================================================

    /** 读取单字符串配置；缺失/空串返回默认值。 */
    protected String configString(Map<String, Object> config, String key, String defaultValue) {
        if (config == null) {
            return defaultValue;
        }
        Object value = config.get(key);
        if (value instanceof String s && !s.isBlank()) {
            return s;
        }
        return defaultValue;
    }

    /** 读取字符串列表配置；缺失返回 null。 */
    protected List<String> configList(Map<String, Object> config, String key) {
        if (config == null) {
            return null;
        }
        Object value = config.get(key);
        if (value instanceof List<?> list) {
            return list.stream().map(String::valueOf).filter(s -> !s.isBlank()).toList();
        }
        return null;
    }

    /** 读取嵌套 Map 配置；缺失返回 null。 */
    @SuppressWarnings("unchecked")
    protected Map<String, Object> configMap(Map<String, Object> config, String key) {
        if (config == null) {
            return null;
        }
        Object value = config.get(key);
        if (value instanceof Map<?, ?> m) {
            return (Map<String, Object>) m;
        }
        return null;
    }

    /**
     * 解析关系 schema FQN 列表：优先 config.relationSchemaFqns，其次 config.relationSchemaFqn，
     * 最后使用默认列表。
     */
    protected List<String> resolveRelationSchemaFqns(Map<String, Object> config, List<String> defaults) {
        List<String> fqns = configList(config, "relationSchemaFqns");
        if (fqns != null && !fqns.isEmpty()) {
            return fqns;
        }
        String single = configString(config, "relationSchemaFqn", null);
        if (single != null && !single.isBlank()) {
            return List.of(single);
        }
        return defaults != null ? defaults : Collections.emptyList();
    }

    /**
     * 按关系 schema 精确查询某实体的出入边——通过 graphReadPort.multiFilter 构造
     * {@link RelationQueryRequest}（source/targetEntityFqns + relationSchemaFqns/prefix + relationTypes）。
     *
     * <p>相比 {@link GraphReadPort#getOutboundRelations}（仅按 relationType 过滤、忽略第三参），
     * 此方法能精确绑定关系 schema FQN，避免 ASSOCIATION_REFERENCE 类型下多种关系混在一起。
     */
    protected List<RelationInstanceDto> queryRelationsBySchema(
            String entityFqn,
            RelationDirection direction,
            Map<String, Object> config,
            List<String> defaultRelationSchemaFqns,
            String defaultRelationSchemaFqnPrefix) {
        List<String> schemaFqns = resolveRelationSchemaFqns(config, defaultRelationSchemaFqns);
        String prefix = configString(config, "relationSchemaFqnPrefix", defaultRelationSchemaFqnPrefix);
        List<String> relationTypes = configList(config, "relationTypes");

        RelationQueryRequest request = new RelationQueryRequest();
        if (direction == RelationDirection.OUTBOUND) {
            request.setSourceEntityFqns(List.of(entityFqn));
        } else {
            request.setTargetEntityFqns(List.of(entityFqn));
        }
        if (!schemaFqns.isEmpty()) {
            request.setRelationSchemaFqns(schemaFqns);
        }
        if (prefix != null && !prefix.isBlank()) {
            request.setRelationSchemaFqnPrefix(prefix);
        }
        if (relationTypes != null && !relationTypes.isEmpty()) {
            request.setRelationTypes(relationTypes);
        }
        request.setPageRequest(new PageRequest(1, 100));

        PageResult<?> result = graphReadPort.multiFilter(request);
        if (result == null || result.getContent() == null) {
            return new ArrayList<>();
        }
        List<RelationInstanceDto> out = new ArrayList<>();
        for (Object item : result.getContent()) {
            if (item instanceof RelationInstanceDto dto) {
                out.add(dto);
            }
        }
        return out;
    }

    /**
     * 从关系实例中取出「对端」实体 FQN：出边取 targetEntityFqn，入边取 sourceEntityFqn。
     */
    protected String resolvePeerFqn(RelationInstanceDto dto, RelationDirection direction) {
        if (dto == null) {
            return null;
        }
        return direction == RelationDirection.OUTBOUND ? dto.getTargetEntityFqn() : dto.getSourceEntityFqn();
    }

    /**
     * 元数据实体精简摘要——通用要求：必含 fqn/name/description，附 entitySchemaFqn。
     */
    protected Map<String, Object> toEntitySummary(MetadataEntityDto dto) {
        Map<String, Object> map = new LinkedHashMap<>();
        if (dto == null) {
            return map;
        }
        map.put("fqn", dto.getFqn());
        map.put("name", dto.getName());
        map.put("description", dto.getDescription() != null ? dto.getDescription() : "");
        if (dto.getEntitySchemaFqn() != null && !dto.getEntitySchemaFqn().isBlank()) {
            map.put("entitySchemaFqn", dto.getEntitySchemaFqn());
        }
        return map;
    }

    /**
     * 关系实例精简摘要——通用要求：必含 fqn/name/description，附类型与端点信息。
     */
    protected Map<String, Object> toRelationSummary(RelationInstanceDto dto) {
        Map<String, Object> map = new LinkedHashMap<>();
        if (dto == null) {
            return map;
        }
        map.put("fqn", dto.getFqn());
        map.put("name", dto.getName());
        map.put("description", dto.getDescription() != null ? dto.getDescription() : "");
        if (dto.getRelationType() != null) {
            map.put("relationType", dto.getRelationType());
        }
        if (dto.getRelationSchemaFqn() != null) {
            map.put("relationSchemaFqn", dto.getRelationSchemaFqn());
        }
        if (dto.getSourceEntityFqn() != null) {
            map.put("sourceEntityFqn", dto.getSourceEntityFqn());
        }
        if (dto.getTargetEntityFqn() != null) {
            map.put("targetEntityFqn", dto.getTargetEntityFqn());
        }
        return map;
    }
}
