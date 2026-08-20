/**
 * MetaForge DSH 插件 —— 为 DeepSeek Harness Agent 提供业务语义认知。
 *
 * 通过两个工具消费 MetaForge 认知服务（REST）：
 *   - metaforge_cognition：按认知模板查询语义说明书（流程/规则/能力/决策/影响）
 *   - metaforge_resolve：自然语言 → 精确实体 FQN（域树枚举 + 确定性匹配，绝不臆造）
 *
 * 数据流：MetaForge 存"语义说明书"（元数据），Agent 查询后对照业务数据执行。
 */

import type { Context } from '@deepseek-ai/cordis'
import z from '@deepseek-ai/schemastery'
import { defineTool } from '@deepseek-ai/dsh-tools'

/** Cordis 插件名（loader 诊断用）。 */
export const name = 'metaforge'

/** 依赖 service：tools（工具注册）。 */
export const inject = ['tools']

/** 插件配置：MetaForge 服务地址、超时、默认 scope。 */
export interface Config {
  /** MetaForge 认知服务地址，默认 http://localhost:8080 */
  serverUrl?: string
  /** 请求超时（毫秒），默认 15000 */
  timeoutMs?: number
  /** 默认认知边界（bundles），默认 ["metaforge:1.0.0"] */
  defaultBundles?: string[]
  /** 默认认知深度 L1/L2/L3 */
  defaultDepth?: string
  /** 默认 Agent 原型 execution/exploration/audit/orchestration */
  defaultArchetype?: string
}

export const Config: z<Config> = z.object({
  serverUrl: z.string().default('http://localhost:8080'),
  timeoutMs: z.number().default(15000),
  defaultBundles: z.array(z.string()).default(['metaforge:1.0.0']),
  defaultDepth: z.string().default('L3'),
  defaultArchetype: z.string().default('EXECUTION'),
})

interface CognitionArgs {
  template: string
  entity_fqn?: string
  parent_fqn?: string
  level?: string
  select_operators?: string[]
  change_type?: string
  max_depth?: number
  scope?: Record<string, unknown>
  depth?: string
  archetype?: string
  max_tokens?: number
}

interface CognitionResult {
  code?: number
  message?: string
  traceId?: string
  data?: unknown
}

async function callCognition(config: Config, args: CognitionArgs): Promise<CognitionResult> {
  const scope = {
    bundles: (args.scope?.bundles as string[]) ?? config.defaultBundles,
    packages: (args.scope?.packages as string[]) ?? [],
    domainGroups: (args.scope?.domainGroups as string[]) ?? [],
    domains: (args.scope?.domains as string[]) ?? [],
    entitySchemas: (args.scope?.entitySchemas as string[]) ?? [],
  }
  const params: Record<string, unknown> = {}
  if (args.entity_fqn) params.entity_fqn = args.entity_fqn
  if (args.parent_fqn) params.parent_fqn = args.parent_fqn
  if (args.level) params.level = args.level
  if (args.select_operators?.length) params.selectOperators = args.select_operators
  if (args.change_type) params.change_type = args.change_type
  if (args.max_depth !== undefined) params.max_depth = args.max_depth

  const body = {
    scope,
    params,
    format: 'JSON',
    cognitionDepth: (args.depth ?? config.defaultDepth ?? 'L3').toUpperCase(),
    agentArchetype: (args.archetype ?? config.defaultArchetype ?? 'EXECUTION').toUpperCase(),
    maxTokens: args.max_tokens ?? 8000,
  }

  const controller = new AbortController()
  const timer = setTimeout(() => controller.abort(), config.timeoutMs)
  try {
    const res = await fetch(`${config.serverUrl}/api/v1/cognition/${args.template}`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(body),
      signal: controller.signal,
    })
    return (await res.json()) as CognitionResult
  } finally {
    clearTimeout(timer)
  }
}

/** 工具结果渲染为模型可读文本。 */
function renderCognition(value: CognitionResult, template: string): string {
  if (value.code !== 200) {
    return `认知查询失败 [${value.code ?? '?'}]: ${value.message ?? '未知错误'}`
  }
  return JSON.stringify(value.data, null, 2)
}

interface DomainNode {
  fqn: string
  name: string
  entitySchemaFqn: string
  hasChildren: boolean
}

/** 从域树顶层递归（BFS）枚举全部可达节点：域组/域/Agent/任务/入口步骤。 */
async function collectDomainTree(config: Config): Promise<DomainNode[]> {
  const nodes: DomainNode[] = []
  const visited = new Set<string>()
  let frontier: (string | null)[] = [null]
  for (let depth = 0; depth < 5; depth++) {
    const results = await Promise.all(
      frontier.map(async (parentFqn) => {
        const r = await callCognition(config, {
          template: 'ORIENT',
          parent_fqn: parentFqn ?? undefined,
        })
        return r
      }),
    )
    const next: string[] = []
    for (const result of results) {
      if (result.code !== 200 || !result.data) continue
      const dims = (result.data as { dimensions?: { data?: { children_grouped?: Record<string, unknown> } }[] })
        .dimensions ?? []
      const childrenGrouped = dims[0]?.data?.children_grouped ?? {}
      for (const list of Object.values(childrenGrouped) as unknown[][]) {
        for (const raw of list) {
          const n = raw as { fqn?: string; name?: string; entitySchemaFqn?: string; has_children?: boolean }
          if (!n.fqn || visited.has(n.fqn)) continue
          visited.add(n.fqn)
          nodes.push({
            fqn: n.fqn,
            name: n.name ?? n.fqn,
            entitySchemaFqn: n.entitySchemaFqn ?? '',
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

function resolveMatch(nodes: DomainNode[], query: string, entitySchema?: string) {
  const q = query.trim()
  const pool = entitySchema
    ? nodes.filter((n) => (n.entitySchemaFqn.split('.').pop() ?? '') === entitySchema)
    : nodes
  if (!q) return [] as DomainNode[]
  const short = (n: DomainNode) => n.fqn.split('.').pop() ?? ''
  const exact = pool.filter((n) => n.fqn === q || short(n) === q)
  if (exact.length) return exact
  const nameExact = pool.filter((n) => n.name === q)
  if (nameExact.length) return nameExact
  const partial = pool.filter((n) => n.name.includes(q) || q.includes(n.name))
  if (partial.length) return partial
  const words = q.split(/[\s\-_]+/).filter((w) => w.length >= 2)
  return pool.filter((n) => words.some((w) => n.name.includes(w)))
}

export function apply(ctx: Context, config: Config): void {
  ctx.tools.register(defineTool({
    name: 'metaforge_cognition',
    description:
      '执行 MetaForge 认知查询，返回语义说明书（结构化认知简报）。用于了解任务/实体的全貌、单步执行指导、变更影响评估、子任务委派边界、领域定位、平台元模型盘点。模板: DISCOVER/ORIENT/BRIEF/GUIDE/FORECAST/DELEGATE。',
    parameters: {
      template: { type: 'string', required: true, description: '认知模板: DISCOVER/ORIENT/BRIEF/GUIDE/FORECAST/DELEGATE' },
      entity_fqn: { type: 'string', description: '实体锚点 FQN（BRIEF/GUIDE/FORECAST/DELEGATE 需要）' },
      parent_fqn: { type: 'string', description: '父节点 FQN（ORIENT/DISCOVER 下钻锚点）' },
      level: { type: 'string', description: '层级过滤（ORIENT: L1-L5/Task/Agent/EntitySchemaFQN）' },
      select_operators: { type: 'array', items: { type: 'string' }, description: '算子子集（自由组合，空=模板全部算子）' },
      change_type: { type: 'string', description: '变更类型 MODIFY/DELETE/CREATE（FORECAST）' },
      max_depth: { type: 'number', description: '追溯深度 1-5（FORECAST）' },
      depth: { type: 'string', description: '认知深度 L1/L2/L3' },
      archetype: { type: 'string', description: 'Agent 原型 execution/exploration/audit/orchestration' },
      max_tokens: { type: 'number', description: 'Token 预算上限' },
    },
    output: {
      schema: {
        type: 'object',
        additionalProperties: true,
        properties: {
          code: { type: 'number' },
          message: { type: 'string' },
          traceId: { type: 'string' },
          data: { type: 'object', additionalProperties: true },
        },
      },
      render: (_args, value) => [{
        type: 'text',
        text: renderCognition(value as unknown as CognitionResult, (_args as { template?: string }).template ?? ''),
      }],
    },
    async execute(args: CognitionArgs, exec) {
      const controller = new AbortController()
      const onAbort = () => controller.abort()
      exec.signal?.addEventListener?.('abort', onAbort)
      try {
        const result = await callCognition(config, {
          template: args.template,
          entity_fqn: args.entity_fqn,
          parent_fqn: args.parent_fqn,
          level: args.level,
          select_operators: args.select_operators,
          change_type: args.change_type,
          max_depth: args.max_depth,
          depth: args.depth,
          archetype: args.archetype,
          max_tokens: args.max_tokens,
        })
        return result as never
      } finally {
        exec.signal?.removeEventListener?.('abort', onAbort)
      }
    },
  }))

  ctx.tools.register(defineTool({
    name: 'metaforge_resolve',
    description:
      '把自然语言/名称/关键词解析为 MetaForge 平台实体 FQN。基于域树枚举 + 确定性匹配（精确FQN>名称精确>名称包含>关键词），绝不臆造 FQN。支持域组/域/Agent/任务及任务入口步骤。',
    parameters: {
      query: { type: 'string', required: true, description: '要解析的实体名称或关键词，如「库存盘点」「支付」' },
      entity_schema: { type: 'string', description: '目标实体类型过滤: SubjectDomainGroup/SubjectDomain/Agent/Task/ExecutionStep/DecisionStep' },
    },
    output: {
      schema: {
        type: 'object',
        additionalProperties: true,
        properties: {
          fqn: { type: 'string' },
          name: { type: 'string' },
          entitySchemaFqn: { type: 'string' },
          candidates: { type: 'array', items: { type: 'object', additionalProperties: true } },
          error: { type: 'string' },
          message: { type: 'string' },
        },
      },
      render: (_args, value) => [{
        type: 'text',
        text: JSON.stringify(value, null, 2),
      }],
    },
    async execute(args: { query: string; entity_schema?: string }, exec) {
      const onAbort = () => { /* resolve 使用独立 fetch 超时 */ }
      exec.signal?.addEventListener?.('abort', onAbort)
      try {
        const nodes = await collectDomainTree(config)
        const matches = resolveMatch(nodes, args.query, args.entity_schema)
        const result: Record<string, unknown> = {}
        if (matches.length === 0) {
          result.error = 'no-match'
          result.message = `未找到与「${args.query}」匹配的实体。可用候选：${nodes.map((n) => n.name).join('、')}`
          result.available = nodes.map((n) => ({ fqn: n.fqn, name: n.name, entitySchemaFqn: n.entitySchemaFqn }))
        } else if (matches.length === 1) {
          result.fqn = matches[0].fqn
          result.name = matches[0].name
          result.entitySchemaFqn = matches[0].entitySchemaFqn
        } else {
          result.candidates = matches.map((m) => ({ fqn: m.fqn, name: m.name, entitySchemaFqn: m.entitySchemaFqn }))
          result.hint = '存在多个候选，请用户选择其一'
        }
        return result as never
      } finally {
        exec.signal?.removeEventListener?.('abort', onAbort)
      }
    },
  }))
}
