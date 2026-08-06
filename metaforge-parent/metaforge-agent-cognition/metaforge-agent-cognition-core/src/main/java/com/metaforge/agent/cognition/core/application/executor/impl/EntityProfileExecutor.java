package com.metaforge.agent.cognition.core.application.executor.impl;

import com.metaforge.agent.cognition.api.enums.ContextMode;
import com.metaforge.agent.cognition.api.enums.PerspectiveCode;
import com.metaforge.agent.cognition.api.perspective.PerspectiveExecutionContext;
import com.metaforge.agent.cognition.api.perspective.PerspectiveExecutor;
import com.metaforge.agent.cognition.core.domain.model.entity.EntityProfile;
import com.metaforge.metadata.api.dto.response.MetadataEntityDto;
import com.metaforge.metamodel.api.dto.NativeAttributeDto;
import com.metaforge.metamodel.api.dto.response.EntitySchemaDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class EntityProfileExecutor implements PerspectiveExecutor {

    private static final Logger log = LoggerFactory.getLogger(EntityProfileExecutor.class);

    private final ExecutorSupport support;

    public EntityProfileExecutor(ExecutorSupport support) {
        this.support = support;
    }

    @Override
    public PerspectiveCode supportedPerspective() {
        return PerspectiveCode.ENTITY_PROFILE;
    }

    @Override
    public Object execute(PerspectiveExecutionContext ctx) {
        log.debug("执行实体画像视角: entityFqn={}, contextMode={}", ctx.entityFqn(), ctx.contextMode());

        EntityProfile profile = new EntityProfile();

        if (ctx.entityFqn() != null) {
            buildFromEntity(ctx.entityFqn(), profile);
        } else if (ctx.contextMode() == ContextMode.BUNDLE_LEVEL) {
            String bundleCode = ctx.bundleFqns() != null && !ctx.bundleFqns().isEmpty()
                    ? ctx.bundleFqns().get(0) : "";
            buildFromBundle(bundleCode, profile);
        } else {
            profile.setEmpty(true);
        }

        return profile;
    }

    private void buildFromEntity(String entityFqn, EntityProfile profile) {
        profile.setFqn(entityFqn);
        profile.setName(extractName(entityFqn));
        profile.setContent(new HashMap<>());
        profile.setSchemaAttributes(new ArrayList<>());
        profile.setCurrentVersion(1);

        Object raw = support.metadata().getByFqn(entityFqn);
        if (raw instanceof MetadataEntityDto entity) {
            profile.setName(entity.getName() != null ? entity.getName() : extractName(entityFqn));
            profile.setDescription(entity.getDescription());
            profile.setEntitySchemaFqn(entity.getEntitySchemaFqn());
            if (entity.getContent() != null) {
                profile.setContent(entity.getContent());
            }
            profile.setCurrentVersion(entity.getCurrentVersion());
            profile.setCreatedBy(entity.getCreatedBy());
            profile.setUpdatedBy(entity.getUpdatedBy());
            profile.setCreatedTime(entity.getCreatedTime());
            profile.setUpdatedTime(entity.getUpdatedTime());
            loadSchemaAttributes(entity.getEntitySchemaFqn(), profile);
        } else {
            profile.setEntitySchemaFqn(entityFqn + "/schema");
            log.debug("元数据实例不存在，保留 FQN 摘要: {}", entityFqn);
        }
    }

    private void buildFromBundle(String bundleCode, EntityProfile profile) {
        profile.setFqn(bundleCode);
        profile.setName("Bundle Profile");
        profile.setContent(new HashMap<>());
        profile.setSchemaAttributes(new ArrayList<>());
        profile.setCurrentVersion(1);

        Object raw = support.metamodel().getBundle(bundleCode);
        if (raw instanceof com.metaforge.metamodel.api.dto.response.BundleDto bundle) {
            profile.setName(bundle.getName() != null ? bundle.getName() : bundleCode);
            profile.setDescription(bundle.getDescription());
        }
    }

    private void loadSchemaAttributes(String entitySchemaFqn, EntityProfile profile) {
        if (entitySchemaFqn == null || entitySchemaFqn.isBlank()) {
            return;
        }
        Object raw = support.metamodel().getEntitySchema(entitySchemaFqn);
        if (raw instanceof EntitySchemaDto schema) {
            profile.setDescription(profile.getDescription() != null
                    ? profile.getDescription() : schema.getDescription());
            List<NativeAttributeDto> attributes = support.parseNativeAttributes(schema.getNativeAttributes());
            List<EntityProfile.NativeAttributeDetail> details = new ArrayList<>();
            for (NativeAttributeDto attr : attributes) {
                EntityProfile.NativeAttributeDetail detail = new EntityProfile.NativeAttributeDetail();
                detail.setName(attr.getName());
                detail.setType(attr.getType());
                detail.setRequired(attr.isRequired());
                detail.setDescription(attr.getDescription());
                detail.setConstraints(attr.getConstraints());
                details.add(detail);
            }
            profile.setSchemaAttributes(details);
        }
    }

    private String extractName(String fqn) {
        if (fqn == null) return "";
        String[] parts = fqn.split("\\.");
        return parts.length > 0 ? parts[parts.length - 1] : fqn;
    }
}
