/*
 * Copyright 2013-2020 the original author or authors.
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

package org.springframework.cloud.aws.autoconfigure.cloudmap;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.amazonaws.services.servicediscovery.AWSServiceDiscovery;

import org.springframework.boot.BootstrapContext;
import org.springframework.boot.BootstrapRegistry;
import org.springframework.boot.ConfigurableBootstrapContext;
import org.springframework.boot.context.config.ConfigDataLocation;
import org.springframework.boot.context.config.ConfigDataLocationNotFoundException;
import org.springframework.boot.context.config.ConfigDataLocationResolver;
import org.springframework.boot.context.config.ConfigDataLocationResolverContext;
import org.springframework.boot.context.config.Profiles;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.cloud.aws.cloudmap.AwsCloudMapProperties;
import org.springframework.cloud.aws.cloudmap.AwsCloudMapPropertySources;

public class AwsCloudMapConfigDataLocationResolver
		implements ConfigDataLocationResolver<AwsCloudMapConfigDataResource> {

	@Override
	public boolean isResolvable(ConfigDataLocationResolverContext context, ConfigDataLocation location) {
		if (!location.hasPrefix(AwsCloudMapProperties.CONFIG_PREFIX)) {
			return false;
		}
		return context.getBinder().bind(AwsCloudMapProperties.CONFIG_PREFIX + ".enabled", Boolean.class).orElse(true);
	}

	@Override
	public List<AwsCloudMapConfigDataResource> resolve(ConfigDataLocationResolverContext context,
			ConfigDataLocation location) throws ConfigDataLocationNotFoundException {
		return Collections.emptyList();
	}

	@Override
	public List<AwsCloudMapConfigDataResource> resolveProfileSpecific(ConfigDataLocationResolverContext resolverContext,
			ConfigDataLocation location, Profiles profiles) throws ConfigDataLocationNotFoundException {
		registerBean(resolverContext, AwsCloudMapProperties.class, loadProperties(resolverContext.getBinder()));

		registerAndPromoteBean(resolverContext, AWSServiceDiscovery.class, this::createSimpleSystemManagementClient);

		AwsCloudMapProperties properties = loadConfigProperties(resolverContext.getBinder());

		AwsCloudMapPropertySources sources = new AwsCloudMapPropertySources(properties.getDiscovery());

		List<AwsCloudMapConfigDataResource> locations = new ArrayList<>();
		locations.add(new AwsCloudMapConfigDataResource(location.isOptional(), sources));

		return locations;
	}

	protected <T> void registerAndPromoteBean(ConfigDataLocationResolverContext context, Class<T> type,
			BootstrapRegistry.InstanceSupplier<T> supplier) {
		registerBean(context, type, supplier);
		context.getBootstrapContext().addCloseListener(event -> {
			T instance = event.getBootstrapContext().get(type);
			event.getApplicationContext().getBeanFactory().registerSingleton("configData" + type.getSimpleName(),
					instance);
		});
	}

	public <T> void registerBean(ConfigDataLocationResolverContext context, Class<T> type, T instance) {
		context.getBootstrapContext().registerIfAbsent(type, BootstrapRegistry.InstanceSupplier.of(instance));
	}

	protected <T> void registerBean(ConfigDataLocationResolverContext context, Class<T> type,
			BootstrapRegistry.InstanceSupplier<T> supplier) {
		ConfigurableBootstrapContext bootstrapContext = context.getBootstrapContext();
		bootstrapContext.registerIfAbsent(type, supplier);
	}

	protected AWSServiceDiscovery createSimpleSystemManagementClient(BootstrapContext context) {
		AwsCloudMapProperties properties = context.get(AwsCloudMapProperties.class);

		return AwsCloudMapBootstrapConfiguration.createServiceDiscoveryClient(properties);
	}

	protected AwsCloudMapProperties loadProperties(Binder binder) {
		return binder.bind(AwsCloudMapProperties.CONFIG_PREFIX, Bindable.of(AwsCloudMapProperties.class))
				.orElseGet(AwsCloudMapProperties::new);
	}

	protected AwsCloudMapProperties loadConfigProperties(Binder binder) {
		return binder.bind(AwsCloudMapProperties.CONFIG_PREFIX, Bindable.of(AwsCloudMapProperties.class))
				.orElseGet(AwsCloudMapProperties::new);
	}

}
