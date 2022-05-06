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
package io.awspring.cloud.autoconfigure.config;

import io.awspring.cloud.autoconfigure.core.AwsProperties;
import io.awspring.cloud.autoconfigure.core.CredentialsProperties;
import io.awspring.cloud.autoconfigure.core.RegionProperties;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.springframework.boot.BootstrapRegistry;
import org.springframework.boot.ConfigurableBootstrapContext;
import org.springframework.boot.context.config.ConfigDataLocation;
import org.springframework.boot.context.config.ConfigDataLocationNotFoundException;
import org.springframework.boot.context.config.ConfigDataLocationResolver;
import org.springframework.boot.context.config.ConfigDataLocationResolverContext;
import org.springframework.boot.context.config.ConfigDataResource;
import org.springframework.boot.context.config.ConfigDataResourceNotFoundException;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.util.StringUtils;

/**
 * Base class for AWS specific {@link ConfigDataLocationResolver}s.
 *
 * @param <T> - the location type
 * @author Maciej Walkowiak
 * @since 3.0
 */
public abstract class AbstractAwsConfigDataLocationResolver<T extends ConfigDataResource>
		implements ConfigDataLocationResolver<T> {

	protected abstract String getPrefix();

	@Override
	public boolean isResolvable(ConfigDataLocationResolverContext context, ConfigDataLocation location) {
		return location.hasPrefix(getPrefix());
	}

	@Override
	public List<T> resolve(ConfigDataLocationResolverContext context, ConfigDataLocation location)
			throws ConfigDataLocationNotFoundException, ConfigDataResourceNotFoundException {
		return Collections.emptyList();
	}

	protected <C> void registerAndPromoteBean(ConfigDataLocationResolverContext context, Class<C> type,
			BootstrapRegistry.InstanceSupplier<C> supplier) {
		registerBean(context, type, supplier);
		context.getBootstrapContext().addCloseListener(event -> {
			String name = "configData" + type.getSimpleName();
			C instance = event.getBootstrapContext().get(type);
			ConfigurableApplicationContext appContext = event.getApplicationContext();
			// Since hook can be activated more than one time, ApplicationContext needs to
			// be checked if bean is already registered to prevent Exception. See
			// https://github.com/awspring/spring-cloud-aws/issues/108 for more
			// information.
			if (!appContext.getBeanFactory().containsBean(name)) {
				event.getApplicationContext().getBeanFactory().registerSingleton(name, instance);
			}
		});
	}

	protected <C> void registerBean(ConfigDataLocationResolverContext context, Class<C> type, C instance) {
		context.getBootstrapContext().registerIfAbsent(type, BootstrapRegistry.InstanceSupplier.of(instance));
	}

	protected <C> void registerBean(ConfigDataLocationResolverContext context, Class<C> type,
			BootstrapRegistry.InstanceSupplier<C> supplier) {
		ConfigurableBootstrapContext bootstrapContext = context.getBootstrapContext();
		bootstrapContext.registerIfAbsent(type, supplier);
	}

	protected CredentialsProperties loadCredentialsProperties(Binder binder) {
		return binder.bind(CredentialsProperties.PREFIX, Bindable.of(CredentialsProperties.class))
				.orElseGet(CredentialsProperties::new);
	}

	protected RegionProperties loadRegionProperties(Binder binder) {
		return binder.bind(RegionProperties.PREFIX, Bindable.of(RegionProperties.class))
				.orElseGet(RegionProperties::new);
	}

	protected AwsProperties loadAwsProperties(Binder binder) {
		return binder.bind(AwsProperties.CONFIG_PREFIX, Bindable.of(AwsProperties.class)).orElseGet(AwsProperties::new);
	}

	protected List<String> getCustomContexts(String keys) {
		if (StringUtils.hasLength(keys)) {
			return Arrays.asList(keys.split(";"));
		}
		return Collections.emptyList();
	}

}
