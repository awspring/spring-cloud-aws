/*
 * Copyright 2013-2022 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.awspring.cloud.autoconfigure.cloudmap.registration;

import io.awspring.cloud.autoconfigure.cloudmap.CloudMapUtils;
import io.awspring.cloud.autoconfigure.cloudmap.properties.registration.CloudMapRegistryProperties;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
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
import org.springframework.lang.Nullable;
import software.amazon.awssdk.services.servicediscovery.ServiceDiscoveryClient;

public class CloudMapAutoRegistration
		implements AutoServiceRegistration, SmartLifecycle, Ordered, SmartApplicationListener, EnvironmentAware {

	private final ServiceDiscoveryClient serviceDiscovery;

	private final CloudMapRegistryProperties properties;

	private final ApplicationContext context;

	private final AtomicBoolean running = new AtomicBoolean(false);

	private final CloudMapUtils UTILS = CloudMapUtils.getInstance();

	@Nullable
	private Environment environment;

	private Map<String, String> attributesMap = new HashMap<>();

	public CloudMapAutoRegistration(ApplicationContext context, ServiceDiscoveryClient serviceDiscovery,
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
		if (this.running.get() && !attributesMap.isEmpty() && attributesMap.containsKey(UTILS.SERVICE_INSTANCE_ID)) {
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