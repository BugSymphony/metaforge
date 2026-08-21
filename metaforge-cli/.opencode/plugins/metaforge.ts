import { type Plugin, tool } from "@opencode-ai/plugin"

/**
 * MetaForge 认知服务消费插件。
 *
 * 提供两个工具，把 opencode Agent 的结构化请求透传给 MetaForge 认知服务端
 * （POST /api/v1/cognition/{templateId}），返回结构化认知简报，供 AI 解读并翻译给普通用户：
 *   - metaforge_cognition：认知查询（6 模板 + 算子 + 锚点）
 *   - metaforge_resolve：FQN 解析（域树枚举 + 确定性匹配，绝不臆造）
 *
 * 服务端 6 个场景模板：
 *   DISCOVER  - 元模型发现（Bundle/Package/EntitySchema/RelationSchema 盘点）
 *   ORIENT    - 业务域定位（域树逐层下钻）
 *   BRIEF     - 实体/任务全景（画像/流程/规则/能力/关系）
 *   GUIDE     - 单步执行指南（执行步骤/决策步骤双态）
 *   FORECAST  - 变更影响链路（影响/风险/冲突/回归）
 *   DELEGATE  - 子任务上下文委派（收窄认知边界，产出 updatedScope）
 */

type CognitionArgs = {
    template: string
    entityFqn?: string
    parentFqn?: string
    level?: string
    selectOperators?: string[]
    changeType?: string
    maxDepth?: number
    scope?: {
        bundles?: string[]
        packages?: string[]
        domainGroups?: string[]
        domains?: string[]
        entitySchemas?: string[]
    }
    depth?: string
    archetype?: string
    maxTokens?: number
}

type CognitionCallResult = {
    code?: number
    message?: string
    traceId?: string
    data?: any
}

async function callCognition(args: CognitionArgs): Promise<CognitionCallResult> {
    const baseUrl = process.env.META_FORGE_SERVER_URL ?? "http://localhost:8080"

    const scope = {
        bundles: args.scope?.bundles ?? ["metaforge:1.0.0"],
        packages: args.scope?.packages ?? [],
        domainGroups: args.scope?.domainGroups ?? [],
        domains: args.scope?.domains ?? [],
        entitySchemas: args.scope?.entitySchemas ?? [],
    }

    const params: Record<string, unknown> = {}
    if (args.entityFqn) params.entity_fqn = args.entityFqn
    if (args.parentFqn) params.parent_fqn = args.parentFqn
    if (args.level) params.level = args.level
    if (args.selectOperators?.length) params.selectOperators = args.selectOperators
    if (args.changeType) params.change_type = args.changeType
    if (args.maxDepth !== undefined) params.max_depth = args.maxDepth

    const body = {
        scope,
        params,
        format: "JSON",
        cognitionDepth: (args.depth ?? "L2").toUpperCase(),
        agentArchetype: (args.archetype ?? "EXECUTION").toUpperCase(),
        maxTokens: args.maxTokens ?? 8000,
    }

    let res: Response
    try {
        res = await fetch(`${baseUrl}/api/v1/cognition/${args.template}`, {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify(body),
        })
    } catch (err) {
        return {
            code: -1,
            message: `无法连接 MetaForge 认知服务端（${baseUrl}）：${err instanceof Error ? err.message : String(err)}`,
        }
    }

    try {
        return (await res.json()) as CognitionCallResult
    } catch {
        return { code: -2, message: `服务端返回非 JSON 响应（HTTP ${res.status}）` }
    }
}

type DomainNode = {
    fqn: string
    name: string
    entitySchemaFqn: string
    hasChildren: boolean
}

/** 从域树顶层递归（BFS）枚举全部可达节点：域组/域/Agent/任务。 */
async function collectDomainTree(): Promise<DomainNode[]> {
    const nodes: DomainNode[] = []
    const visited = new Set<string>()
    const maxDepth = 5

    let frontier: (string | null)[] = [null]
    for (let depth = 0; depth < maxDepth; depth++) {
        const results = await Promise.all(
            frontier.map(async (parentFqn) => {
                const r = await callCognition({
                    template: "ORIENT",
                    parentFqn: parentFqn ?? undefined,
                })
                return { parentFqn, result: r }
            }),
        )

        const next: string[] = []
        for (const { result } of results) {
            if (result.code !== 200 || !result.data) continue
            const dim = result.data.dimensions ?? []
            const childrenGrouped = dim[0]?.data?.children_grouped ?? {}
            for (const list of Object.values(childrenGrouped) as any[]) {
                for (const n of list) {
                    if (!n?.fqn || visited.has(n.fqn)) continue
                    visited.add(n.fqn)
                    nodes.push({
                        fqn: n.fqn,
                        name: n.name ?? n.fqn,
                        entitySchemaFqn: n.entitySchemaFqn ?? "",
                        hasChildren: !!n.has_children,
                    })
                    if (n.has_children) next.push(n.fqn)
                }
            }
        }

        if (next.length === 0) break
        frontier = next
    }

    return nodes
}

const MATCH_METHOD = {
    exactFqn: "精确FQN",
    exactName: "名称精确",
    partial: "名称包含",
    keyword: "关键词",
} as const

function resolveMatch(nodes: DomainNode[], query: string, entitySchema?: string) {
    const q = query.trim()
    const pool = entitySchema
        ? nodes.filter((n) => (n.entitySchemaFqn.split(".").pop() ?? "") === entitySchema)
        : nodes

    if (!q) return { level: -1, method: null, matches: [] as DomainNode[] }

    const shortFqn = (n: DomainNode) => n.fqn.split(".").pop() ?? ""

    const exactFqn = pool.filter((n) => n.fqn === q || shortFqn(n) === q)
    if (exactFqn.length) return { level: 1, method: MATCH_METHOD.exactFqn, matches: exactFqn }

    const exactName = pool.filter((n) => n.name === q)
    if (exactName.length) return { level: 2, method: MATCH_METHOD.exactName, matches: exactName }

    const partial = pool.filter((n) => n.name.includes(q) || q.includes(n.name))
    if (partial.length) return { level: 3, method: MATCH_METHOD.partial, matches: partial }

    const words = q.split(/[\s\-_]+/).filter((w) => w.length >= 2)
    const keyword = pool.filter((n) => words.some((w) => n.name.includes(w)))
    if (keyword.length) return { level: 4, method: MATCH_METHOD.keyword, matches: keyword }

    return { level: -1, method: null, matches: [] as DomainNode[] }
}

export const MetaforgePlugin: Plugin = async () => {
    return {
        tool: {
            metaforge_cognition: tool({
                description:
                    "执行 MetaForge 认知查询，返回可直接解读的结构化认知简报。用于了解任务/实体的全貌、步骤执行指导、变更影响评估、子任务委派边界、领域导航、平台元模型盘点。",
                args: {
                    template: tool.schema
                        .enum(["DISCOVER", "ORIENT", "BRIEF", "GUIDE", "FORECAST", "DELEGATE"])
                        .describe(
                            "认知场景模板。DISCOVER=元模型发现; ORIENT=业务域定位; BRIEF=实体/任务全景; GUIDE=单步执行指南; FORECAST=变更影响链路; DELEGATE=子任务委派",
                        ),
                    entityFqn: tool.schema
                        .string()
                        .optional()
                        .describe("实体锚点 FQN（BRIEF/GUIDE/FORECAST/DELEGATE 必填）"),
                    parentFqn: tool.schema
                        .string()
                        .optional()
                        .describe("父节点 FQN（ORIENT/DISCOVER 下钻锚点）"),
                    level: tool.schema
                        .string()
                        .optional()
                        .describe("层级过滤（ORIENT：L1-L5/Task/Agent/EntitySchemaFQN）"),
                    selectOperators: tool.schema
                        .array(tool.schema.string())
                        .optional()
                        .describe("算子子集（自由视角组合，为空=模板全部算子）"),
                    changeType: tool.schema
                        .enum(["MODIFY", "DELETE", "CREATE"])
                        .optional()
                        .describe("变更类型（FORECAST 的约束冲突判定）"),
                    maxDepth: tool.schema
                        .number()
                        .optional()
                        .describe("追溯深度 1-5（FORECAST）"),
                    scope: tool.schema
                        .object({
                            bundles: tool.schema.array(tool.schema.string()).optional(),
                            packages: tool.schema.array(tool.schema.string()).optional(),
                            domainGroups: tool.schema.array(tool.schema.string()).optional(),
                            domains: tool.schema.array(tool.schema.string()).optional(),
                            entitySchemas: tool.schema.array(tool.schema.string()).optional(),
                        })
                        .optional()
                        .describe("认知边界（默认 bundles=['metaforge:1.0.0']）"),
                    depth: tool.schema
                        .enum(["L1", "L2", "L3"])
                        .optional()
                        .describe("认知深度 L1概览/L2标准/L3全量"),
                    archetype: tool.schema
                        .enum(["execution", "exploration", "audit", "orchestration"])
                        .optional()
                        .describe("Agent 原型"),
                    maxTokens: tool.schema.number().optional().describe("Token 预算上限"),
                },
                async execute(args) {
                    const result = await callCognition(args)

                    if (result.code !== 200) {
                        return {
                            title: `认知查询失败 [${result.code ?? "?"}]`,
                            output: `认知查询失败：${result.message ?? "未知错误"}`,
                            metadata: {
                                error: "cognition-error",
                                code: result.code,
                                traceId: result.traceId,
                            },
                        }
                    }

                    return {
                        title: `认知简报 ${args.template}`,
                        output: JSON.stringify(result.data, null, 2),
                        metadata: { template: args.template, traceId: result.traceId },
                    }
                },
            }),

            metaforge_resolve: tool({
                description:
                    "把自然语言/名称/关键词解析为 MetaForge 平台实体 FQN。基于域树枚举 + 确定性匹配（精确FQN>名称精确>名称包含>关键词），绝不臆造 FQN。短期支持 域组/域/Agent/任务 及任务入口步骤/入口决策步骤。",
                args: {
                    query: tool.schema
                        .string()
                        .describe("要解析的实体名称或关键词，如「库存盘点」「支付」"),
                    entitySchema: tool.schema
                        .enum([
                            "SubjectDomainGroup",
                            "SubjectDomain",
                            "Agent",
                            "Task",
                            "ExecutionStep",
                            "DecisionStep",
                        ])
                        .optional()
                        .describe("目标实体类型过滤"),
                },
                async execute(args) {
                    let nodes: DomainNode[]
                    try {
                        nodes = await collectDomainTree()
                    } catch (err) {
                        return {
                            title: "FQN 解析失败",
                            output: `枚举域树失败：${err instanceof Error ? err.message : String(err)}`,
                            metadata: { error: "enum-fail" },
                        }
                    }

                    const { level, method, matches } = resolveMatch(
                        nodes,
                        args.query,
                        args.entitySchema,
                    )

                    if (matches.length === 0) {
                        const available = nodes.map((n) => `${n.name}（${n.fqn}）`).join("\n")
                        return {
                            title: "FQN 解析：未匹配",
                            output: `未找到与「${args.query}」匹配的实体。\n\n平台现有可选（域树范围内）：\n${available}\n\n请提供更精确的描述，或从上述清单中选择。`,
                            metadata: { error: "no-match", query: args.query },
                        }
                    }

                    if (matches.length === 1) {
                        const m = matches[0]
                        return {
                            title: `FQN 解析：${m.name}`,
                            output: JSON.stringify(
                                {
                                    fqn: m.fqn,
                                    name: m.name,
                                    entitySchemaFqn: m.entitySchemaFqn,
                                    matchMethod: method,
                                },
                                null,
                                2,
                            ),
                            metadata: { fqn: m.fqn, matchMethod: method },
                        }
                    }

                    const candidates = matches.map((m) => ({
                        fqn: m.fqn,
                        name: m.name,
                        entitySchemaFqn: m.entitySchemaFqn,
                    }))
                    return {
                        title: `FQN 解析：${matches.length} 个候选`,
                        output: JSON.stringify(
                            {
                                candidates,
                                matchMethod: method,
                                hint: "存在多个候选，请用户选择其一",
                            },
                            null,
                            2,
                        ),
                        metadata: { candidateCount: matches.length, matchMethod: method },
                    }
                },
            }),
        },
    }
}
