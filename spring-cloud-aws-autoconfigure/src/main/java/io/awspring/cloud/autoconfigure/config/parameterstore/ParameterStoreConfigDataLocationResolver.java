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

import io.awspring.cloud.autoconfigure.config.AbstractAwsConfigDataLocationResolver;
import io.awspring.cloud.autoconfigure.core.AwsProperties;
import io.awspring.cloud.autoconfigure.core.CredentialsProperties;
import io.awspring.cloud.autoconfigure.core.CredentialsProviderAutoConfiguration;
import io.awspring.cloud.autoconfigure.core.RegionProperties;
import io.awspring.cloud.autoconfigure.core.RegionProviderAutoConfiguration;
import io.awspring.cloud.core.SpringCloudClientConfiguration;
import java.util.ArrayList;
import java.util.List;
import org.springframework.boot.BootstrapContext;
import org.springframework.boot.context.config.ConfigDataLocation;
import org.springframework.boot.context.config.ConfigDataLocationNotFoundException;
import org.springframework.boot.context.config.ConfigDataLocationResolverContext;
import org.springframework.boot.context.config.Profiles;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.util.StringUtils;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.regions.providers.AwsRegionProvider;
import software.amazon.awssdk.services.ssm.SsmClient;
import software.amazon.awssdk.services.ssm.SsmClientBuilder;

/**
 * @author Eddú Meléndez
 * @since 2.3.0
 */
public class ParameterStoreConfigDataLocationResolver
		extends AbstractAwsConfigDataLocationResolver<ParameterStoreConfigDataResource> {

	/**
	 * AWS ParameterStore Config Data prefix.
	 */
	public static final String PREFIX = "aws-parameterstore:";

	@Override
	protected String getPrefix() {
		return PREFIX;
	}

	@Override
	public List<ParameterStoreConfigDataResource> resolveProfileSpecific(
			ConfigDataLocationResolverContext resolverContext, ConfigDataLocation location, Profiles profiles)
			throws ConfigDataLocationNotFoundException {
		registerBean(resolverContext, AwsProperties.class, loadAwsProperties(resolverContext.getBinder()));
		registerBean(resolverContext, ParameterStoreProperties.class, loadProperties(resolverContext.getBinder()));
		registerBean(resolverContext, CredentialsProperties.class,
				loadCredentialsProperties(resolverContext.getBinder()));
		registerBean(resolverContext, RegionProperties.class, loadRegionProperties(resolverContext.getBinder()));

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

	protected SsmClient createSimpleSystemManagementClient(BootstrapContext context) {
		ParameterStoreProperties properties = context.get(ParameterStoreProperties.class);

		AwsCredentialsProvider credentialsProvider;

		try {
			credentialsProvider = context.get(AwsCredentialsProvider.class);
		}
		catch (IllegalStateException e) {
			CredentialsProperties credentialsProperties = context.get(CredentialsProperties.class);
			credentialsProvider = CredentialsProviderAutoConfiguration.createCredentialsProvider(credentialsProperties);
		}

		AwsRegionProvider regionProvider;

		try {
			regionProvider = context.get(AwsRegionProvider.class);
		}
		catch (IllegalStateException e) {
			RegionProperties regionProperties = context.get(RegionProperties.class);
			regionProvider = RegionProviderAutoConfiguration.createRegionProvider(regionProperties);
		}

		AwsProperties awsProperties = context.get(AwsProperties.class);

		SsmClientBuilder builder = SsmClient.builder()
				.overrideConfiguration(new SpringCloudClientConfiguration().clientOverrideConfiguration());
		if (StringUtils.hasLength(properties.getRegion())) {
			builder.region(Region.of(properties.getRegion()));
		}
		else {
			builder.region(regionProvider.getRegion());
		}
		if (properties.getEndpoint() != null) {
			builder.endpointOverride(properties.getEndpoint());
		}
		else if (awsProperties.getEndpoint() != null) {
			builder.endpointOverride(awsProperties.getEndpoint());
		}
		builder.credentialsProvider(credentialsProvider);
		return builder.build();
	}

	protected ParameterStoreProperties loadProperties(Binder binder) {
		return binder.bind(ParameterStoreProperties.CONFIG_PREFIX, Bindable.of(ParameterStoreProperties.class))
				.orElseGet(ParameterStoreProperties::new);
	}
}
