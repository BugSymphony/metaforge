package com.metaforge.graph.infrastructure.persistence.adapter;

import com.metaforge.graph.api.constant.GraphErrorCode;
import com.metaforge.graph.infrastructure.config.GraphBizException;
import com.metaforge.graph.domain.repository.MetadataEntityGateway;
import com.metaforge.metadata.api.dto.response.MetadataEntityDto;
import com.metaforge.metadata.api.service.MetadataQueryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * 元数据访问适配器——通过 metaforge-metadata-api 的 MetadataQueryService 消费上游服务。
 *
 * <p>实现 {@link MetadataEntityGateway} 领域端口，将上游 metadata-management BC
 * 的元数据实体查询能力适配为本 BC 领域层可消费的接口。
 */
@Component
public class GraphMetadataGatewayAdapter implements MetadataEntityGateway {

    private static final Logger log = LoggerFactory.getLogger(GraphMetadataGatewayAdapter.class);

    private final MetadataQueryService metadataQueryService;

    public GraphMetadataGatewayAdapter(MetadataQueryService metadataQueryService) {
        this.metadataQueryService = metadataQueryService;
    }

    @Override
    public boolean isEntityActive(String entityFqn) {
        log.debug("检查实体是否生效: fqn={}", entityFqn);

        try {
            MetadataEntityDto entity = metadataQueryService.getByFqn(entityFqn);
            return entity != null;
        } catch (Exception e) {
            log.warn("实体不存在或已下线: fqn={}, reason={}", entityFqn, e.getMessage());
            throw new EntityNotActiveException("端点实体无效（不存在或已下线）: " + entityFqn);
        }
    }

    @Override
    public String getEntityInfo(String entityFqn) {
        log.debug("获取实体信息: fqn={}", entityFqn);

        try {
            MetadataEntityDto entity = metadataQueryService.getByFqn(entityFqn);
            return entity.getFqn();
        } catch (Exception e) {
            log.warn("获取实体信息失败: fqn={}, reason={}", entityFqn, e.getMessage());
            return null;
        }
    }

    public static class EntityNotActiveException extends GraphBizException {
        public EntityNotActiveException(String message) {
            super(GraphErrorCode.ENDPOINT_INVALID, message);
        }

        @Override
        public String getErrorCodeName() {
            return "ENDPOINT_INVALID";
        }
    }
}
