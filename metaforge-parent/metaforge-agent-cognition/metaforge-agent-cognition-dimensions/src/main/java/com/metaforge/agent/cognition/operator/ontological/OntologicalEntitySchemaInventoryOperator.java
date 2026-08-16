package com.metaforge.agent.cognition.operator.ontological;

import com.metaforge.agent.cognition.api.dto.request.Scope;
import com.metaforge.agent.cognition.api.enums.DimensionCategory;
import com.metaforge.agent.cognition.api.spi.CognitionQueryContext;
import com.metaforge.agent.cognition.api.spi.CognitionResult;
import com.metaforge.agent.cognition.operator.common.AbstractCognitionOperator;
import com.metaforge.common.dto.PageRequest;
import com.metaforge.common.dto.PageResult;
import com.metaforge.metamodel.api.dto.ElementQueryRequest;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
public class OntologicalEntitySchemaInventoryOperator extends AbstractCognitionOperator {

	@Override
	public String operatorId() {
		return "ontological.entity-schema-inventory";
	}

	@Override
	public DimensionCategory category() {
		return DimensionCategory.ONTOLOGICAL;
	}

	@Override
	public CognitionResult execute(CognitionQueryContext context) {
		Object query = buildQuery(context);
		Object portResult = executeWithPort(() -> metamodelReadPort.listEntitySchemas(query));
		if (portResult instanceof CognitionResult cr) {
			return cr;
		}

		List<Map<String, Object>> schemas = extractContent(portResult);

		Scope scope = context.scope();
		ScopeFilterResult filtered = applyScope(schemas, scope);

		List<String> discoveredSchemas = new ArrayList<>();
		List<Map<String, Object>> lazyNodes = new ArrayList<>();

		for (Map<String, Object> schema : filtered.inScopeItems()) {
			String schemaFqn = (String) schema.get("fqn");
			if (schemaFqn != null) {
				discoveredSchemas.add(schemaFqn);
			}

			long instanceCount = fetchInstanceCount(schemaFqn);

			@SuppressWarnings("unchecked")
			List<String> keyAttributes = schema.containsKey("key_attributes")
					? (List<String>) schema.get("key_attributes")
					: List.of();

			Object schemaData = Map.of(
					"schema", schema,
					"instance_count", instanceCount,
					"key_attributes", keyAttributes
			);

			boolean hasChildren = instanceCount > 0;
			String nextCall = hasChildren ? "ontological.instance-catalog" : null;
			Map<String, Object> lazyNode = buildLazyNode(schemaData, hasChildren, nextCall);
			lazyNode.put("instance_count", instanceCount);
			lazyNode.put("key_attributes", keyAttributes);
			lazyNodes.add(lazyNode);
		}

		Map<String, Object> resultData = new LinkedHashMap<>();
		resultData.put("entity_schemas", lazyNodes);
		resultData.put("updated_scope", Map.of(
				"entity_schemas", discoveredSchemas,
				"skipped", filtered.skippedFqns()
		));

		return CognitionResult.success(operatorId(), category(), lazyNodes);
	}

	private long fetchInstanceCount(String entitySchemaFqn) {
		if (entitySchemaFqn == null) return 0;
		Object result = executeWithPort(() -> metadataReadPort.listByEntitySchema(entitySchemaFqn, new PageRequest(1, 1)));
		if (result instanceof PageResult<?> pr) {
			return pr.getTotal();
		}
		return 0;
	}

	private ElementQueryRequest buildQuery(CognitionQueryContext context) {
		Map<String, Object> params = context.templateParams();
		if (params != null && params.get("query") instanceof ElementQueryRequest direct) {
			return direct;
		}
		return buildFromAnchors(context);
	}

	private ElementQueryRequest buildFromAnchors(CognitionQueryContext context) {
		Map<String, Object> params = context.templateParams();
		String anchor = null;
		if (params != null) {
			Object bundleFqn = params.get("bundleVersionFqn");
			if (bundleFqn != null) anchor = bundleFqn.toString();
			else {
				Object parentFqn = params.get("parent_fqn");
				if (parentFqn != null) anchor = parentFqn.toString();
			}
		}
		if (anchor == null) {
			List<String> bundleFqns = context.bundleFqns();
			if (bundleFqns != null && !bundleFqns.isEmpty()) {
				anchor = bundleFqns.get(0);
			}
		}

		ElementQueryRequest query = new ElementQueryRequest();
		if (anchor != null && !anchor.isBlank()) {
			query.setFqnPrefixes(List.of(anchor));
		}
		query.setPage(context.cursor() != null && context.cursor() > 0 ? context.cursor() : 1);
		query.setSize(context.pageSize() > 0 ? context.pageSize() : 20);
		return query;
	}

	@SuppressWarnings("unchecked")
	private List<Map<String, Object>> extractContent(Object portResult) {
		if (portResult instanceof PageResult<?> pr) {
			List<Map<String, Object>> result = new ArrayList<>();
			for (Object item : pr.getContent()) {
				if (item instanceof com.metaforge.metamodel.api.dto.response.EntitySchemaDto dto) {
					result.add(toSchemaSummary(dto));
				} else if (item instanceof Map) {
					result.add((Map<String, Object>) item);
				}
			}
			return result;
		}
		return Collections.emptyList();
	}

	/**
	 * EntitySchema 精简摘要：仅 fqn/name/description/enabled。
	 */
	private Map<String, Object> toSchemaSummary(com.metaforge.metamodel.api.dto.response.EntitySchemaDto dto) {
		Map<String, Object> map = new LinkedHashMap<>();
		map.put("fqn", dto.getFqn());
		map.put("name", dto.getName());
		map.put("description", dto.getDescription());
		map.put("enabled", dto.isEnabled());
		return map;
	}
}
