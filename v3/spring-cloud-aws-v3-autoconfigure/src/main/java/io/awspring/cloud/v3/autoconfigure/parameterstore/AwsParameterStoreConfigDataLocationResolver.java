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
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
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
import org.springframework.util.StringUtils;

/**
 * @author Eddú Meléndez
 * @since 2.3.0
 */
public class AwsParameterStoreConfigDataLocationResolver
		implements ConfigDataLocationResolver<AwsParameterStoreConfigDataResource> {

	/**
	 * AWS ParameterStore Config Data prefix.
	 */
	public static final String PREFIX = "aws-parameterstore:";

	private final Log log = LogFactory.getLog(AwsParameterStoreConfigDataLocationResolver.class);

	@Override
	public boolean isResolvable(ConfigDataLocationResolverContext context, ConfigDataLocation location) {
		if (!location.hasPrefix(PREFIX)) {
			return false;
		}
		return context.getBinder().bind(AwsParameterStoreProperties.CONFIG_PREFIX + ".enabled", Boolean.class)
				.orElse(true);
	}

	@Override
	public List<AwsParameterStoreConfigDataResource> resolve(ConfigDataLocationResolverContext context,
			ConfigDataLocation location) throws ConfigDataLocationNotFoundException {
		return Collections.emptyList();
	}

	@Override
	public List<AwsParameterStoreConfigDataResource> resolveProfileSpecific(
			ConfigDataLocationResolverContext resolverContext, ConfigDataLocation location, Profiles profiles)
			throws ConfigDataLocationNotFoundException {
		registerBean(resolverContext, AwsParameterStoreProperties.class, loadProperties(resolverContext.getBinder()));

		registerAndPromoteBean(resolverContext, SsmClient.class, this::createSimpleSystemManagementClient);

		AwsParameterStorePropertySources sources = new AwsParameterStorePropertySources(log);

		List<String> contexts = getCustomContexts(location.getNonPrefixedValue(PREFIX));

		List<AwsParameterStoreConfigDataResource> locations = new ArrayList<>();
		contexts.forEach(propertySourceContext -> locations
				.add(new AwsParameterStoreConfigDataResource(propertySourceContext, location.isOptional(), sources)));

		return locations;
	}

	private List<String> getCustomContexts(String keys) {
		if (StringUtils.hasLength(keys)) {
			return Arrays.asList(keys.split(";"));
		}
		return Collections.emptyList();
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

	protected SsmClient createSimpleSystemManagementClient(BootstrapContext context) {
		AwsParameterStoreProperties properties = context.get(AwsParameterStoreProperties.class);
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

	protected AwsParameterStoreProperties loadProperties(Binder binder) {
		return binder.bind(AwsParameterStoreProperties.CONFIG_PREFIX, Bindable.of(AwsParameterStoreProperties.class))
				.orElseGet(AwsParameterStoreProperties::new);
	}

}
