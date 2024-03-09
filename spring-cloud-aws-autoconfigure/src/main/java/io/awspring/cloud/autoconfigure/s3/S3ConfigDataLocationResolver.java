/*
 * Copyright 2013-2023 the original author or authors.
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
package io.awspring.cloud.autoconfigure.s3;

import io.awspring.cloud.autoconfigure.config.AbstractAwsConfigDataLocationResolver;
import io.awspring.cloud.autoconfigure.core.AwsClientCustomizer;
import io.awspring.cloud.autoconfigure.core.AwsProperties;
import io.awspring.cloud.autoconfigure.core.CredentialsProperties;
import io.awspring.cloud.autoconfigure.core.RegionProperties;
import io.awspring.cloud.autoconfigure.s3.properties.S3Properties;
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
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3ClientBuilder;

/**
 * Resolves config data locations in AWS S3.
 *
 * @author Kunal Varpe
 * @since 3.0.0
 */
public class S3ConfigDataLocationResolver extends AbstractAwsConfigDataLocationResolver<S3ConfigDataResource> {

	/**
	 * AWS S3 Config Data prefix.
	 */
	public static final String PREFIX = "aws-s3:";

	private final Log log;

	public S3ConfigDataLocationResolver(DeferredLogFactory deferredLogFactory) {
		this.log = deferredLogFactory.getLog(S3ConfigDataLocationResolver.class);
	}

	@Override
	protected String getPrefix() {
		return PREFIX;
	}

	@Override
	public List<S3ConfigDataResource> resolveProfileSpecific(ConfigDataLocationResolverContext resolverContext,
			ConfigDataLocation location, Profiles profiles) throws ConfigDataLocationNotFoundException {
		registerBean(resolverContext, AwsProperties.class, loadAwsProperties(resolverContext.getBinder()));
		registerBean(resolverContext, S3Properties.class, loadProperties(resolverContext.getBinder()));
		registerBean(resolverContext, CredentialsProperties.class,
				loadCredentialsProperties(resolverContext.getBinder()));
		registerBean(resolverContext, RegionProperties.class, loadRegionProperties(resolverContext.getBinder()));

		registerAndPromoteBean(resolverContext, S3Client.class, this::createAwsS3Client);

		S3PropertySources propertySources = new S3PropertySources();

		List<String> contexts = getCustomContexts(location.getNonPrefixedValue(PREFIX));

		List<S3ConfigDataResource> locations = new ArrayList<>();
		contexts.forEach(propertySourceContext -> locations
				.add(new S3ConfigDataResource(propertySourceContext, location.isOptional(), propertySources)));

		if (!location.isOptional() && locations.isEmpty()) {
			throw new S3KeysMissingException("No S3 keys provided in `spring.config.import=aws-s3:` configuration.");
		}

		return locations;
	}

	protected S3Client createAwsS3Client(BootstrapContext context) {
		S3ClientBuilder builder = configure(S3Client.builder(), context.get(S3Properties.class), context);
		try {
			AwsS3ClientCustomizer configurer = context.get(AwsS3ClientCustomizer.class);
			if (configurer != null) {
				AwsClientCustomizer.apply(configurer, builder);
			}
		}
		catch (IllegalStateException e) {
			log.debug("Bean of type AwsS3ClientCustomizer is not registered: " + e.getMessage());
		}
		return builder.build();
	}

	protected S3Properties loadProperties(Binder binder) {
		return binder.bind(S3Properties.PREFIX, Bindable.of(S3Properties.class)).orElseGet(S3Properties::new);
	}

}