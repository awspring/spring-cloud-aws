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

package io.awspring.cloud.v3.autoconfigure.parameterstore;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import io.awspring.cloud.v3.core.SpringCloudClientConfiguration;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.ssm.SsmClient;
import software.amazon.awssdk.services.ssm.SsmClientBuilder;

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
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.util.StringUtils;

/**
 * @author Eddú Meléndez
 * @since 2.3.0
 */
public class ParameterStoreConfigDataLocationResolver
		implements ConfigDataLocationResolver<ParameterStoreConfigDataResource> {

	/**
	 * AWS ParameterStore Config Data prefix.
	 */
	public static final String PREFIX = "aws-parameterstore:";

	@Override
	public boolean isResolvable(ConfigDataLocationResolverContext context, ConfigDataLocation location) {
		if (!location.hasPrefix(PREFIX)) {
			return false;
		}
		return context.getBinder().bind(ParameterStoreProperties.CONFIG_PREFIX + ".enabled", Boolean.class)
				.orElse(true);
	}

	@Override
	public List<ParameterStoreConfigDataResource> resolve(ConfigDataLocationResolverContext context,
			ConfigDataLocation location) throws ConfigDataLocationNotFoundException {
		return Collections.emptyList();
	}

	@Override
	public List<ParameterStoreConfigDataResource> resolveProfileSpecific(
			ConfigDataLocationResolverContext resolverContext, ConfigDataLocation location, Profiles profiles)
			throws ConfigDataLocationNotFoundException {
		registerBean(resolverContext, ParameterStoreProperties.class, loadProperties(resolverContext.getBinder()));

		registerAndPromoteBean(resolverContext, SsmClient.class, this::createSimpleSystemManagementClient);

		ParameterStorePropertySources sources = new ParameterStorePropertySources();

		List<String> contexts = getCustomContexts(location.getNonPrefixedValue(PREFIX));

		List<ParameterStoreConfigDataResource> locations = new ArrayList<>();
		contexts.forEach(propertySourceContext -> locations
				.add(new ParameterStoreConfigDataResource(propertySourceContext, location.isOptional(), sources)));

		if (!location.isOptional() && locations.isEmpty()) {
			throw new ParameterStoreKeysMissingException(
					"No Parameter Store keys provided in `spring.config.import=aws-parameterstore:` configuration.");
		}
		return locations;
	}

	private List<String> getCustomContexts(String keys) {
		if (StringUtils.hasLength(keys)) {
			return Arrays.asList(keys.split(";"));
		}
		return Collections.emptyList();
	}

	/**
	 * Since hook can be activated more then one time, ApplicationContext needs to be
	 * checked if bean is already registered to prevent Exception. See issue #108 for more
	 * information.
	 */
	protected <T> void registerAndPromoteBean(ConfigDataLocationResolverContext context, Class<T> type,
			BootstrapRegistry.InstanceSupplier<T> supplier) {
		registerBean(context, type, supplier);
		context.getBootstrapContext().addCloseListener(event -> {
			String name = "configData" + type.getSimpleName();
			T instance = event.getBootstrapContext().get(type);
			ConfigurableApplicationContext appContext = event.getApplicationContext();
			if (!appContext.getBeanFactory().containsBean(name)) {
				event.getApplicationContext().getBeanFactory().registerSingleton(name, instance);
			}
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

	protected SsmClient createSimpleSystemManagementClient(BootstrapContext context) {
		ParameterStoreProperties properties = context.get(ParameterStoreProperties.class);
		SsmClientBuilder builder = SsmClient.builder()
				.overrideConfiguration(SpringCloudClientConfiguration.clientOverrideConfiguration());
		if (StringUtils.hasLength(properties.getRegion())) {
			builder.region(Region.of(properties.getRegion()));
		}
		if (properties.getEndpoint() != null) {
			builder.endpointOverride(properties.getEndpoint());
		}
		return builder.build();
	}

	protected ParameterStoreProperties loadProperties(Binder binder) {
		return binder.bind(ParameterStoreProperties.CONFIG_PREFIX, Bindable.of(ParameterStoreProperties.class))
				.orElseGet(ParameterStoreProperties::new);
	}

}
