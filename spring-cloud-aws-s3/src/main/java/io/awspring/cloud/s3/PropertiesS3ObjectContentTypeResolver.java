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
package io.awspring.cloud.s3;

import java.io.IOException;
import java.util.Properties;
import org.jspecify.annotations.Nullable;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.support.PropertiesLoaderUtils;
import org.springframework.util.Assert;

/**
 * Resolves content type for S3 object from a properties file.
 * <p>
 * If properties file is not given by constructor, loads default list of known extension to content type relations from
 * a classpath.
 *
 * @author Maciej Walkowiak
 * @since 3.0
 */
public class PropertiesS3ObjectContentTypeResolver implements S3ObjectContentTypeResolver {
	private static final String PROPERTIES_FILE_LOCATION = "/io/awspring/cloud/s3/S3ObjectContentTypeResolver.properties";

	private final Properties properties;

	public PropertiesS3ObjectContentTypeResolver() {
		this(loadProperties());
	}

	private static Properties loadProperties() {
		try {
			return PropertiesLoaderUtils.loadProperties(new ClassPathResource(PROPERTIES_FILE_LOCATION));
		}
		catch (IOException e) {
			throw new S3Exception(
					"Error when loading properties from " + PROPERTIES_FILE_LOCATION + " for content type resolution",
					e);
		}
	}

	public PropertiesS3ObjectContentTypeResolver(Properties properties) {
		Assert.notNull(properties, "properties are required");
		this.properties = properties;
	}

	@Override
	public String resolveContentType(String fileName) {
		Assert.notNull(fileName, "fileName is required");

		String extension = resolveExtension(fileName);
		if (extension != null) {
			return properties.getProperty(extension);
		}
		else {
			return null;
		}
	}

	@Nullable
	public String resolveExtension(String fileName) {
		Assert.notNull(fileName, "fileName is required");

		if (fileName.contains(".")) {
			return fileName.substring(fileName.lastIndexOf(".") + 1);
		}
		else {
			return null;
		}
	}
}
