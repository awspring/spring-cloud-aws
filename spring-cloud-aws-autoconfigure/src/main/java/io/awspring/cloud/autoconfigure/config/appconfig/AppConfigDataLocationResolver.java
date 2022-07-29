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
package io.awspring.cloud.autoconfigure.config.appconfig;

import io.awspring.cloud.appconfig.RequestContext;
import io.awspring.cloud.autoconfigure.config.AbstractAwsConfigDataLocationResolver;
import io.awspring.cloud.autoconfigure.core.AwsClientCustomizer;
import io.awspring.cloud.autoconfigure.core.AwsProperties;
import io.awspring.cloud.autoconfigure.core.CredentialsProperties;
import io.awspring.cloud.autoconfigure.core.RegionProperties;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import org.apache.commons.logging.Log;
import org.springframework.boot.BootstrapContext;
import org.springframework.boot.context.config.ConfigDataLocation;
import org.springframework.boot.context.config.ConfigDataLocationNotFoundException;
import org.springframework.boot.context.config.ConfigDataLocationResolverContext;
import org.springframework.boot.context.config.Profiles;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import software.amazon.awssdk.services.appconfigdata.AppConfigDataClient;
import software.amazon.awssdk.services.appconfigdata.AppConfigDataClientBuilder;

public class AppConfigDataLocationResolver extends AbstractAwsConfigDataLocationResolver<AppConfigDataResource> {

	public static final String PREFIX = "aws-appconfig:";

	private final Log log;

	public AppConfigDataLocationResolver(Log log) {
		this.log = log;
	}

	@Override
	protected String getPrefix() {
		return PREFIX;
	}

	@Override
	public List<AppConfigDataResource> resolveProfileSpecific(ConfigDataLocationResolverContext resolverContext,
			ConfigDataLocation location, Profiles profiles) throws ConfigDataLocationNotFoundException {
		registerBean(resolverContext, AwsProperties.class, loadAwsProperties(resolverContext.getBinder()));
		registerBean(resolverContext, AppConfigProperties.class, loadProperties(resolverContext.getBinder()));
		registerBean(resolverContext, CredentialsProperties.class,
				loadCredentialsProperties(resolverContext.getBinder()));
		registerBean(resolverContext, RegionProperties.class, loadRegionProperties(resolverContext.getBinder()));

		registerAndPromoteBean(resolverContext, AppConfigDataClient.class, this::createAppConfigDataClient);

		AppConfigPropertySources sources = new AppConfigPropertySources();

		List<String> contexts = getCustomContexts(location.getNonPrefixedValue(PREFIX));

		List<AppConfigDataResource> locations = new ArrayList<>();
		contexts.forEach(propertySourceContext -> locations
				.add(new AppConfigDataResource(splitContext(propertySourceContext), location.isOptional(), sources)));

		return locations;
	}

	public static RequestContext splitContext(String context) {
		RequestContext.Builder builder = RequestContext.builder();
		AtomicReference<Integer> i = new AtomicReference<>(0);
		builder.context(context);
		Arrays.stream(context.split("___")).forEach(split -> {
			if (i.get() == 0) {
				builder.applicationIdentifier(split);
			}
			else if (i.get() == 1) {
				builder.configurationProfileIdentifier(split);
			}
			else if (i.get() == 2) {
				builder.environmentIdentifier(split);
			}
			i.set(i.get() + 1);
		});
		return builder.build();
	}

	protected AppConfigDataClient createAppConfigDataClient(BootstrapContext context) {
		AppConfigDataClientBuilder builder = configure(AppConfigDataClient.builder(),
				context.get(AppConfigProperties.class), context);
		try {
			AwsAppConfigDataClientCustomizer configurer = context.get(AwsAppConfigDataClientCustomizer.class);
			if (configurer != null) {
				AwsClientCustomizer.apply(configurer, builder);
			}
		}
		catch (IllegalStateException e) {
			log.debug("Bean of type AwsAppConfigDataClientCustomizer is not registered: " + e.getMessage());
		}
		return builder.build();
	}

	protected AppConfigProperties loadProperties(Binder binder) {
		return binder.bind(AppConfigProperties.CONFIG_PREFIX, Bindable.of(AppConfigProperties.class))
				.orElseGet(AppConfigProperties::new);
	}
}
