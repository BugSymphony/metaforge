package com.metaforge.metamodel.infrastructure.persistence.adapter;

import com.metaforge.metamodel.domain.model.entity.Package;
import com.metaforge.metamodel.domain.repository.PackageRepository;
import com.metaforge.metamodel.infrastructure.mapper.PackageMapper;
import com.metaforge.metamodel.infrastructure.persistence.jpa.PackageJpaRepository;
import com.metaforge.metamodel.infrastructure.persistence.jpa.PackageJpo;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Package 仓储适配器：Spring Data JPA 实现。
 */
@Component
public class PackageRepositoryAdapter implements PackageRepository {

    private final PackageJpaRepository jpaRepository;
    private final PackageMapper mapper;

    public PackageRepositoryAdapter(PackageJpaRepository jpaRepository, PackageMapper mapper) {
        this.jpaRepository = jpaRepository;
        this.mapper = mapper;
    }

    @Override
    public Package save(Package pkg) {
        PackageJpo jpo = mapper.toJpo(pkg);
        PackageJpo saved = jpaRepository.save(jpo);
        return mapper.toDomain(saved);
    }

    @Override
    public Optional<Package> findByFqn(String fqn) {
        return jpaRepository.findByFqn(fqn).map(mapper::toDomain);
    }

    @Override
    public List<Package> findByBundleVersionFqn(String bundleVersionFqn) {
        return jpaRepository.findByBundleVersionFqn(bundleVersionFqn).stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<Package> findByParentPackageFqn(String parentPackageFqn) {
        return jpaRepository.findByParentPackageFqn(parentPackageFqn).stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public boolean existsByFqnPrefix(String fqnPrefix) {
        return jpaRepository.existsByFqnStartingWith(fqnPrefix);
    }

    @Override
    public void delete(Package pkg) {
        PackageJpo jpo = mapper.toJpo(pkg);
        jpaRepository.delete(jpo);
    }
}
