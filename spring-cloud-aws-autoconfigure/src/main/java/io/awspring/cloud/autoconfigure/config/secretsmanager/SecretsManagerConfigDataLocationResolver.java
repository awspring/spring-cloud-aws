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

import io.awspring.cloud.autoconfigure.config.AbstractAwsConfigDataLocationResolver;
import io.awspring.cloud.autoconfigure.core.AwsProperties;
import io.awspring.cloud.autoconfigure.core.CredentialsProperties;
import io.awspring.cloud.autoconfigure.core.RegionProperties;
import java.util.ArrayList;
import java.util.List;
import org.springframework.boot.BootstrapContext;
import org.springframework.boot.context.config.ConfigDataLocation;
import org.springframework.boot.context.config.ConfigDataLocationNotFoundException;
import org.springframework.boot.context.config.ConfigDataLocationResolverContext;
import org.springframework.boot.context.config.Profiles;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import software.amazon.awssdk.services.secretsmanager.SecretsManagerClient;

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

	@Override
	protected String getPrefix() {
		return PREFIX;
	}

	@Override
	public List<SecretsManagerConfigDataResource> resolveProfileSpecific(
			ConfigDataLocationResolverContext resolverContext, ConfigDataLocation location, Profiles profiles)
			throws ConfigDataLocationNotFoundException {
		registerBean(resolverContext, AwsProperties.class, loadAwsProperties(resolverContext.getBinder()));
		registerBean(resolverContext, SecretsManagerProperties.class, loadProperties(resolverContext.getBinder()));
		registerBean(resolverContext, CredentialsProperties.class,
				loadCredentialsProperties(resolverContext.getBinder()));
		registerBean(resolverContext, RegionProperties.class, loadRegionProperties(resolverContext.getBinder()));

		registerAndPromoteBean(resolverContext, SecretsManagerClient.class, this::createAwsSecretsManagerClient);

		SecretsManagerPropertySources propertySources = new SecretsManagerPropertySources();

		List<String> contexts = getCustomContexts(location.getNonPrefixedValue(PREFIX));

		List<SecretsManagerConfigDataResource> locations = new ArrayList<>();
		contexts.forEach(propertySourceContext -> locations.add(
				new SecretsManagerConfigDataResource(propertySourceContext, location.isOptional(), propertySources)));

		if (!location.isOptional() && locations.isEmpty()) {
			throw new SecretsManagerKeysMissingException(
					"No Secrets Manager keys provided in `spring.config.import=aws-secretsmanager:` configuration.");
		}

		return locations;
	}

	protected SecretsManagerClient createAwsSecretsManagerClient(BootstrapContext context) {
		return configure(SecretsManagerClient.builder(), context.get(SecretsManagerProperties.class), context,
				AwsClientConfigurerSecretsManager.class).build();
	}

	protected SecretsManagerProperties loadProperties(Binder binder) {
		return binder.bind(SecretsManagerProperties.CONFIG_PREFIX, Bindable.of(SecretsManagerProperties.class))
				.orElseGet(SecretsManagerProperties::new);
	}

}
