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
package io.awspring.cloud.autoconfigure.config.parameterstore;

import io.awspring.cloud.autoconfigure.AwsSyncClientCustomizer;
import io.awspring.cloud.autoconfigure.config.AbstractAwsConfigDataLocationResolver;
import io.awspring.cloud.autoconfigure.core.AwsClientCustomizer;
import io.awspring.cloud.autoconfigure.core.AwsProperties;
import io.awspring.cloud.autoconfigure.core.CredentialsProperties;
import io.awspring.cloud.autoconfigure.core.RegionProperties;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.logging.Log;
import org.springframework.boot.BootstrapContext;
import org.springframework.boot.context.config.ConfigDataLocation;
import org.springframework.boot.context.config.ConfigDataLocationNotFoundException;
import org.springframework.boot.context.config.ConfigDataLocationResolverContext;
import org.springframework.boot.context.config.Profiles;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.logging.DeferredLogFactory;
import software.amazon.awssdk.services.ssm.SsmClient;
import software.amazon.awssdk.services.ssm.SsmClientBuilder;

/**
 * @author Eddú Meléndez
 * @author Matej Nedic
 * @since 2.3.0
 */
public class ParameterStoreConfigDataLocationResolver
		extends AbstractAwsConfigDataLocationResolver<ParameterStoreConfigDataResource> {

	/**
	 * AWS ParameterStore Config Data prefix.
	 */
	public static final String PREFIX = "aws-parameterstore:";

	private final Log log;

	public ParameterStoreConfigDataLocationResolver(DeferredLogFactory deferredLogFactory) {
		this.log = deferredLogFactory.getLog(ParameterStoreConfigDataLocationResolver.class);
	}

	@Override
	protected String getPrefix() {
		return PREFIX;
	}

	@Override
	public List<ParameterStoreConfigDataResource> resolveProfileSpecific(
			ConfigDataLocationResolverContext resolverContext, ConfigDataLocation location, Profiles profiles)
			throws ConfigDataLocationNotFoundException {
		var properties = loadProperties(resolverContext.getBinder());
		List<ParameterStoreConfigDataResource> locations = new ArrayList<>();
		ParameterStorePropertySources sources = new ParameterStorePropertySources();
		List<String> contexts = getCustomContexts(location.getNonPrefixedValue(PREFIX));

		if (properties.isEnabled()) {
			registerBean(resolverContext, AwsProperties.class, loadAwsProperties(resolverContext.getBinder()));
			registerBean(resolverContext, ParameterStoreProperties.class, properties);
			registerBean(resolverContext, CredentialsProperties.class,
					loadCredentialsProperties(resolverContext.getBinder()));
			registerBean(resolverContext, RegionProperties.class, loadRegionProperties(resolverContext.getBinder()));

			registerAndPromoteBean(resolverContext, SsmClient.class, this::createSimpleSystemManagementClient);
			contexts.forEach(propertySourceContext -> locations
					.add(new ParameterStoreConfigDataResource(propertySourceContext, location.isOptional(), sources)));

			if (!location.isOptional() && locations.isEmpty()) {
				throw new ParameterStoreKeysMissingException(
						"No Parameter Store keys provided in `spring.config.import=aws-parameterstore:` configuration.");
			}
		}
		else {
			// create dummy resources with enabled flag set to false,
			// because returned locations cannot be empty
			contexts.forEach(
					propertySourceContext -> locations.add(new ParameterStoreConfigDataResource(propertySourceContext,
							location.isOptional(), false, sources)));
		}
		return locations;
	}

	protected SsmClient createSimpleSystemManagementClient(BootstrapContext context) {
		SsmClientBuilder builder = configure(SsmClient.builder(), context.get(ParameterStoreProperties.class), context);
		try {
			AwsParameterStoreClientCustomizer configurer = context.get(AwsParameterStoreClientCustomizer.class);
			if (configurer != null) {
				AwsClientCustomizer.apply(configurer, builder);
			}
		}
		catch (IllegalStateException e) {
			log.debug("Bean of type AwsParameterStoreClientCustomizer is not registered: " + e.getMessage());
		}

		try {
			AwsSyncClientCustomizer awsSyncClientCustomizer = context.get(AwsSyncClientCustomizer.class);
			if (awsSyncClientCustomizer != null) {
				awsSyncClientCustomizer.customize(builder);
			}
		}
		catch (IllegalStateException e) {
			log.debug("Bean of type AwsSyncClientCustomizer is not registered: " + e.getMessage());
		}

		try {
			SsmClientCustomizer ssmClientCustomizer = context.get(SsmClientCustomizer.class);
			if (ssmClientCustomizer != null) {
				ssmClientCustomizer.customize(builder);
			}
		}
		catch (IllegalStateException e) {
			log.debug("Bean of type SsmClientCustomizer is not registered: " + e.getMessage());
		}

		return builder.build();
	}

	protected ParameterStoreProperties loadProperties(Binder binder) {
		return binder.bind(ParameterStoreProperties.CONFIG_PREFIX, Bindable.of(ParameterStoreProperties.class))
				.orElseGet(ParameterStoreProperties::new);
	}
}
