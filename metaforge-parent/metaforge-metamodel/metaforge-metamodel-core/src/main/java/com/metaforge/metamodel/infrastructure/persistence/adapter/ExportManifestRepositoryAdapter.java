package com.metaforge.metamodel.infrastructure.persistence.adapter;

import com.metaforge.metamodel.domain.model.aggregate.ExportManifest;
import com.metaforge.metamodel.domain.repository.ExportManifestRepository;
import com.metaforge.metamodel.infrastructure.persistence.jpa.ExportManifestJpo;
import com.metaforge.metamodel.infrastructure.persistence.jpa.ExportManifestJpaRepository;

import com.metaforge.common.util.JsonbUtils;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
public class ExportManifestRepositoryAdapter implements ExportManifestRepository {

    private final ExportManifestJpaRepository jpaRepository;

    public ExportManifestRepositoryAdapter(ExportManifestJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public ExportManifest save(ExportManifest manifest) {
        ExportManifestJpo jpo = toJpo(manifest);
        ExportManifestJpo saved = jpaRepository.save(jpo);
        return toDomain(saved);
    }

    @Override
    public Optional<ExportManifest> findByBundleVersionFqn(String bundleVersionFqn) {
        return jpaRepository.findByBundleVersionFqn(bundleVersionFqn).map(this::toDomain);
    }

    private ExportManifestJpo toJpo(ExportManifest m) {
        if (m == null) return null;
        ExportManifestJpo jpo = new ExportManifestJpo();
        jpo.setId(m.getId());
        jpo.setBundleVersionFqn(m.getBundleVersionFqn());
        jpo.setExportedPackageFqns(JsonbUtils.toJsonb(m.getExportedPackageFqns()));
        jpo.setCreatedTime(m.getCreatedTime());
        jpo.setUpdatedTime(m.getUpdatedTime());
        return jpo;
    }

    private ExportManifest toDomain(ExportManifestJpo jpo) {
        if (jpo == null) return null;
        ExportManifest m = new ExportManifest();
        m.setId(jpo.getId());
        m.setBundleVersionFqn(jpo.getBundleVersionFqn());
        m.setExportedPackageFqns(JsonbUtils.fromJsonbList(
                jpo.getExportedPackageFqns(), String.class));
        m.setCreatedTime(jpo.getCreatedTime());
        m.setUpdatedTime(jpo.getUpdatedTime());
        return m;
    }
}
