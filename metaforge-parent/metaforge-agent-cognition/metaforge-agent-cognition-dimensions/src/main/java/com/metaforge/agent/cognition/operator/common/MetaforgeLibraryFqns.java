package com.metaforge.agent.cognition.operator.common;

/**
 * V4 内置 metaforge Bundle v1.0.0 的元模型 FQN 常量。
 *
 * <p>唯一权威来源：{@code metaforge-boot/src/main/resources/db/migration/V4__metamodel_governance_init.sql}。
 * 算子默认绑定值均引用此处；模板 {@code config.operators.*} 可覆盖为其他 Bundle 的 FQN。
 */
public final class MetaforgeLibraryFqns {

    private MetaforgeLibraryFqns() {}

    public static final String BUNDLE_VERSION = "metaforge:1.0.0";

    /** 实体类型（entity_schema FQN）。 */
    public static final class Entity {
        private Entity() {}

        // common 包 L1→L5
        public static final String SUBJECT_DOMAIN_GROUP = "metaforge:1.0.0.common.SubjectDomainGroup";
        public static final String SUBJECT_DOMAIN = "metaforge:1.0.0.common.SubjectDomain";
        public static final String BUSINESS_OBJECT = "metaforge:1.0.0.common.BusinessObject";
        public static final String LOGICAL_ENTITY = "metaforge:1.0.0.common.LogicalEntity";
        public static final String ATTRIBUTE = "metaforge:1.0.0.common.Attribute";

        // agent 包
        public static final String AGENT = "metaforge:1.0.0.agent.Agent";
        public static final String AGENT_PROFILE = "metaforge:1.0.0.agent.AgentProfile";
        public static final String AGENT_PERMISSION = "metaforge:1.0.0.agent.AgentPermission";
        public static final String TASK = "metaforge:1.0.0.agent.Task";
        public static final String EXECUTION_STEP = "metaforge:1.0.0.agent.ExecutionStep";
        public static final String CAPABILITY = "metaforge:1.0.0.agent.Capability";
        public static final String EXECUTION_RULE = "metaforge:1.0.0.agent.ExecutionRule";
        public static final String DECISION_STEP = "metaforge:1.0.0.agent.DecisionStep";
        public static final String RISK_PATTERN = "metaforge:1.0.0.agent.RiskPattern";
        public static final String COST_ESTIMATE = "metaforge:1.0.0.agent.CostEstimate";

        // protocol 包
        public static final String HTTP = "metaforge:1.0.0.protocol.Http";
        public static final String MCP_TOOL = "metaforge:1.0.0.protocol.McpTool";
        public static final String CLI = "metaforge:1.0.0.protocol.Cli";
        public static final String LOCAL_METHOD = "metaforge:1.0.0.protocol.LocalMethod";
    }

    /** 关系类型（relation_schema FQN）。 */
    public static final class Relation {
        private Relation() {}

        // common 包 L1→L5 层级
        public static final String SUBJECT_DOMAIN_GROUP_CATEGORIZED_AS = "metaforge:1.0.0.common.SubjectDomainGroupCategorizedAs";
        public static final String SUBJECT_DOMAIN_GROUP_CONTAINS_SUBJECT_DOMAIN = "metaforge:1.0.0.common.SubjectDomainGroupContainsSubjectDomain";
        public static final String SUBJECT_DOMAIN_CONTAINS_BUSINESS_OBJECT = "metaforge:1.0.0.common.SubjectDomainContainsBusinessObject";
        public static final String BUSINESS_OBJECT_REFINES_LOGICAL_ENTITY = "metaforge:1.0.0.common.BusinessObjectRefinesLogicalEntity";
        public static final String LOGICAL_ENTITY_CONTAINS_ATTRIBUTE = "metaforge:1.0.0.common.LogicalEntityContainsAttribute";

        // agent 包：域归属
        public static final String SUBJECT_DOMAIN_COMPOSES_AGENT = "metaforge:1.0.0.agent.SubjectDomainComposesAgent";
        public static final String SUBJECT_DOMAIN_COMPOSES_TASK = "metaforge:1.0.0.agent.SubjectDomainComposesTask";

        // agent 包：能力分配（使用方 → Capability）
        public static final String AGENT_HAS_CAPABILITY = "metaforge:1.0.0.agent.AgentHasCapability";
        public static final String TASK_REQUIRES_CAPABILITY = "metaforge:1.0.0.agent.TaskRequiresCapability";
        public static final String STEP_USES_CAPABILITY = "metaforge:1.0.0.agent.StepUsesCapability";

        // agent 包：任务执行链（起点步骤 + 顺序 + 跨层级 + 决策）
        public static final String TASK_HAS_ENTRY_SUBTASK = "metaforge:1.0.0.agent.TaskHasEntrySubtask";
        public static final String TASK_HAS_ENTRY_STEP = "metaforge:1.0.0.agent.TaskHasEntryStep";
        public static final String TASK_HAS_ENTRY_DECISION_STEP = "metaforge:1.0.0.agent.TaskHasEntryDecisionStep";
        public static final String STEP_HAS_NEXT_STEP = "metaforge:1.0.0.agent.StepHasNextStep";
        public static final String STEP_HAS_NEXT_DECISION_STEP = "metaforge:1.0.0.agent.StepHasNextDecisionStep";
        public static final String STEP_HAS_NEXT_TASK = "metaforge:1.0.0.agent.StepHasNextTask";
        public static final String TASK_HAS_NEXT_STEP = "metaforge:1.0.0.agent.TaskHasNextStep";
        public static final String DECISION_STEP_HAS_NEXT_STEP = "metaforge:1.0.0.agent.DecisionStepHasNextStep";
        public static final String DECISION_STEP_HAS_NEXT_DECISION_STEP = "metaforge:1.0.0.agent.DecisionStepHasNextDecisionStep";
        public static final String DECISION_STEP_HAS_NEXT_TASK = "metaforge:1.0.0.agent.DecisionStepHasNextTask";

        // agent 包：组成与委派
        public static final String AGENT_USES_PROFILE = "metaforge:1.0.0.agent.AgentUsesProfile";
        public static final String AGENT_HAS_PERMISSION = "metaforge:1.0.0.agent.AgentHasPermission";
        public static final String AGENT_EXECUTES_TASK = "metaforge:1.0.0.agent.AgentExecutesTask";
        public static final String AGENT_DELEGATES_TO = "metaforge:1.0.0.agent.AgentDelegatesTo";

        // agent 包：约束/风险
        public static final String RULE_APPLIES_TO = "metaforge:1.0.0.agent.RuleAppliesTo";
        public static final String RULE_APPLIES_TO_TASK = "metaforge:1.0.0.agent.RuleAppliesToTask";
        public static final String RULE_DEPENDS_ON = "metaforge:1.0.0.agent.RuleDependsOn";
        public static final String RISK_AFFECTS = "metaforge:1.0.0.agent.RiskAffects";

        // protocol 包：能力引用协议
        public static final String CAPABILITY_IMPLEMENTS_HTTP = "metaforge:1.0.0.protocol.CapabilityImplementsHttp";
        public static final String CAPABILITY_IMPLEMENTS_MCP_TOOL = "metaforge:1.0.0.protocol.CapabilityImplementsMcpTool";
        public static final String CAPABILITY_IMPLEMENTS_CLI = "metaforge:1.0.0.protocol.CapabilityImplementsCli";
        public static final String CAPABILITY_IMPLEMENTS_LOCAL_METHOD = "metaforge:1.0.0.protocol.CapabilityImplementsLocalMethod";
    }

    /** 协议包关系前缀——protocol-detail 按此前缀查询能力引用的协议实例。 */
    public static final String PROTOCOL_RELATION_PREFIX = "metaforge:1.0.0.protocol.";
}
