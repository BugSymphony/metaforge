package com.metaforge.agent.cognition.operator.ontological;

import com.metaforge.agent.cognition.api.dto.request.Scope;
import com.metaforge.agent.cognition.api.enums.DimensionCategory;
import com.metaforge.agent.cognition.api.exception.InvalidLevelException;
import com.metaforge.agent.cognition.api.spi.CognitionQueryContext;
import com.metaforge.agent.cognition.api.spi.CognitionResult;
import com.metaforge.agent.cognition.operator.common.AbstractCognitionOperator;
import com.metaforge.agent.cognition.operator.common.MetaforgeLibraryFqns;
import com.metaforge.common.dto.PageRequest;
import com.metaforge.common.dto.PageResult;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
public class OntologicalDomainDrillDownOperator extends AbstractCognitionOperator {

    private static final String CONFIG_KEY_LEVEL_ALIASES = "levelAliases";

    @Override
    public String operatorId() {
        return "ontological.domain-drilldown";
    }

    @Override
    public DimensionCategory category() {
        return DimensionCategory.ONTOLOGICAL;
    }

    @Override
    public CognitionResult execute(CognitionQueryContext context) {
        String level = getLevel(context);
        String entityFqn = context.entityFqn();
        String parentFqn = getParentFqn(context);
        String anchor = parentFqn != null && !parentFqn.isBlank() ? parentFqn : entityFqn;

        String relationType = resolveRelationType(context.operatorConfig());
        String levelFqn = resolveLevelFqn(level, context);

        // 顶层自动发现（level 空 且 无锚点）：level 显示默认 L1（主题域分组）
        String effectiveLevel = level;
        if ((effectiveLevel == null || effectiveLevel.isBlank())
                && (anchor == null || anchor.isBlank())) {
            effectiveLevel = resolveDefaultTopLevel(context);
        }

        List<Map<String, Object>> entities = collectEntities(anchor, levelFqn, relationType, context);

        // 每个实体附加 lazy 导航信息（has_children/suggested_next_call），再按类型分组。
        // 仅输出 children_grouped，避免 children（扁平）与 children_grouped 数据重复。
        List<Map<String, Object>> flatNodes = buildLazyNodes(entities, relationType);
        Map<String, List<Map<String, Object>>> grouped = groupByEntityType(flatNodes);

        List<String> discoveredDomains = entities.stream()
                .map(e -> (String) e.get("fqn"))
                .filter(f -> f != null)
                .toList();

        Map<String, Object> resultData = new LinkedHashMap<>();
        resultData.put("children_grouped", grouped);
        resultData.put("level", effectiveLevel);
        resultData.put("updated_scope", Map.of("domains", discoveredDomains));

        return CognitionResult.success(operatorId(), category(), resultData);
    }

    /**
     * 解析 level 为 EntitySchema FQN：
     * null/空 → null（自动发现，返回所有类型）；
     * 命中模板 config.levelAliases → 别名对应 FQN；
     * 含 ':' 的字符串 → 视为完整 EntitySchema FQN（跨 Bundle 精确过滤）；
     * 其他 → 抛 InvalidLevelException(34013)。
     */
    private String resolveLevelFqn(String level, CognitionQueryContext context) {
        if (level == null || level.isBlank()) {
            return null;
        }
        String trimmed = level.trim();

        Map<String, Object> aliases = resolveAliases(context);
        if (aliases.containsKey(trimmed)) {
            Object fqn = aliases.get(trimmed);
            return fqn != null ? fqn.toString() : null;
        }

        if (trimmed.contains(":")) {
            return trimmed;
        }

        throw new InvalidLevelException(level, context.templateId());
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> resolveAliases(CognitionQueryContext context) {
        Map<String, Object> config = context.templateConfig();
        if (config != null && config.get(CONFIG_KEY_LEVEL_ALIASES) instanceof Map<?, ?> aliases) {
            return (Map<String, Object>) aliases;
        }
        return Collections.emptyMap();
    }

    private List<Map<String, Object>> collectEntities(String anchor, String levelFqn,
                                                      String relationType,
                                                      CognitionQueryContext context) {
        boolean levelExplicit = levelFqn != null && !levelFqn.isBlank();

        if (levelExplicit && levelFqn.equals(getSchemaScopeAnchor(context))) {
            // scope 锚定：返回锚点范围内实体（不过滤）
            if (anchor != null && !anchor.isBlank()) {
                return expandCompositionChildren(anchor, relationType);
            }
            return listByScopePrefix(context);
        }

        if (anchor != null && !anchor.isBlank()) {
            // 下钻：锚点 COMPOSITION 子节点，level 指定时按类型精确过滤
            List<Map<String, Object>> children = expandCompositionChildren(anchor, relationType);
            return levelExplicit ? filterBySchema(children, levelFqn) : children;
        }

        if (levelExplicit) {
            // 顶层 + level 指定：直接按类型查询（避免全量列表分页截断导致类型缺失）
            return listByEntitySchema(levelFqn, context);
        }

        // 顶层自动发现（level 空）：默认 L1 主题域分组（SubjectDomainGroup），而非全部类型
        return listByEntitySchema(resolveDefaultTopLevel(context), context);
    }

    /**
     * 顶层默认层级 FQN——优先模板 config.levelAliases.L1，否则 V4 默认 SubjectDomainGroup。
     */
    private String resolveDefaultTopLevel(CognitionQueryContext context) {
        Map<String, Object> aliases = resolveAliases(context);
        Object l1 = aliases.get("L1");
        if (l1 != null && !l1.toString().isBlank()) {
            return l1.toString();
        }
        return MetaforgeLibraryFqns.Entity.SUBJECT_DOMAIN_GROUP;
    }

    /**
     * 按实体类型（EntitySchema FQN）直接查询实例列表。
     */
    private List<Map<String, Object>> listByEntitySchema(String entitySchemaFqn,
                                                         CognitionQueryContext context) {
        int page = resolvePage(context);
        int size = context.pageSize() > 0 ? context.pageSize() : 20;
        Object portResult = executeWithPort(() ->
                metadataReadPort.listByEntitySchema(entitySchemaFqn, new PageRequest(page, size)));
        if (portResult instanceof CognitionResult) {
            return Collections.emptyList();
        }
        return extractContent(portResult);
    }

    private List<Map<String, Object>> expandCompositionChildren(String parentFqn, String relationType) {
        Object result = executeWithPort(() ->
                graphReadPort.getOutboundRelations(parentFqn, relationType, null));
        if (result instanceof CognitionResult) {
            return Collections.emptyList();
        }
        List<Map<String, Object>> children = new ArrayList<>();
        if (result instanceof List<?> list) {
            for (Object item : list) {
                if (item instanceof com.metaforge.graph.api.dto.RelationInstanceDto edge
                        && edge.getTargetEntityFqn() != null) {
                    children.add(enrichChild(edge.getTargetEntityFqn()));
                }
            }
        }
        return children;
    }

    private Map<String, Object> enrichChild(String fqn) {
        Object meta = executeWithPort(() -> metadataReadPort.getByFqn(fqn));
        if (meta instanceof com.metaforge.metadata.api.dto.response.MetadataEntityDto dto) {
            Map<String, Object> child = toEntitySummary(dto);
            if (dto.getContent() != null) {
                for (String key : new String[]{"entity_type", "entityType"}) {
                    Object value = dto.getContent().get(key);
                    if (value != null) {
                        child.put(key, value);
                    }
                }
            }
            return child;
        }
        Map<String, Object> child = new LinkedHashMap<>();
        child.put("fqn", fqn);
        return child;
    }

    private List<Map<String, Object>> listByScopePrefix(CognitionQueryContext context) {
        List<String> prefixes = getFqnPrefixes(context);
        int page = resolvePage(context);
        int size = context.pageSize() > 0 ? context.pageSize() : 20;
        Object portResult = executeWithPort(() ->
                metadataReadPort.listByFqnPrefixes(prefixes, new PageRequest(page, size)));
        if (portResult instanceof CognitionResult) {
            return Collections.emptyList();
        }
        return extractContent(portResult);
    }

    private List<Map<String, Object>> filterBySchema(List<Map<String, Object>> entities, String schemaFqn) {
        return entities.stream()
                .filter(e -> schemaFqn.equals(e.get("entitySchemaFqn"))
                        || schemaFqn.equals(e.get("entity_type"))
                        || schemaFqn.equals(e.get("entityType")))
                .toList();
    }

    private List<Map<String, Object>> buildLazyNodes(List<Map<String, Object>> entities, String relationType) {
        List<Map<String, Object>> lazyNodes = new ArrayList<>();
        for (Map<String, Object> entity : entities) {
            String fqn = (String) entity.get("fqn");
            boolean hasChildren = checkHasCompositionChildren(fqn, relationType);
            String nextCall = hasChildren ? "ontological.domain-drilldown" : null;
            Map<String, Object> node = new LinkedHashMap<>(entity);
            node.put("has_children", hasChildren);
            if (nextCall != null && !nextCall.isBlank()) {
                node.put("suggested_next_call", nextCall);
            }
            node.put("entity_type", resolveEntityType(entity));
            lazyNodes.add(node);
        }
        return lazyNodes;
    }

    private Map<String, List<Map<String, Object>>> groupByEntityType(List<Map<String, Object>> entities) {
        Map<String, List<Map<String, Object>>> grouped = new LinkedHashMap<>();
        for (Map<String, Object> entity : entities) {
            String type = resolveEntityType(entity);
            grouped.computeIfAbsent(type, k -> new ArrayList<>()).add(entity);
        }
        return grouped;
    }

    private String resolveEntityType(Map<String, Object> entity) {
        for (String key : new String[]{"entitySchemaFqn", "entity_type", "entityType"}) {
            Object value = entity.get(key);
            if (value instanceof String str && !str.isBlank()) {
                return str;
            }
        }
        return "UNKNOWN";
    }

    private boolean checkHasCompositionChildren(String entityFqn, String relationType) {
        if (entityFqn == null) return false;
        Object result = executeWithPort(() ->
                graphReadPort.getOutboundRelations(entityFqn, relationType, null));
        if (result instanceof CognitionResult) return false;
        if (result instanceof List<?> list) return !list.isEmpty();
        return false;
    }

    private String resolveRelationType(Map<String, Object> operatorConfig) {
        List<String> relationTypes = configList(operatorConfig, "relationTypes");
        if (relationTypes != null && !relationTypes.isEmpty()) {
            return relationTypes.get(0);
        }
        String single = configString(operatorConfig, "relationType", null);
        return single != null && !single.isBlank() ? single : "COMPOSITION";
    }

    private String getSchemaScopeAnchor(CognitionQueryContext context) {
        Scope scope = context.scope();
        if (scope != null && scope.entitySchemas() != null && !scope.entitySchemas().isEmpty()) {
            return scope.entitySchemas().get(0);
        }
        return null;
    }

    private String getLevel(CognitionQueryContext context) {
        Map<String, Object> params = context.templateParams();
        if (params != null && params.get("level") != null) {
            return params.get("level").toString();
        }
        return null;
    }

    private String getParentFqn(CognitionQueryContext context) {
        Map<String, Object> params = context.templateParams();
        if (params != null && params.get("parent_fqn") instanceof String s && !s.isBlank()) {
            return s;
        }
        return null;
    }

    private int resolvePage(CognitionQueryContext context) {
        if (context.cursor() != null && context.cursor() > 0) {
            return context.cursor();
        }
        return 1;
    }

    private List<String> getFqnPrefixes(CognitionQueryContext context) {
        List<String> bundleFqns = context.bundleFqns();
        if (bundleFqns != null && !bundleFqns.isEmpty()) {
            return bundleFqns;
        }
        Map<String, Object> params = context.templateParams();
        if (params != null && params.containsKey("fqnPrefixes")) {
            @SuppressWarnings("unchecked")
            List<String> prefixes = (List<String>) params.get("fqnPrefixes");
            if (prefixes != null && !prefixes.isEmpty()) {
                return prefixes;
            }
        }
        // 顶层自动发现兜底：无 scope/prefix 时使用模板默认 Bundle 前缀，
        // 避免空字符串前缀（metadata prefixSpec 会过滤空串）导致空结果
        String bundleVersion = configString(context.templateConfig(), "bundleVersionFqn", null);
        if (bundleVersion != null && !bundleVersion.isBlank()) {
            return List.of(bundleVersion);
        }
        return Collections.emptyList();
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> extractContent(Object portResult) {
        if (portResult instanceof PageResult<?> pr) {
            List<?> content = pr.getContent();
            List<Map<String, Object>> result = new ArrayList<>();
            for (Object item : content) {
                if (item instanceof com.metaforge.metadata.api.dto.response.MetadataEntityDto dto) {
                    result.add(toContentMap(dto));
                } else if (item instanceof Map) {
                    result.add((Map<String, Object>) item);
                }
            }
            return result;
        }
        return Collections.emptyList();
    }
}
