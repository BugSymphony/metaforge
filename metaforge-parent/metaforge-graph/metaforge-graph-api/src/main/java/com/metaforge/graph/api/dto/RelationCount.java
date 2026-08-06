package com.metaforge.graph.api.dto;

/**
 * 实体关系计数 DTO。
 */
public class RelationCount {

    private String entityFqn;
    private long outboundCount;
    private long inboundCount;

    public RelationCount() {}

    public static RelationCount of(String entityFqn, long outbound, long inbound) {
        RelationCount count = new RelationCount();
        count.entityFqn = entityFqn;
        count.outboundCount = outbound;
        count.inboundCount = inbound;
        return count;
    }

    public String getEntityFqn() { return entityFqn; }
    public void setEntityFqn(String entityFqn) { this.entityFqn = entityFqn; }

    public long getOutboundCount() { return outboundCount; }
    public void setOutboundCount(long outboundCount) { this.outboundCount = outboundCount; }

    public long getInboundCount() { return inboundCount; }
    public void setInboundCount(long inboundCount) { this.inboundCount = inboundCount; }
}
