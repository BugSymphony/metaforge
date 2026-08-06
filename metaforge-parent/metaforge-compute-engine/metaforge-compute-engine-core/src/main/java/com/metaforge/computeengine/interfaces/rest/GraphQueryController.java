package com.metaforge.computeengine.interfaces.rest;

import com.metaforge.computeengine.api.dto.common.EntitySummary;
import com.metaforge.computeengine.api.dto.request.AdjacencyQueryRequest;
import com.metaforge.computeengine.api.dto.request.BatchQueryRequest;
import com.metaforge.computeengine.api.dto.request.CompositionTreeQueryRequest;
import com.metaforge.computeengine.api.dto.request.CompoundSearchRequest;
import com.metaforge.computeengine.api.dto.request.PatternMatchRequest;
import com.metaforge.computeengine.api.dto.request.SubgraphQueryRequest;
import com.metaforge.computeengine.api.dto.response.GraphQueryResult;
import com.metaforge.computeengine.api.service.GraphQueryService;
import com.metaforge.common.dto.ApiResponse;
import com.metaforge.common.dto.PageResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 多维图查询 REST Controller。
 *
 * <p>提供六种核心图查询能力：多度邻接查询、组合层级树查询（含上溯父链）、子图提取、
 * 图模式匹配、多条件复合检索、批量语义查询。所有查询结果包含统一截断标记与内联摘要。
 * <p>所有响应由 foundation-core GlobalResponseBodyAdvice 自动包装为 {@link ApiResponse ApiResponse&lt;T&gt;} 格式。
 * 错误码范围 33000-33999。
 * @author metaforge
 */
@RestController
@RequestMapping("/api/v1/compute-engine")
@Tag(name = "compute-engine", description = "语义查询与推理引擎 — 多维图遍历检索、路径推理分析、影响溯源评估")
public class GraphQueryController {

	private final GraphQueryService graphQueryService;

	public GraphQueryController(GraphQueryService graphQueryService) {
		this.graphQueryService = graphQueryService;
	}

	@Operation(
			summary = "多度邻接查询",
			description = """
					以指定起点实体为中心，沿关系边多度扩展，返回每度发现的实体与关系。\n
					同一实体在多路径出现时仅返回一次，标注最短到达深度。\n
					遍历深度同时受全局 max-depth 与各 AssociationType per-type maxDepth 约束，取两者较小值。\n
					过滤参数为 7 维可选条件，维度间 AND，维度内 OR。被过滤内容不参与遍历且不计入深度。"""
	)
	@PostMapping("/adjacency")
	public GraphQueryResult queryAdjacency(
			@Parameter(description = "邻接查询请求（起点 FQN、遍历方向、最大深度、关系类型过滤、7维过滤条件）", required = true)
			@Valid @RequestBody AdjacencyQueryRequest request) {
		return graphQueryService.queryAdjacency(request);
	}

	@Operation(
			summary = "组合层级树查询",
			description = """
					基于 COMPOSITION 关系递归展开指定节点的组合结构。\n
					direction 参数指定遍历方向：
					<ul>
					  <li>FORWARD — 从当前节点向下展开子树，保留树形嵌套结构</li>
					  <li>BACKWARD — 从当前节点向上追溯完整父链，返回扁平层级列表</li>
					  <li>BOTH — 双向展开，向上父链 + 向下子树合并输出</li>
					</ul>
					COMPOSITION 关系可传递，权重连乘表示层级衰减。"""
	)
	@PostMapping("/composition-tree")
	public GraphQueryResult queryCompositionTree(
			@Parameter(description = "组合层级树查询请求（根节点 FQN、遍历方向、最大深度、过滤条件）", required = true)
			@Valid @RequestBody CompositionTreeQueryRequest request) {
		return graphQueryService.queryCompositionTree(request);
	}

	@Operation(
			summary = "子图提取查询",
			description = """
					以一个或多个中心实体为种子，在指定深度内扩展（1~3 度），
					返回子图内的全部实体集合、关系集合及实体-关系邻接映射。\n
					多个种子实体的子图结果自动合并去重。"""
	)
	@PostMapping("/subgraph")
	public GraphQueryResult querySubgraph(
			@Parameter(description = "子图提取请求（中心实体 FQN 列表、扩展深度 1~3、过滤条件）", required = true)
			@Valid @RequestBody SubgraphQueryRequest request) {
		return graphQueryService.querySubgraph(request);
	}

	@Operation(
			summary = "图模式匹配查询",
			description = """
					在线性路径模式中匹配符合模式的路径实例。模式格式：\n
					<pre>EntityTypeA -[RelationType]-> EntityTypeB -[RelationType]-> EntityTypeC ...</pre>
					通配符 '*' 匹配任意完整的 EntitySchema FQN（不拆分名称段），'?' 匹配任意完整的 RelationSchema FQN。\n
					模式长度上限 4 段（3 条关系边）。返回所有匹配路径实例，每条标注实体 FQN、关系 FQN、实体类型、关系类型。"""
	)
	@PostMapping("/pattern-match")
	public GraphQueryResult queryPatternMatch(
			@Parameter(description = "模式匹配请求（模式字符串如 \"* -[?]-> * -[?]-> *\"、最大结果数）", required = true)
			@Valid @RequestBody PatternMatchRequest request) {
		return graphQueryService.queryPatternMatch(request);
	}

	@Operation(
			summary = "多条件复合检索",
			description = """
					按实体类型、属性条件（精准/模糊/范围匹配）、关系条件组合过滤。\n
					支持与/或逻辑组合，支持分页与排序。此接口为唯一支持分页的查询模式。\n
					属性条件支持操作符：EQ（等于）、NEQ（不等于）、LIKE（模糊）、GT（大于）、LT（小于）、GTE（大于等于）、LTE（小于等于）。"""
	)
	@PostMapping("/search")
	public PageResult<EntitySummary> searchCompound(
			@Parameter(description = "复合检索请求（实体类型、属性条件列表、关系类型、分页排序参数）", required = true)
			@Valid @RequestBody CompoundSearchRequest request) {
		return graphQueryService.searchCompound(request);
	}

	@Operation(
			summary = "批量语义查询",
			description = """
					一次传入最多 200 个 FQN，返回每个 FQN 对应的实体摘要及关联关系摘要。\n
					不存在的 FQN 在结果中单独标记为 notFoundFqns，不影响其他 FQN 的正常返回。"""
	)
	@PostMapping("/batch")
	public GraphQueryResult queryBatch(
			@Parameter(description = "批量查询请求（FQN 列表，上限 200）", required = true)
			@Valid @RequestBody BatchQueryRequest request) {
		return graphQueryService.queryBatch(request);
	}
}
