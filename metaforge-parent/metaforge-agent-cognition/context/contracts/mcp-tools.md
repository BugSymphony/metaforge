---
id: mcp-tools
protocol: SDK
version: 1.0.0
owner: metaforge-agent-cognition
description: MCP 工具 cognition_execute 定义，通过模板执行认知查询并返回适合 LLM 消费的结构化上下文。
type: business
---

# MCP Tools Contract

## Important: Delegated Publication

> **This BC does NOT publish MCP tools directly** (per BC Constitution override, § Communication Adapters).
> The MCP tool defined herein is **published by `metaforge-consumer` on this BC's behalf**.
> The consumer proxies the MCP request through the REST API (`POST /api/v1/cognition/{templateId}`).

## Capability Overview

**Tool**: `cognition_execute`

**Description**

> Executes a cognitive query using the specified template, returning structured context suitable for LLM consumption. Supports 14 cognitive perspectives across 5 built-in templates.

## Parameter Description

| Parameter          | Type     | Required | Default        | Description                                              |
|--------------------|----------|----------|----------------|----------------------------------------------------------|
| templateId         | string   | Y        | —              | Template ID (see built-in templates below)                |
| bundle_fqns        | string[] | Y        | —              | Bundle FQN list, e.g. `["order:1.0.0", "refund:1.0.0"]` |
| entity_fqn         | string   | N        | —              | Entity FQN, required for ENTITY_LEVEL scope               |
| cognition_depth    | string   | N        | "L2"            | Cognition depth: `L1` / `L2` / `L3`                     |
| agent_archetype    | string   | N        | "execution"     | Agent archetype: `execution` / `exploration` / `audit` / `orchestration` |
| max_tokens         | number   | N        | 8000           | Max tokens for response                                   |
| format             | string   | N        | "json"          | Output format: `json` / `prompt`                         |

## Return Value Definition

#### JSON Format (`format: "json"`)

```json
{
  "context_meta": {
    "template_id": "cognition-guidance",
    "context_mode": "ENTITY_LEVEL",
    "data_version_anchors": {
      "order": {"version": "1.0.0", "queriedAt": "2026-08-01T10:30:00Z"}
    },
    "truncated_perspectives": [],
    "skipped_perspectives": []
  },
  "perspectives": {
    "entity_profile": {
      "perspective_id": "entity_profile",
      "status": "OK",
      "data": { ... },
      "truncated": false,
      "truncated_reason": null,
      "error_message": null
    }
  }
}
```

#### Prompt Format (`format: "prompt"`)

Returns a Markdown string suitable for direct injection into LLM context windows.

### Built-in Template IDs

| Template ID        | Perspectives Included                                 | Typical Use Case                  |
|--------------------|-------------------------------------------------------|-----------------------------------|
| bundle-catalog     | bundle_profile, version_map, capability_summary       | Listing available Bundles          |
| cognition-guidance | All 14 perspectives                                   | Full cognitive context             |
| task-brief         | entity_profile, constraint_set                        | Task execution briefing            |
| step-guide         | entity_profile, capability_catalog                    | Step-level guidance                |
| navigate           | entity_profile, domain_navigation                     | Domain structure navigation        |

## Exception Handling

On error, the tool returns a structured error object:

```json
{
  "isError": true,
  "code": 34001,
  "message": "Template 'unknown' not registered",
  "data": null
}
```

| Code  | Condition                                   |
|-------|---------------------------------------------|
| 34001 | Template not registered                      |
| 34002 | Bundle FQN format invalid                    |
| 34003 | Bundle FQN list empty                        |
| 34004 | Entity FQN prefix not in any Bundle          |
| 34005 | Single perspective timed out (200ms)         |
| 34006 | Upstream BC unavailable                      |

## Consumption Flow

### Agent-to-Consumer Flow

```
┌─────────────────┐     MCP Request      ┌────────────────────┐     REST POST      ┌──────────────────────┐
│   LLM Agent      │ ──────────────────► │  metaforge-consumer │ ────────────────► │  agent-cognition BC   │
│  (MCP Client)    │                     │   (MCP Publisher)   │                    │  (OHS REST Adapter)   │
└─────────────────┘     MCP Response     └────────────────────┘     REST Response   └──────────────────────┘
       │                                         │                        │
       │  Tool call: cognition_execute            │  Translate to REST     │  Execute query
       │  templateId="step-guide"                 │  POST /api/v1/         │  Orchestrate perspectives
       │  bundle_fqns=["order:1.0.0"]             │    cognition/step-guide│  Return GuidanceResult
       │                                         │                        │
       ▼                                         ▼                        ▼
```

The consumer:
1. Receives the MCP `cognition_execute` invocation from the LLM agent.
2. Translates parameters to the REST request body.
3. Calls `POST /api/v1/cognition/{templateId}` on this BC.
4. Returns the `GuidanceResult` (JSON or prompt-formatted Markdown) to the agent.
