package com.metaforge.metamodel.infrastructure.persistence.adapter;

import com.metaforge.metamodel.domain.model.entity.AttributeTemplate;
import com.metaforge.metamodel.domain.repository.AttributeTemplateRepository;
import com.metaforge.metamodel.infrastructure.mapper.AttributeTemplateMapper;
import com.metaforge.metamodel.infrastructure.persistence.jpa.AttributeTemplateJpaRepository;
import com.metaforge.metamodel.infrastructure.persistence.jpa.AttributeTemplateJpo;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Component
public class AttributeTemplateRepositoryAdapter implements AttributeTemplateRepository {

    private final AttributeTemplateJpaRepository jpaRepository;
    private final AttributeTemplateMapper mapper;

    public AttributeTemplateRepositoryAdapter(AttributeTemplateJpaRepository jpaRepository,
                                               AttributeTemplateMapper mapper) {
        this.jpaRepository = jpaRepository;
        this.mapper = mapper;
    }

    @Override
    public AttributeTemplate save(AttributeTemplate entity) {
        AttributeTemplateJpo jpo = mapper.toJpo(entity);
        AttributeTemplateJpo saved = jpaRepository.save(jpo);
        return mapper.toDomain(saved);
    }

    @Override
    public Optional<AttributeTemplate> findByFqn(String fqn) {
        return jpaRepository.findByFqn(fqn).map(mapper::toDomain);
    }

    @Override
    public List<AttributeTemplate> findByBundleVersionFqn(String bundleVersionFqn) {
        return jpaRepository.findByBundleVersionFqn(bundleVersionFqn).stream()
                .map(mapper::toDomain).collect(Collectors.toList());
    }

    @Override
    public void delete(AttributeTemplate entity) {
        jpaRepository.delete(mapper.toJpo(entity));
    }
}
