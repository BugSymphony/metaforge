package com.metaforge.agent.cognition.core.application.executor.impl;

import com.metaforge.agent.cognition.api.enums.PerspectiveCode;
import com.metaforge.agent.cognition.api.perspective.PerspectiveExecutionContext;
import com.metaforge.agent.cognition.api.perspective.PerspectiveExecutor;
import com.metaforge.agent.cognition.core.domain.model.entity.BundleDirectory;
import com.metaforge.metamodel.api.dto.response.BundleDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class BundleDirectoryExecutor implements PerspectiveExecutor {

    private static final Logger log = LoggerFactory.getLogger(BundleDirectoryExecutor.class);

    private final ExecutorSupport support;

    public BundleDirectoryExecutor(ExecutorSupport support) {
        this.support = support;
    }

    @Override
    public PerspectiveCode supportedPerspective() {
        return PerspectiveCode.BUNDLE_DIRECTORY;
    }

    @Override
    public Object execute(PerspectiveExecutionContext ctx) {
        log.debug("执行 Bundle 目录视角: bundleFqns={}", ctx.bundleFqns());

        BundleDirectory directory = new BundleDirectory();
        directory.setBundles(new ArrayList<>());

        Object raw = support.metamodel().listBundles(1, Integer.MAX_VALUE);
        List<BundleDto> bundles = support.bundles(raw);
        if (bundles == null) {
            return directory;
        }

        List<BundleDirectory.BundleEntry> entries = new ArrayList<>();
        for (BundleDto bundle : bundles) {
            BundleDirectory.BundleEntry entry = new BundleDirectory.BundleEntry();
            entry.setFqn(bundle.getFqn());
            entry.setName(bundle.getName() != null ? bundle.getName() : bundle.getFqn());
            entry.setDescription(bundle.getDescription());
            entry.setOwner(bundle.getOwner());
            entry.setSystem(bundle.isSystem());
            entry.setDomainTree(new ArrayList<>());
            entries.add(entry);
        }
        directory.setBundles(entries);
        return directory;
    }
}
