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
package io.awspring.cloud.autoconfigure.config.s3;

import io.awspring.cloud.autoconfigure.AwsSyncClientCustomizer;
import io.awspring.cloud.autoconfigure.config.AbstractAwsConfigDataLocationResolver;
import io.awspring.cloud.autoconfigure.core.*;
import io.awspring.cloud.autoconfigure.s3.S3ClientCustomizer;
import io.awspring.cloud.autoconfigure.s3.properties.S3Properties;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.logging.Log;
import org.springframework.boot.BootstrapContext;
import org.springframework.boot.context.config.ConfigDataLocation;
import org.springframework.boot.context.config.ConfigDataLocationNotFoundException;
import org.springframework.boot.context.config.ConfigDataLocationResolverContext;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.logging.DeferredLogFactory;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3ClientBuilder;

/**
 * Resolves config data locations in AWS S3.
 *
 * @author Kunal Varpe
 * @author Matej Nedic
 * @since 3.3.0
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
	public List<S3ConfigDataResource> resolve(ConfigDataLocationResolverContext resolverContext,
			ConfigDataLocation location) throws ConfigDataLocationNotFoundException {

		S3Properties s3Properties = loadProperties(resolverContext.getBinder());
		List<S3ConfigDataResource> locations = new ArrayList<>();
		S3PropertySources propertySources = new S3PropertySources();
		List<String> contexts = getCustomContexts(location.getNonPrefixedValue(PREFIX));

		if (s3Properties.getConfig().isEnabled()) {
			registerBean(resolverContext, AwsProperties.class, loadAwsProperties(resolverContext.getBinder()));
			registerBean(resolverContext, S3Properties.class, s3Properties);
			registerBean(resolverContext, CredentialsProperties.class,
					loadCredentialsProperties(resolverContext.getBinder()));
			registerBean(resolverContext, RegionProperties.class, loadRegionProperties(resolverContext.getBinder()));

			registerAndPromoteBean(resolverContext, S3Client.class, this::createS3Client);

			contexts.forEach(propertySourceContext -> locations
					.add(new S3ConfigDataResource(propertySourceContext, location.isOptional(), propertySources)));

			if (!location.isOptional() && locations.isEmpty()) {
				throw new S3KeysMissingException(
						"No S3 keys provided in `spring.config.import=aws-s3:` configuration.");
			}
		}
		else {
			// create dummy resources with enabled flag set to false,
			// because returned locations cannot be empty
			contexts.forEach(propertySourceContext -> locations.add(
					new S3ConfigDataResource(propertySourceContext, location.isOptional(), false, propertySources)));
		}
		return locations;
	}

	private S3Client createS3Client(BootstrapContext context) {
		S3ClientBuilder builder = configure(S3Client.builder(), context.get(S3Properties.class), context);

		try {
			S3ClientCustomizer s3ClientCustomizer = context.get(S3ClientCustomizer.class);
			if (s3ClientCustomizer != null) {
				s3ClientCustomizer.customize(builder);
			}
		}
		catch (IllegalStateException e) {
			log.debug("Bean of type AwsSyncClientCustomizer is not registered: " + e.getMessage());
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

		return builder.build();
	}

	protected S3Properties loadProperties(Binder binder) {
		return binder.bind(S3Properties.PREFIX, Bindable.of(S3Properties.class)).orElseGet(S3Properties::new);
	}
}
