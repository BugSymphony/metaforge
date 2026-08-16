package com.metaforge.agent.cognition.core.infrastructure.registry;

import com.metaforge.agent.cognition.api.spi.CognitionOperator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class OperatorRegistryInitializer implements ApplicationListener<ApplicationReadyEvent> {

    private static final Logger log = LoggerFactory.getLogger(OperatorRegistryInitializer.class);

    private final OperatorRegistry operatorRegistry;
    private final List<CognitionOperator> operators;

    public OperatorRegistryInitializer(OperatorRegistry operatorRegistry,
                                        @Autowired(required = false) List<CognitionOperator> operators) {
        this.operatorRegistry = operatorRegistry;
        this.operators = operators != null ? operators : new ArrayList<>();
    }

    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {
        log.info("开始注册认知算子 SPI 实现: 发现 {} 个 Bean", operators.size());
        operatorRegistry.registerAll(operators);
    }
}
