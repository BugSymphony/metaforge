package com.metaforge.computeengine.api.service;

import com.metaforge.common.dto.PageResult;
import com.metaforge.computeengine.api.annotation.OpenHostService;
import com.metaforge.computeengine.api.dto.common.EntitySummary;
import com.metaforge.computeengine.api.dto.common.FilterCriteria;
import com.metaforge.computeengine.api.dto.request.AdjacencyQueryRequest;
import com.metaforge.computeengine.api.dto.request.BatchQueryRequest;
import com.metaforge.computeengine.api.dto.request.CompositionTreeQueryRequest;
import com.metaforge.computeengine.api.dto.request.CompoundSearchRequest;
import com.metaforge.computeengine.api.dto.request.PatternMatchRequest;
import com.metaforge.computeengine.api.dto.request.SubgraphQueryRequest;
import com.metaforge.computeengine.api.dto.response.GraphQueryResult;

/**
 * 多维图查询服务。
 * <p>
 * 提供六种核心图查询能力：多度邻接查询、组合层级树查询、子图提取查询、
 * 图模式匹配查询、多条件复合检索、批量语义查询。
 * <p>
 * 所有查询默认基于生效态数据执行（metadata_entity.status='ACTIVE'、relation_instance.status='ACTIVE'），
 * 历史版本与草稿版本不参与计算。遍历深度受两阶段约束：全局 max-depth 与 per-AssociationType maxDepth。
 * 所有结果载体统一包含 truncated（boolean）+ truncatedReason（DEPTH_EXCEEDED/COUNT_EXCEEDED/TIMEOUT）截断标记。
 * <p>
 * 过滤参数 {@link FilterCriteria} 为 7 维可选条件，维度间 AND，维度内 OR。
 * 各 FQN 维度通过 matchMode 显式指定 PREFIX/EXACT 匹配策略，relationInstanceFqns 额外支持 PATTERN 模式。
 * 被过滤的实体/关系不参与遍历且不计入深度。
 */
@OpenHostService
public interface GraphQueryService {

    /**
     * 多度邻接查询。
     * <p>
     * 以指定起点实体为中心，沿关系边多度扩展，返回每度发现的实体与关系。
     * 同一实体在多路径出现时仅返回一次，标注最短到达深度。
     * 遍历深度同时受全局 max-depth 与各 AssociationType per-type maxDepth 约束，取两者较小值。
     *
     * @param request 邻接查询请求（包含起点实体 FQN、遍历方向、最大深度、关注关系类型、过滤条件）
     * @return 图查询结果（实体集合、关系集合、邻接映射、截断标记）
     */
    GraphQueryResult queryAdjacency(AdjacencyQueryRequest request);

    /**
     * 组合层级树查询。
     * <p>
     * 基于 COMPOSITION 关系递归展开指定节点的组合结构。
     * direction 参数指定遍历方向：
     * <ul>
     *   <li>FORWARD：从当前节点向下展开子树，保留树形嵌套结构，每个节点含子节点列表与深度</li>
     *   <li>BACKWARD：从当前节点向上追溯完整父链，返回扁平层级列表</li>
     *   <li>BOTH：双向展开，向上父链 + 向下子树合并输出</li>
     * </ul>
     *
     * @param request 组合层级树查询请求（包含根节点 FQN、遍历方向、最大深度）
     * @return 图查询结果（树形或扁平结构，含截断标记）
     */
    GraphQueryResult queryCompositionTree(CompositionTreeQueryRequest request);

    /**
     * 子图提取查询。
     * <p>
     * 以一个或多个中心实体为种子，在指定深度内扩展，返回子图内的全部实体集合、关系集合及邻接映射。
     *
     * @param request 子图提取查询请求（中心实体 FQN 列表、扩展深度 1~3、过滤条件）
     * @return 图查询结果（实体集合、关系集合、邻接映射、截断标记）
     */
    GraphQueryResult querySubgraph(SubgraphQueryRequest request);

    /**
     * 图模式匹配查询。
     * <p>
     * 在线性路径模式中匹配符合模式的路径实例。模式格式：
     * <pre>EntityTypeA -[RelationType]-&gt; EntityTypeB -[RelationType]-&gt; EntityTypeC ...</pre>
     * 通配符 '*' 匹配任意完整的 EntitySchema FQN（不拆分名称段），'?' 匹配任意完整的 RelationSchema FQN。
     * 模式长度上限 4 段（3 条关系边）。
     * 返回所有匹配路径实例，每条标注实体 FQN、关系 FQN、实体类型、关系类型。
     *
     * @param request 模式匹配请求（模式字符串、匹配上限）
     * @return 图查询结果（匹配路径列表、截断标记）
     */
    GraphQueryResult queryPatternMatch(PatternMatchRequest request);

    /**
     * 多条件复合检索。
     * <p>
     * 按实体类型、属性条件（精准/模糊/范围匹配）、关系条件组合过滤。
     * 支持与/或逻辑组合，支持分页与排序。
     * 此接口为唯一支持分页的查询模式。
     *
     * @param request 复合检索请求（实体类型、属性条件、关系条件、分页参数）
     * @return 分页结果（实体摘要列表）
     */
    PageResult<EntitySummary> searchCompound(CompoundSearchRequest request);

    /**
     * 批量语义查询。
     * <p>
     * 一次传入最多 200 个 FQN，返回每个 FQN 对应的实体摘要及关联关系摘要。
     * 不存在的 FQN 在结果中单独标记状态，不影响其他 FQN 的正常返回。
     *
     * @param request 批量查询请求（FQN 列表，上限 200）
     * @return 图查询结果（实体摘要列表、关系摘要列表、未找到 FQN 列表）
     */
    GraphQueryResult queryBatch(BatchQueryRequest request);
}
