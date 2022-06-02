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

import io.awspring.cloud.secretsmanager.SecretsManagerPropertySource;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import software.amazon.awssdk.services.secretsmanager.SecretsManagerClient;

/**
 * Provides prefix config import support.
 *
 * @author Eddú Meléndez
 * @author Maciej Walkowiak
 * @author Arun Patra
 */
public class SecretsManagerPropertySources {

	private static Log LOG = LogFactory.getLog(SecretsManagerPropertySources.class);

	/**
	 * Creates property source for given context.
	 * @param context property source context equivalent to the secret name
	 * @param optional if creating context should fail with exception if secret cannot be loaded
	 * @param client Secret Manager client
	 * @return a property source or null if secret could not be loaded and optional is set to true
	 */
	@Nullable
	public SecretsManagerPropertySource createPropertySource(String context, boolean optional,
			SecretsManagerClient client) {
		Assert.notNull(context, "context is required");
		Assert.notNull(client, "SecretsManagerClient is required");

		LOG.info("Loading secrets from AWS Secret Manager secret with name: " + context + ", optional: " + optional);
		try {
			SecretsManagerPropertySource propertySource = new SecretsManagerPropertySource(context, client);
			propertySource.init();
			return propertySource;
			// TODO: howto call close when /refresh
		}
		catch (Exception e) {
			if (!optional) {
				throw new AwsSecretsManagerPropertySourceNotFoundException(e);
			}
			else {
				LOG.warn("Unable to load AWS secret from " + context + ". " + e.getMessage());
			}
		}
		return null;
	}

	static class AwsSecretsManagerPropertySourceNotFoundException extends RuntimeException {

		AwsSecretsManagerPropertySourceNotFoundException(Exception source) {
			super(source);
		}

	}

}
