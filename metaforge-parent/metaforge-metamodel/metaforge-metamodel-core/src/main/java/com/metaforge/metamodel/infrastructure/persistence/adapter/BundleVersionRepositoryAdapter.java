package com.metaforge.metamodel.infrastructure.persistence.adapter;

import com.metaforge.metamodel.api.enums.VersionStatus;
import com.metaforge.metamodel.domain.model.aggregate.BundleVersion;
import com.metaforge.metamodel.domain.repository.BundleVersionRepository;
import com.metaforge.metamodel.infrastructure.mapper.BundleVersionMapper;
import com.metaforge.metamodel.infrastructure.persistence.jpa.BundleVersionJpaRepository;
import com.metaforge.metamodel.infrastructure.persistence.jpa.BundleVersionJpo;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * BundleVersion 仓储适配器：Spring Data JPA 实现。
 */
@Component
public class BundleVersionRepositoryAdapter implements BundleVersionRepository {

    private final BundleVersionJpaRepository jpaRepository;
    private final BundleVersionMapper mapper;

    public BundleVersionRepositoryAdapter(BundleVersionJpaRepository jpaRepository,
                                           BundleVersionMapper mapper) {
        this.jpaRepository = jpaRepository;
        this.mapper = mapper;
    }

    @Override
    public BundleVersion save(BundleVersion version) {
        BundleVersionJpo jpo = mapper.toJpo(version);
        BundleVersionJpo saved = jpaRepository.save(jpo);
        return mapper.toDomain(saved);
    }

    @Override
    public Optional<BundleVersion> findByFqn(String fqn) {
        return jpaRepository.findByFqn(fqn).map(mapper::toDomain);
    }

    @Override
    public List<BundleVersion> findByBundleFqnAndStatus(String bundleFqn, VersionStatus status) {
        return jpaRepository.findByBundleFqnAndStatus(bundleFqn, status.name()).stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public Optional<BundleVersion> findTopByBundleFqnAndStatusOrderByCreatedTimeDesc(
            String bundleFqn, VersionStatus status) {
        return jpaRepository.findTopByBundleFqnAndStatusOrderByCreatedTimeDesc(
                bundleFqn, status.name()).map(mapper::toDomain);
    }

    @Override
    public boolean existsByBundleFqnAndStatus(String bundleFqn, VersionStatus status) {
        return jpaRepository.existsByBundleFqnAndStatus(bundleFqn, status.name());
    }

    @Override
    public List<BundleVersion> findByBundleFqnOrderByCreatedTimeDesc(String bundleFqn) {
        return jpaRepository.findByBundleFqnOrderByCreatedTimeDesc(bundleFqn).stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
    }
}
