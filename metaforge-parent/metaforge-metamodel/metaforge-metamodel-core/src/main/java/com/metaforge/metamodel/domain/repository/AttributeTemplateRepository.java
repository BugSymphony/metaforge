package com.metaforge.metamodel.domain.repository;

import com.metaforge.metamodel.domain.model.entity.AttributeTemplate;

import java.util.List;
import java.util.Optional;

public interface AttributeTemplateRepository {

    AttributeTemplate save(AttributeTemplate entity);

    Optional<AttributeTemplate> findByFqn(String fqn);

    List<AttributeTemplate> findByBundleVersionFqn(String bundleVersionFqn);

    void delete(AttributeTemplate entity);
}
