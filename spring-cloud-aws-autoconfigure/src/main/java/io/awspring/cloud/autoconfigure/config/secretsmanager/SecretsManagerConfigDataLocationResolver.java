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
package io.awspring.cloud.autoconfigure.config.secretsmanager;

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
import org.springframework.boot.context.config.*;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.logging.DeferredLogFactory;
import software.amazon.awssdk.services.secretsmanager.SecretsManagerClient;
import software.amazon.awssdk.services.secretsmanager.SecretsManagerClientBuilder;

/**
 * Resolves config data locations in AWS Secrets Manager.
 *
 * @author Eddú Meléndez
 * @author Maciej Walkowiak
 * @author Arun Patra
 * @since 2.3.0
 */
public class SecretsManagerConfigDataLocationResolver
		extends AbstractAwsConfigDataLocationResolver<SecretsManagerConfigDataResource> {

	/**
	 * AWS Secrets Manager Config Data prefix.
	 */
	public static final String PREFIX = "aws-secretsmanager:";

	private final Log log;

	public SecretsManagerConfigDataLocationResolver(DeferredLogFactory deferredLogFactory) {
		this.log = deferredLogFactory.getLog(SecretsManagerConfigDataLocationResolver.class);
	}

	@Override
	protected String getPrefix() {
		return PREFIX;
	}

	@Override
	public List<SecretsManagerConfigDataResource> resolve(ConfigDataLocationResolverContext resolverContext,
			ConfigDataLocation location)
			throws ConfigDataLocationNotFoundException, ConfigDataResourceNotFoundException {
		SecretsManagerProperties secretsManagerProperties = loadProperties(resolverContext.getBinder());

		List<String> contexts = getCustomContexts(location.getNonPrefixedValue(PREFIX));
		List<SecretsManagerConfigDataResource> locations = new ArrayList<>();
		SecretsManagerPropertySources propertySources = new SecretsManagerPropertySources();

		if (secretsManagerProperties.isEnabled()) {
			registerBean(resolverContext, AwsProperties.class, loadAwsProperties(resolverContext.getBinder()));
			registerBean(resolverContext, SecretsManagerProperties.class, secretsManagerProperties);
			registerBean(resolverContext, CredentialsProperties.class,
					loadCredentialsProperties(resolverContext.getBinder()));
			registerBean(resolverContext, RegionProperties.class, loadRegionProperties(resolverContext.getBinder()));
			registerAndPromoteBean(resolverContext, SecretsManagerClient.class, this::createAwsSecretsManagerClient);

			contexts.forEach(
					propertySourceContext -> locations.add(new SecretsManagerConfigDataResource(propertySourceContext,
							location.isOptional(), propertySources)));

			if (!location.isOptional() && locations.isEmpty()) {
				throw new SecretsManagerKeysMissingException(
						"No Secrets Manager keys provided in `spring.config.import=aws-secretsmanager:` configuration.");
			}
		}
		else {
			// create dummy resources with enabled flag set to false,
			// because returned locations cannot be empty
			contexts.forEach(
					propertySourceContext -> locations.add(new SecretsManagerConfigDataResource(propertySourceContext,
							location.isOptional(), false, propertySources)));
		}

		return locations;
	}

	protected SecretsManagerClient createAwsSecretsManagerClient(BootstrapContext context) {
		SecretsManagerClientBuilder builder = configure(SecretsManagerClient.builder(),
				context.get(SecretsManagerProperties.class), context);
		try {
			AwsSecretsManagerClientCustomizer configurer = context.get(AwsSecretsManagerClientCustomizer.class);
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
			SecretsManagerClientCustomizer secretsManagerClientCustomizer = context
					.get(SecretsManagerClientCustomizer.class);
			if (secretsManagerClientCustomizer != null) {
				secretsManagerClientCustomizer.customize(builder);
			}
		}
		catch (IllegalStateException e) {
			log.debug("Bean of type SecretsManagerClientCustomizer is not registered: " + e.getMessage());
		}

		return builder.build();
	}

	protected SecretsManagerProperties loadProperties(Binder binder) {
		return binder.bind(SecretsManagerProperties.CONFIG_PREFIX, Bindable.of(SecretsManagerProperties.class))
				.orElseGet(SecretsManagerProperties::new);
	}

}
