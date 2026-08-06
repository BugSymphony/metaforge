package com.metaforge.graph.infrastructure.persistence.jpa;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * EntityRelationIndex JPA 持久化实体（JPO）。
 * 映射 semantic_relation_network.entity_relation_index 双向索引表。
 */
@Entity
@Table(name = "entity_relation_index", schema = "semantic_relation_network")
public class EntityRelationIndexJpo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "entity_fqn", nullable = false, length = 512)
    private String entityFqn;

    @Column(name = "direction", nullable = false, length = 8)
    private String direction;

    @Column(name = "relation_fqn", nullable = false, length = 1536)
    private String relationFqn;

    @Column(name = "created_time", nullable = false)
    private LocalDateTime createdTime;

    @PrePersist
    protected void onCreate() {
        if (createdTime == null) {
            createdTime = LocalDateTime.now();
        }
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getEntityFqn() { return entityFqn; }
    public void setEntityFqn(String entityFqn) { this.entityFqn = entityFqn; }

    public String getDirection() { return direction; }
    public void setDirection(String direction) { this.direction = direction; }

    public String getRelationFqn() { return relationFqn; }
    public void setRelationFqn(String relationFqn) { this.relationFqn = relationFqn; }

    public LocalDateTime getCreatedTime() { return createdTime; }
    public void setCreatedTime(LocalDateTime createdTime) { this.createdTime = createdTime; }
}
