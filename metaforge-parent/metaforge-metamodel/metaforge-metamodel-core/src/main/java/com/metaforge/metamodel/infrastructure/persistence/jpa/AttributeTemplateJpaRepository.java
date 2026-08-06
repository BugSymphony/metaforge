package com.metaforge.metamodel.infrastructure.persistence.jpa;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AttributeTemplateJpaRepository extends JpaRepository<AttributeTemplateJpo, Long> {

    Optional<AttributeTemplateJpo> findByFqn(String fqn);

    List<AttributeTemplateJpo> findByBundleVersionFqn(String bundleVersionFqn);
}
