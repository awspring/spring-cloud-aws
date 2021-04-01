/*
 * Copyright 2013-2021 the original author or authors.
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

package io.awspring.cloud.v3.autoconfigure.secretsmanager;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import io.awspring.cloud.v3.core.SpringCloudClientConfiguration;
import org.apache.commons.logging.Log;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.secretsmanager.SecretsManagerClient;
import software.amazon.awssdk.services.secretsmanager.SecretsManagerClientBuilder;

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
 * Resolves config data locations in AWS Secrets Manager.
 *
 * @author Eddú Meléndez
 * @author Maciej Walkowiak
 * @author Arun Patra
 * @since 2.3.0
 */
public class SecretsManagerConfigDataLocationResolver
		implements ConfigDataLocationResolver<SecretsManagerConfigDataResource> {

	private final Log log;

	/**
	 * AWS Secrets Manager Config Data prefix.
	 */
	public static final String PREFIX = "aws-secretsmanager:";

	public SecretsManagerConfigDataLocationResolver(Log log) {
		this.log = log;
	}

	@Override
	public boolean isResolvable(ConfigDataLocationResolverContext context, ConfigDataLocation location) {
		if (!location.hasPrefix(PREFIX)) {
			return false;
		}
		return context.getBinder().bind(SecretsManagerProperties.CONFIG_PREFIX + ".enabled", Boolean.class)
				.orElse(true);
	}

	@Override
	public List<SecretsManagerConfigDataResource> resolve(ConfigDataLocationResolverContext context,
			ConfigDataLocation location) throws ConfigDataLocationNotFoundException {
		return Collections.emptyList();
	}

	@Override
	public List<SecretsManagerConfigDataResource> resolveProfileSpecific(
			ConfigDataLocationResolverContext resolverContext, ConfigDataLocation location, Profiles profiles)
			throws ConfigDataLocationNotFoundException {
		registerBean(resolverContext, SecretsManagerProperties.class, loadProperties(resolverContext.getBinder()));

		registerAndPromoteBean(resolverContext, SecretsManagerClient.class, this::createAwsSecretsManagerClient);

		SecretsManagerPropertySources propertySources = new SecretsManagerPropertySources(log);

		List<String> contexts = getCustomContexts(location.getNonPrefixedValue(PREFIX));

		List<SecretsManagerConfigDataResource> locations = new ArrayList<>();
		contexts.forEach(propertySourceContext -> locations.add(
				new SecretsManagerConfigDataResource(propertySourceContext, location.isOptional(), propertySources)));

		return locations;
	}

	private List<String> getCustomContexts(String keys) {
		if (!StringUtils.isEmpty(keys)) {
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

	protected SecretsManagerClient createAwsSecretsManagerClient(BootstrapContext context) {
		SecretsManagerProperties properties = context.get(SecretsManagerProperties.class);

		SecretsManagerClientBuilder builder = SecretsManagerClient.builder()
				.overrideConfiguration(SpringCloudClientConfiguration.clientOverrideConfiguration());

		if (StringUtils.hasLength(properties.getRegion())) {
			builder.region(Region.of(properties.getRegion()));
		}
		if (properties.getEndpoint() != null) {
			builder.endpointOverride(properties.getEndpoint());
		}

		return builder.build();
	}

	protected SecretsManagerProperties loadProperties(Binder binder) {
		return binder.bind(SecretsManagerProperties.CONFIG_PREFIX, Bindable.of(SecretsManagerProperties.class))
				.orElseGet(SecretsManagerProperties::new);
	}

}
