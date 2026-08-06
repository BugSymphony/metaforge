package com.metaforge.metamodel.infrastructure.persistence.adapter;

import com.metaforge.metamodel.domain.model.aggregate.Bundle;
import com.metaforge.metamodel.domain.repository.BundleRepository;
import com.metaforge.metamodel.infrastructure.mapper.BundleMapper;
import com.metaforge.metamodel.infrastructure.persistence.jpa.BundleJpaRepository;
import com.metaforge.metamodel.infrastructure.persistence.jpa.BundleJpo;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Bundle 仓储适配器：Spring Data JPA 实现。
 */
@Component
public class BundleRepositoryAdapter implements BundleRepository {

    private final BundleJpaRepository jpaRepository;
    private final BundleMapper mapper;

    public BundleRepositoryAdapter(BundleJpaRepository jpaRepository, BundleMapper mapper) {
        this.jpaRepository = jpaRepository;
        this.mapper = mapper;
    }

    @Override
    public Bundle save(Bundle bundle) {
        BundleJpo jpo = mapper.toJpo(bundle);
        BundleJpo saved = jpaRepository.save(jpo);
        return mapper.toDomain(saved);
    }

    @Override
    public Optional<Bundle> findByFqn(String fqn) {
        return jpaRepository.findByFqn(fqn).map(mapper::toDomain);
    }

    @Override
    public boolean existsByFqn(String fqn) {
        return jpaRepository.existsByFqn(fqn);
    }

    @Override
    public List<Bundle> findAll() {
        return jpaRepository.findAll().stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public void delete(Bundle bundle) {
        BundleJpo jpo = mapper.toJpo(bundle);
        jpaRepository.delete(jpo);
    }
}
