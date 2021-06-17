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

package io.awspring.cloud.autoconfigure.paramstore;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import com.amazonaws.services.simplesystemsmanagement.AWSSimpleSystemsManagement;
import io.awspring.cloud.paramstore.AwsParamStoreProperties;
import io.awspring.cloud.paramstore.AwsParamStorePropertySources;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

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
import org.springframework.lang.Nullable;
import org.springframework.util.StringUtils;

/**
 * @author Eddú Meléndez
 * @since 2.3.0
 */
public class AwsParamStoreConfigDataLocationResolver
		implements ConfigDataLocationResolver<AwsParamStoreConfigDataResource> {

	/**
	 * AWS ParameterStore Config Data prefix.
	 */
	public static final String PREFIX = "aws-parameterstore:";

	private final Log log = LogFactory.getLog(AwsParamStoreConfigDataLocationResolver.class);

	@Override
	public boolean isResolvable(ConfigDataLocationResolverContext context, ConfigDataLocation location) {
		if (!location.hasPrefix(PREFIX)) {
			return false;
		}
		return context.getBinder().bind(AwsParamStoreProperties.CONFIG_PREFIX + ".enabled", Boolean.class).orElse(true);
	}

	@Override
	public List<AwsParamStoreConfigDataResource> resolve(ConfigDataLocationResolverContext resolverContext,
			ConfigDataLocation location) throws ConfigDataLocationNotFoundException {
		return resolve(resolverContext, location, null);
	}

	@Override
	public List<AwsParamStoreConfigDataResource> resolveProfileSpecific(
			ConfigDataLocationResolverContext resolverContext, ConfigDataLocation location, Profiles profiles)
			throws ConfigDataLocationNotFoundException {
		return resolve(resolverContext, location, profiles);
	}

	private List<AwsParamStoreConfigDataResource> resolve(ConfigDataLocationResolverContext resolverContext,
			ConfigDataLocation location, @Nullable Profiles profiles) throws ConfigDataLocationNotFoundException {

		registerBean(resolverContext, AwsParamStoreProperties.class, loadProperties(resolverContext.getBinder()));

		registerAndPromoteBean(resolverContext, AWSSimpleSystemsManagement.class,
				this::createSimpleSystemManagementClient);

		AwsParamStoreProperties properties = loadConfigProperties(resolverContext.getBinder());

		AwsParamStorePropertySources sources = new AwsParamStorePropertySources(properties, log);

		if (location.getValue().equals(PREFIX) && profiles == null) {
			throw new IllegalArgumentException(
					"Automatic paths from profiles are not supported in profile specific application properties files. Move 'spring.config.import' to root application properties file or set custom context.");
		}

		List<String> contexts = location.getValue().equals(PREFIX)
				? sources.getAutomaticContexts(profiles.getAccepted())
				: getCustomContexts(location.getNonPrefixedValue(PREFIX));

		List<AwsParamStoreConfigDataResource> locations = new ArrayList<>();
		contexts.forEach(propertySourceContext -> locations
				.add(new AwsParamStoreConfigDataResource(propertySourceContext, location.isOptional(), sources)));

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

	protected AWSSimpleSystemsManagement createSimpleSystemManagementClient(BootstrapContext context) {
		AwsParamStoreProperties properties = context.get(AwsParamStoreProperties.class);

		return AwsParamStoreBootstrapConfiguration.createSimpleSystemManagementClient(properties);
	}

	protected AwsParamStoreProperties loadProperties(Binder binder) {
		return binder.bind(AwsParamStoreProperties.CONFIG_PREFIX, Bindable.of(AwsParamStoreProperties.class))
				.orElseGet(AwsParamStoreProperties::new);
	}

	protected AwsParamStoreProperties loadConfigProperties(Binder binder) {
		AwsParamStoreProperties properties = binder
				.bind(AwsParamStoreProperties.CONFIG_PREFIX, Bindable.of(AwsParamStoreProperties.class))
				.orElseGet(AwsParamStoreProperties::new);

		if (!StringUtils.hasLength(properties.getName())) {
			properties.setName(binder.bind("spring.application.name", String.class).orElse("application"));
		}

		return properties;
	}

}
