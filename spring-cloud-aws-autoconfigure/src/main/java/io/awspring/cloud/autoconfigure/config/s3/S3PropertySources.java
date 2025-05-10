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

import io.awspring.cloud.s3.config.S3PropertySource;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import software.amazon.awssdk.services.s3.S3Client;

/**
 * Provides prefix config import support.
 *
 * @author Kunal Varpe
 * @since 3.3.0
 */
public class S3PropertySources {

	private static Log LOG = LogFactory.getLog(S3PropertySources.class);

	/**
	 * Creates property source for given context.
	 * @param context property source context equivalent to s3 bucket file
	 * @param optional if creating context should fail with exception if s3 bucket file cannot be loaded
	 * @param client S3 client
	 * @return a property source or null if s3 bucket file could not be loaded and optional is set to true
	 */
	@Nullable
	public S3PropertySource createPropertySource(String context, boolean optional, S3Client client) {
		Assert.notNull(context, "context is required");
		Assert.notNull(client, "S3Client is required");

		LOG.info("Loading properties from AWS S3 object: " + context + ", optional: " + optional);
		try {
			S3PropertySource propertySource = new S3PropertySource(context, client);
			propertySource.init();
			return propertySource;
		}
		catch (Exception e) {
			LOG.warn("Unable to load AWS S3 object from " + context + ". " + e.getMessage());
			if (!optional) {
				throw new AwsS3PropertySourceNotFoundException(e);
			}
		}
		return null;
	}

	static class AwsS3PropertySourceNotFoundException extends RuntimeException {

		AwsS3PropertySourceNotFoundException(Exception source) {
			super(source);
		}

	}

}
