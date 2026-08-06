package com.metaforge.metamodel.infrastructure.persistence.adapter;

import com.metaforge.metamodel.domain.repository.BundleDependencyRepository;
import com.metaforge.metamodel.infrastructure.persistence.jpa.BundleDependencyJpo;
import com.metaforge.metamodel.infrastructure.persistence.jpa.BundleDependencyJpaRepository;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Bundle 依赖仓储适配器：Spring Data JPA 实现。
 */
@Component
public class BundleDependencyRepositoryAdapter implements BundleDependencyRepository {

    private final BundleDependencyJpaRepository jpaRepository;

    public BundleDependencyRepositoryAdapter(BundleDependencyJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public void save(String sourceVersionFqn, String targetVersionFqn) {
        BundleDependencyJpo jpo = new BundleDependencyJpo();
        jpo.setSourceVersionFqn(sourceVersionFqn);
        jpo.setTargetVersionFqn(targetVersionFqn);
        jpaRepository.save(jpo);
    }

    @Override
    public List<String> findTargetFqnsBySource(String sourceVersionFqn) {
        return jpaRepository.findBySourceVersionFqn(sourceVersionFqn).stream()
                .map(BundleDependencyJpo::getTargetVersionFqn)
                .collect(Collectors.toList());
    }

    @Override
    public List<String> findSourceFqnsByTarget(String targetVersionFqn) {
        return jpaRepository.findByTargetVersionFqn(targetVersionFqn).stream()
                .map(BundleDependencyJpo::getSourceVersionFqn)
                .collect(Collectors.toList());
    }

    @Override
    public boolean exists(String sourceVersionFqn, String targetVersionFqn) {
        return jpaRepository.existsBySourceVersionFqnAndTargetVersionFqn(
                sourceVersionFqn, targetVersionFqn);
    }

    @Override
    public void delete(String sourceVersionFqn, String targetVersionFqn) {
        jpaRepository.deleteBySourceVersionFqnAndTargetVersionFqn(
                sourceVersionFqn, targetVersionFqn);
    }

    @Override
    public List<String> findAllSourceFqns() {
        return jpaRepository.findAll().stream()
                .map(BundleDependencyJpo::getSourceVersionFqn)
                .distinct()
                .collect(Collectors.toList());
    }
}
