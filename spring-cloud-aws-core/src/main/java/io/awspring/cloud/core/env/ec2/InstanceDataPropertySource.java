/*
 * Copyright 2013-2021 the original author or authors.
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

package io.awspring.cloud.core.env.ec2;

import java.io.IOException;
import java.util.Properties;

import com.amazonaws.AmazonClientException;
import com.amazonaws.util.EC2MetadataUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.core.env.EnumerablePropertySource;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.support.PropertiesLoaderUtils;
import org.springframework.util.StringUtils;

/**
 * @author Agim Emruli
 * @author Eddú Meléndez
 * @since 2.3
 */
public class InstanceDataPropertySource extends EnumerablePropertySource<Object> {

	private static final Logger LOGGER = LoggerFactory.getLogger(InstanceDataPropertySource.class);

	private static final String EC2_METADATA_ROOT = "/latest/meta-data";

	private static final String DEFAULT_KNOWN_PROPERTIES_PATH = InstanceDataPropertySource.class.getSimpleName()
			+ ".properties";

	private static final Properties KNOWN_PROPERTY_NAMES;

	static {
		// Load all known properties from the classpath. This is not meant
		// to be changed by external developers.
		try {
			ClassPathResource resource = new ClassPathResource(DEFAULT_KNOWN_PROPERTIES_PATH,
					InstanceDataPropertySource.class);
			KNOWN_PROPERTY_NAMES = PropertiesLoaderUtils.loadProperties(resource);
		}
		catch (IOException ex) {
			throw new IllegalStateException(
					"Could not load '" + DEFAULT_KNOWN_PROPERTIES_PATH + "': " + ex.getMessage());
		}
	}

	public InstanceDataPropertySource(String name) {
		super(name, new Object());
	}

	private static String getRootPropertyName(String propertyName) {
		String[] propertyTokens = StringUtils.split(propertyName, "/");
		return propertyTokens != null ? propertyTokens[0] : propertyName;
	}

	@Override
	public Object getProperty(String name) {
		if (!KNOWN_PROPERTY_NAMES.containsKey(getRootPropertyName(name))) {
			return null;
		}

		try {
			return EC2MetadataUtils.getData(EC2_METADATA_ROOT + "/" + name);
		}
		catch (AmazonClientException e) {
			// Suppress exception if we are not able to contact the service,
			// because that is quite often the case if we run in unit tests outside the
			// environment.
			LOGGER.warn("Error getting instance meta-data with name '{}' error message is '{}'", name, e.getMessage());
			return null;
		}
	}

	@Override
	public String[] getPropertyNames() {
		return KNOWN_PROPERTY_NAMES.keySet().stream().map(Object::toString).distinct().toArray(String[]::new);
	}

}
