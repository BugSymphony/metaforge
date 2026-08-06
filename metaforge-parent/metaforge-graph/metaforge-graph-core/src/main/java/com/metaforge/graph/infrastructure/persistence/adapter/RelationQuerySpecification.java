package com.metaforge.graph.infrastructure.persistence.adapter;

import com.metaforge.graph.infrastructure.persistence.jpa.RelationInstanceJpo;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;

/**
 * JPA Specification 动态查询构建器。
 * 支持维度间 AND，维度内 OR 的复合查询条件组装。
 */
public final class RelationQuerySpecification {

    private RelationQuerySpecification() {}

    public static Specification<RelationInstanceJpo> multiFilter(
            List<String> relationTypes,
            List<String> sourceEntityFqns,
            List<String> targetEntityFqns,
            List<String> relationSchemaFqns,
            String nameKeyword,
            String descriptionKeyword) {

        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (relationTypes != null && !relationTypes.isEmpty()) {
                predicates.add(root.get("relationType").in(relationTypes));
            }
            if (sourceEntityFqns != null && !sourceEntityFqns.isEmpty()) {
                List<Predicate> orPredicates = new ArrayList<>();
                for (String fqn : sourceEntityFqns) {
                    orPredicates.add(cb.like(root.get("sourceEntityFqn"), fqn + "%"));
                }
                predicates.add(cb.or(orPredicates.toArray(new Predicate[0])));
            }
            if (targetEntityFqns != null && !targetEntityFqns.isEmpty()) {
                List<Predicate> orPredicates = new ArrayList<>();
                for (String fqn : targetEntityFqns) {
                    orPredicates.add(cb.like(root.get("targetEntityFqn"), fqn + "%"));
                }
                predicates.add(cb.or(orPredicates.toArray(new Predicate[0])));
            }
            if (relationSchemaFqns != null && !relationSchemaFqns.isEmpty()) {
                predicates.add(root.get("relationSchemaFqn").in(relationSchemaFqns));
            }
            if (nameKeyword != null && !nameKeyword.isEmpty()) {
                predicates.add(cb.like(cb.lower(root.get("name")), "%" + nameKeyword.toLowerCase() + "%"));
            }
            if (descriptionKeyword != null && !descriptionKeyword.isEmpty()) {
                predicates.add(cb.like(cb.lower(root.get("description")), "%" + descriptionKeyword.toLowerCase() + "%"));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    public static Specification<RelationInstanceJpo> fqnPrefix(String fqnPrefix) {
        return (root, query, cb) -> {
            if (fqnPrefix == null || fqnPrefix.isEmpty()) {
                return cb.conjunction();
            }
            return cb.like(root.get("fqn"), fqnPrefix + "%");
        };
    }
}
