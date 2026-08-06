package com.metaforge.metamodel.interfaces.mcp;

import java.util.ArrayList;
import java.util.List;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import com.metaforge.metamodel.api.dto.response.AttributeTemplateDto;
import com.metaforge.metamodel.api.dto.response.BundleVersionDto;
import com.metaforge.metamodel.api.dto.response.EntitySchemaDto;
import com.metaforge.metamodel.api.dto.response.ExportManifestDto;
import com.metaforge.metamodel.api.dto.response.RelationSchemaDto;
import com.metaforge.metamodel.api.service.BundleManagementService;
import com.metaforge.metamodel.api.service.BundleVersionManagementService;
import com.metaforge.metamodel.api.service.ElementDefinitionService;
import com.metaforge.metamodel.api.service.ExportManifestService;
import com.metaforge.metamodel.domain.model.valueobject.ValidationResult;
import com.metaforge.metamodel.domain.service.FqnGenerator;
import com.metaforge.metamodel.domain.service.ValidationService;

/**
 * 元模型治理 BC 的 MCP 工具集。
 *
 * <p>通过 Spring AI {@code @Tool} 注解将领域能力发布为 MCP Server 工具方法，
 * 供 {@code agent-consumption} BC 经 {@code spring-ai-starter-mcp-server-webmvc}
 * 自动注册到 MCP Server 并暴露给 Agent 消费端调用。
 *
 * <p><strong>工具集名称</strong>：{@code metamodel-query}<br>
 * <strong>适用场景</strong>：Agent 获取元模型元素定义、JSON Schema、版本信息、导出清单、预发布校验。
 *
 * <p><strong>数据来源</strong>：所有查询仅读取 {@code metamodel_governance} Schema，
 * 不涉及跨 BC 数据访问。FQN 均支持版本省略规则自动解析。
 */
@Component
public class MetamodelMcpTools {

    private final ElementDefinitionService elementService;
    private final BundleManagementService bundleService;
    private final BundleVersionManagementService versionService;
    private final ExportManifestService manifestService;
    private final FqnGenerator fqnGenerator;
    private final ValidationService validationService;

    public MetamodelMcpTools(ElementDefinitionService elementService,
                              BundleManagementService bundleService,
                              BundleVersionManagementService versionService,
                              ExportManifestService manifestService,
                              FqnGenerator fqnGenerator,
                              ValidationService validationService) {
        this.elementService = elementService;
        this.bundleService = bundleService;
        this.versionService = versionService;
        this.manifestService = manifestService;
        this.fqnGenerator = fqnGenerator;
        this.validationService = validationService;
    }

    // ========================================================================
    // 工具 1: getElementSchema — 查询 EntitySchema 完整定义
    // ========================================================================

    /**
     * 按 FQN 查询 EntitySchema 的完整定义，包含平铺合并后的 JSON Schema。
     *
     * <p><strong>输入</strong> — FQN 支持以下三种格式：
     * <ol>
     *   <li>纯净 FQN：{@code order:1.0.0.pkg_order.Order}</li>
     *   <li>带类型前缀：{@code entity:order:1.0.0.pkg_order.Order}</li>
     *   <li>省略版本：{@code order.pkg_order.Order}（自动解析为最新已发布版本）</li>
     * </ol>
     *
     * <p><strong>输出</strong> — {@link EntitySchemaDto} 包含以下字段：
     * <table>
     *   <tr><td>fqn</td><td>纯净 FQN</td></tr>
     *   <tr><td>name</td><td>人类可读名称</td></tr>
     *   <tr><td>description</td><td>语义描述（业务含义 + 适用场景）</td></tr>
     *   <tr><td>packageFqn</td><td>所属 Package FQN</td></tr>
     *   <tr><td>nativeAttributes</td><td>原生属性定义 JSON 数组</td></tr>
     *   <tr><td>mountedTemplateFqns</td><td>挂载属性模板 FQN 列表 JSON</td></tr>
     *   <tr><td>jsonSchema</td><td>发布时生成的扁平 JSON Schema（Draft 2020-12）</td></tr>
     *   <tr><td>enabled</td><td>是否可消费（DRAFT→false, PUBLISHED→true）</td></tr>
     * </table>
     *
     * @param fqn 元素的 FQN（纯净格式/带类型前缀/省略版本）
     * @return EntitySchemaDto 完整定义，不存在时返回 null
     */
    @Tool(description = "按 FQN 查询 EntitySchema 的完整定义，返回包含平铺 JSON Schema 的元模型元素详情")
    public EntitySchemaDto getElementSchema(
            @ToolParam(description = "元素的 FQN，支持纯净格式(order:1.0.0.pkg_order.Order)、带 entity: 前缀、或省略版本(order.pkg_order.Order)") String fqn) {
        String pureFqn = fqnGenerator.stripTypePrefix(fqn);
        return elementService.findEntitySchemaByFqn(pureFqn).orElse(null);
    }

    // ========================================================================
    // 工具 2: queryElements — FQN 前缀集合批量查询
    // ========================================================================

    /**
     * 按 FQN 前缀集合批量查询 EntitySchema，多个前缀按 OR 逻辑拼接。
     *
     * <p><strong>输入</strong> — FQN 前缀列表，用于按 Bundle/版本/Package 维度过滤：
     * <ul>
     *   <li>{@code ["order:1.0.0.pkg_order."]} — 精确到 Package</li>
     *   <li>{@code ["order:", "metaforge:"]} — 跨 Bundle 查询</li>
     *   <li>空列表 — 返回全部已发布版本的 EntitySchema（上限 200 条）</li>
     * </ul>
     *
     * <p><strong>输出</strong> — {@code List<EntitySchemaDto>} 匹配的元素列表。
     *
     * @param fqnPrefixes FQN 前缀列表，OR 逻辑，空列表返回全部
     * @return 匹配的 EntitySchema 列表，上限 200 条
     */
    @Tool(description = "按 FQN 前缀集合批量查询 EntitySchema，多个前缀用 OR 拼接，支持空列表返回全部元素")
    public List<EntitySchemaDto> queryElements(
            @ToolParam(description = "FQN 前缀列表，如 [\"order:1.0.0.pkg_order.\", \"metaforge:1.0.0.common.\"]，空列表返回全部(上限200)") List<String> fqnPrefixes) {
        if (fqnPrefixes == null || fqnPrefixes.isEmpty()) {
            List<EntitySchemaDto> result = new ArrayList<>();
            var bundles = bundleService.listAll();
            for (var bundle : bundles) {
                var versions = versionService.listByBundle(bundle.getFqn());
                for (var v : versions) {
                    elementService.findEntitySchemaByFqn(v.getFqn()).ifPresent(result::add);
                }
                if (result.size() >= 200) break;
            }
            return result;
        }
        return fqnPrefixes.stream()
                .flatMap(prefix -> {
                    String purePrefix = fqnGenerator.stripTypePrefix(prefix);
                    List<EntitySchemaDto> results = new ArrayList<>();
                    elementService.findEntitySchemaByFqn(purePrefix).ifPresent(results::add);
                    return results.stream();
                })
                .distinct()
                .limit(200)
                .toList();
    }

    // ========================================================================
    // 工具 3: getRelationSchema — 查询 RelationSchema
    // ========================================================================

    /**
     * 按 FQN 查询 RelationSchema 的详细定义，返回关联类型与基数约束。
     *
     * <p><strong>输入</strong> — FQN（如 {@code order:1.0.0.pkg_order.Order_contains_Item}）。
     *
     * <p><strong>输出</strong> — {@link RelationSchemaDto} 包含：
     * <table>
     *   <tr><td>fqn</td><td>纯净 FQN</td></tr>
     *   <tr><td>sourceFqn</td><td>源端 EntitySchema FQN</td></tr>
     *   <tr><td>targetFqn</td><td>目标端 EntitySchema FQN</td></tr>
     *   <tr><td>associationType</td><td>关联类型（组成/关联引用/映射对应/依赖影响/流程时序）</td></tr>
     *   <tr><td>cardinalitySource</td><td>源端基数（1/0..1/0..N/1..N）</td></tr>
     *   <tr><td>cardinalityTarget</td><td>目标端基数</td></tr>
     *   <tr><td>jsonSchema</td><td>发布时生成的扁平 JSON Schema</td></tr>
     * </table>
     *
     * @param fqn RelationSchema 的 FQN
     * @return RelationSchemaDto，不存在时返回 null
     */
    @Tool(description = "按 FQN 查询 RelationSchema，返回关联类型、基数约束及 JSON Schema")
    public RelationSchemaDto getRelationSchema(
            @ToolParam(description = "RelationSchema 的 FQN，如 order:1.0.0.pkg_order.Order_contains_Item") String fqn) {
        return elementService.findRelationSchemaByFqn(fqn).orElse(null);
    }

    // ========================================================================
    // 工具 4: listBundleVersions — 列出 Bundle 所有版本
    // ========================================================================

    /**
     * 列出指定 Bundle 的所有版本（按创建时间倒序排列）。
     *
     * <p><strong>输入</strong> — Bundle FQN（如 {@code order} 或 {@code metaforge}）。
     *
     * <p><strong>输出</strong> — {@code List<BundleVersionDto>}，每个元素包含：
     * <ul>
     *   <li>fqn — 版本 FQN（如 order:1.0.0）</li>
     *   <li>status — DRAFT 或 PUBLISHED</li>
     *   <li>upgradeLevel — MAJOR/MINOR/PATCH</li>
     *   <li>enabled — DRAFT→false, PUBLISHED→true</li>
     *   <li>sourceVersionFqn — 源版本 FQN</li>
     * </ul>
     *
     * @param bundleFqn Bundle FQN
     * @return 版本列表（倒序）
     */
    @Tool(description = "列出指定 Bundle 的所有版本，按创建时间倒序，返回状态与升级等级信息")
    public List<BundleVersionDto> listBundleVersions(
            @ToolParam(description = "Bundle FQN，如 order 或 metaforge") String bundleFqn) {
        return versionService.listByBundle(bundleFqn);
    }

    // ========================================================================
    // 工具 5: getExportManifest — 查询导出清单
    // ========================================================================

    /**
     * 查询指定 BundleVersion 的导出清单，返回可跨 Bundle 引用的 Package 白名单。
     *
     * <p><strong>输入</strong> — BundleVersion FQN（如 {@code order:1.0.0}）。
     *
     * <p><strong>输出</strong> — {@link ExportManifestDto}：
     * <ul>
     *   <li>bundleVersionFqn — 所属版本 FQN</li>
     *   <li>exportedPackageFqns — 导出 Package FQN 白名单列表</li>
     * </ul>
     *
     * @param versionFqn BundleVersion FQN
     * @return ExportManifestDto，未配置时返回 null
     */
    @Tool(description = "查询 BundleVersion 的导出清单，返回对外可见的 Package 命名空间白名单")
    public ExportManifestDto getExportManifest(
            @ToolParam(description = "BundleVersion FQN，如 order:1.0.0") String versionFqn) {
        return manifestService.findByVersionFqn(versionFqn).orElse(null);
    }

    // ========================================================================
    // 工具 6: resolveFqn — FQN 解析
    // ========================================================================

    /**
     * 解析 FQN 为结构化部件，自动处理类型前缀剥离与版本省略规则。
     *
     * <p><strong>输入</strong> — FQN 字符串，支持以下格式：
     * <ul>
     *   <li>完整版本：{@code order:1.0.0.pkg_order.Order}</li>
     *   <li>省略版本：{@code order.pkg_order.Order}（自动解析为最新已发布版本）</li>
     *   <li>带类型前缀：{@code entity:order:1.0.0.pkg_order.Order}</li>
     * </ul>
     *
     * <p><strong>输出</strong> — JSON 格式字符串：
     * <pre>{@code {"bundleCode":"order","version":"1.0.0","shortName":"Order","parentFqn":"order:1.0.0.pkg_order"}}</pre>
     *
     * @param fqn 待解析的 FQN
     * @return JSON 格式的结构化部件信息
     */
    @Tool(description = "解析 FQN 为结构化部件(bundleCode/version/shortName/parentFqn)，支持版本省略规则和类型前缀")
    public String resolveFqn(
            @ToolParam(description = "待解析的 FQN，如 order.pkg_order.Order（省略版本）或 order:1.0.0.pkg_order.Order（完整版本）") String fqn) {
        String pureFqn = fqnGenerator.stripTypePrefix(fqn);
        var parts = fqnGenerator.parse(pureFqn);
        return String.format(
                "{\"bundleCode\":\"%s\", \"version\":\"%s\", \"shortName\":\"%s\", \"parentFqn\":\"%s\"}",
                parts.bundleCode(),
                parts.version() != null ? parts.version() : "latest",
                parts.shortName(),
                parts.parentFqn());
    }

    // ========================================================================
    // 工具 7: getAttributeTemplate — 查询属性模板
    // ========================================================================

    /**
     * 查询指定 FQN 的 AttributeTemplate，返回属性定义集合。
     *
     * <p><strong>输入</strong> — AttributeTemplate FQN（如 {@code order:1.0.0.AuditFields}）。
     *
     * <p><strong>输出</strong> — {@link AttributeTemplateDto}：
     * <ul>
     *   <li>fqn — FQN</li>
     *   <li>name — 模板名称</li>
     *   <li>attributeDefinitions — 属性定义 JSON 数组（JSON Schema Draft 2020-12 子集）</li>
     *   <li>enabled — 是否可消费</li>
     * </ul>
     *
     * @param fqn AttributeTemplate 的 FQN
     * @return AttributeTemplateDto，不存在时返回 null
     */
    @Tool(description = "查询 AttributeTemplate 的属性定义集合，返回 JSON Schema Draft 2020-12 格式的属性模板内容")
    public AttributeTemplateDto getAttributeTemplate(
            @ToolParam(description = "AttributeTemplate 的 FQN，如 order:1.0.0.AuditFields") String fqn) {
        return elementService.findAttributeTemplateByFqn(fqn).orElse(null);
    }

    // ========================================================================
    // 工具 8: validateVersion — 预发布校验
    // ========================================================================

    /**
     * 对指定 BundleVersion 执行发布前全量校验，返回带元素级定位的校验报告。
     *
     * <p>校验内容对照 FR-050（发布前全量校验）：
     * <ol>
     *   <li><strong>依赖链自洽性</strong> — 循环依赖检测（Kahn + DFS）、缺失依赖检查</li>
     *   <li><strong>关联端点合法性</strong> — RelationSchema 的 sourceFqn / targetFqn 存在且可见</li>
     *   <li><strong>FQN 全局唯一性</strong> — 当前版本内无重复 FQN</li>
     *   <li><strong>Package 嵌套深度</strong> — 不超过 5 层</li>
     *   <li><strong>属性名冲突</strong> — 挂载模板组无重复</li>
     * </ol>
     *
     * <p><strong>输入</strong> — 待校验的 BundleVersion FQN（如 {@code order:1.0.0}）。
     *
     * <p><strong>输出</strong> — {@link ValidationResult}：
     * <ul>
     *   <li>passed — boolean，校验是否全部通过</li>
     *   <li>errors — 错误列表，每条精确定位到：
     *     <ul>
     *       <li>elementFqn — 出错的元素 FQN</li>
     *       <li>fieldName — 出错的字段名</li>
     *       <li>message — 错误原因描述</li>
     *     </ul>
     *   </li>
     * </ul>
     *
     * @param versionFqn 待校验的 BundleVersion FQN
     * @return ValidationResult 校验报告（passed + 精确定位的 errors）
     */
    @Tool(description = "对 BundleVersion 执行发布前全量校验(FR-050)，返回带元素级定位的校验报告")
    public ValidationResult validateVersion(
            @ToolParam(description = "待校验的 BundleVersion FQN，如 order:1.0.0") String versionFqn) {
        return validationService.validatePublish(versionFqn);
    }
}
