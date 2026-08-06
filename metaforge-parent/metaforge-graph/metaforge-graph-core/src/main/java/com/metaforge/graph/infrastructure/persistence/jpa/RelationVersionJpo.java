package com.metaforge.graph.infrastructure.persistence.jpa;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import java.time.LocalDateTime;

/**
 * RelationVersion JPA 持久化实体（JPO）。
 * 映射 semantic_relation_network.relation_version 历史表。
 * 仅允许 INSERT，禁止 UPDATE/DELETE。
 */
@Entity
@Table(name = "relation_version", schema = "semantic_relation_network")
public class RelationVersionJpo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "fqn", nullable = false, length = 1536)
    private String fqn;

    @Column(name = "name", nullable = false, length = 512)
    private String name;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "source_entity_fqn", nullable = false, length = 512)
    private String sourceEntityFqn;

    @Column(name = "target_entity_fqn", nullable = false, length = 512)
    private String targetEntityFqn;

    @Column(name = "relation_type", nullable = false, length = 64)
    private String relationType;

    @Column(name = "relation_schema_fqn", nullable = false, length = 256)
    private String relationSchemaFqn;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "content", nullable = false, columnDefinition = "jsonb")
    private String content;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "embedding", columnDefinition = "jsonb")
    private String embedding;

    @Column(name = "version", nullable = false)
    private Integer version;

    @Column(name = "activated_by", length = 128)
    private String activatedBy;

    @Column(name = "activated_time", nullable = false)
    private LocalDateTime activatedTime;

    @PrePersist
    protected void onCreate() {
        if (activatedTime == null) {
            activatedTime = LocalDateTime.now();
        }
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getFqn() { return fqn; }
    public void setFqn(String fqn) { this.fqn = fqn; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getSourceEntityFqn() { return sourceEntityFqn; }
    public void setSourceEntityFqn(String sourceEntityFqn) { this.sourceEntityFqn = sourceEntityFqn; }

    public String getTargetEntityFqn() { return targetEntityFqn; }
    public void setTargetEntityFqn(String targetEntityFqn) { this.targetEntityFqn = targetEntityFqn; }

    public String getRelationType() { return relationType; }
    public void setRelationType(String relationType) { this.relationType = relationType; }

    public String getRelationSchemaFqn() { return relationSchemaFqn; }
    public void setRelationSchemaFqn(String relationSchemaFqn) { this.relationSchemaFqn = relationSchemaFqn; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public String getEmbedding() { return embedding; }
    public void setEmbedding(String embedding) { this.embedding = embedding; }

    public Integer getVersion() { return version; }
    public void setVersion(Integer version) { this.version = version; }

    public String getActivatedBy() { return activatedBy; }
    public void setActivatedBy(String activatedBy) { this.activatedBy = activatedBy; }

    public LocalDateTime getActivatedTime() { return activatedTime; }
    public void setActivatedTime(LocalDateTime activatedTime) { this.activatedTime = activatedTime; }
}
