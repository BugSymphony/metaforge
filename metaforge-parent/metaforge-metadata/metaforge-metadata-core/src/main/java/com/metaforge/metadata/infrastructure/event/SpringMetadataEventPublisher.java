package com.metaforge.metadata.infrastructure.event;

import java.util.Set;

import com.metaforge.metadata.api.event.MetadataChangeEvent;
import com.metaforge.metadata.api.event.MetadataChangeListener;
import com.metaforge.metadata.domain.event.MetadataEventPublisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

@Component
public class SpringMetadataEventPublisher implements MetadataEventPublisher {

	private static final Logger log = LoggerFactory.getLogger(SpringMetadataEventPublisher.class);
	private final ApplicationEventPublisher eventPublisher;
	private final Set<MetadataChangeListener> listeners;

	public SpringMetadataEventPublisher(ApplicationEventPublisher eventPublisher,
										Set<MetadataChangeListener> listeners) {
		this.eventPublisher = eventPublisher;
		this.listeners = listeners;
	}

	@Override
	public void publish(MetadataChangeEvent event) {
		log.info("发布元数据变更事件: fqn={} changeType={} version={}",
				event.getFqn(), event.getChangeType(), event.getVersion());
		eventPublisher.publishEvent(event);

		if (listeners.isEmpty())
			return;

		for (MetadataChangeListener listener : listeners) {
			listener.onMetadataChange(event);
		}
	}
}
