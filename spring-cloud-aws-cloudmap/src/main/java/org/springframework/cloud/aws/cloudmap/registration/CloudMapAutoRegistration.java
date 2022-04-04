package org.springframework.cloud.aws.cloudmap.registration;

import com.amazonaws.services.servicediscovery.AWSServiceDiscovery;
import org.springframework.cloud.aws.cloudmap.CloudMapUtils;
import org.springframework.cloud.aws.cloudmap.model.registration.CloudMapRegistryProperties;
import org.springframework.cloud.client.discovery.event.InstanceRegisteredEvent;
import org.springframework.cloud.client.serviceregistry.AutoServiceRegistration;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.EnvironmentAware;
import org.springframework.context.SmartLifecycle;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.context.event.SmartApplicationListener;
import org.springframework.core.Ordered;
import org.springframework.core.env.Environment;

import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

public class CloudMapAutoRegistration implements AutoServiceRegistration, SmartLifecycle,
	Ordered, SmartApplicationListener, EnvironmentAware {

	private final AWSServiceDiscovery serviceDiscovery;
	private final CloudMapRegistryProperties properties;
	private final ApplicationContext context;
	private final AtomicBoolean running = new AtomicBoolean(false);
	private final CloudMapUtils UTILS = CloudMapUtils.INSTANCE.getInstance();

	private Environment environment;
	private Map<String, String> attributesMap;

	public CloudMapAutoRegistration(ApplicationContext context, AWSServiceDiscovery serviceDiscovery,
									CloudMapRegistryProperties properties) {
		this.context = context;
		this.serviceDiscovery = serviceDiscovery;
		this.properties = properties;
	}

	@Override
	public boolean isAutoStartup() {
		return true;
	}

	@Override
	public void stop(Runnable callback) {
		stop();
		callback.run();
	}

	@Override
	public int getPhase() {
		return 0;
	}

	@Override
	public int getOrder() {
		return 0;
	}

	@Override
	public void onApplicationEvent(ApplicationEvent event) {
		if (event instanceof ContextClosedEvent) {
			onApplicationEvent((ContextClosedEvent) event);
		}
	}

	@Override
	public void start() {
		if (!this.running.get()) {
			final Map<String, String> attributesMap = UTILS.registerInstance(serviceDiscovery, properties, environment);
			if (attributesMap != null && attributesMap.containsKey(UTILS.SERVICE_INSTANCE_ID)) {
				this.attributesMap = attributesMap;
				this.context.publishEvent(new InstanceRegisteredEvent<>(this, attributesMap));
				this.running.set(true);
			}
		}
	}

	@Override
	public void stop() {
		if (this.running.get() && attributesMap != null && attributesMap.containsKey(UTILS.SERVICE_INSTANCE_ID)) {
			UTILS.deregisterInstance(serviceDiscovery, attributesMap);
			this.running.set(false);
		}
	}

	@Override
	public boolean isRunning() {
		return this.running.get();
	}

	@Override
	public boolean supportsEventType(Class<? extends ApplicationEvent> eventType) {
		return true;
	}

	@Override
	public void setEnvironment(Environment environment) {
		this.environment = environment;
	}

	public void onApplicationEvent(ContextClosedEvent event) {
		stop();
	}
}
