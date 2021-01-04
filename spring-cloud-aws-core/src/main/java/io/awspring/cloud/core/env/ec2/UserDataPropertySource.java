/*
 * Copyright 2013-2019 the original author or authors.
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

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import com.amazonaws.AmazonClientException;
import com.amazonaws.util.EC2MetadataUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.beans.factory.config.PlaceholderConfigurerSupport;
import org.springframework.core.env.EnumerablePropertySource;
import org.springframework.util.StringUtils;

/**
 * @author Agim Emruli
 * @author Eddú Meléndez
 * @since 2.3
 */
public class UserDataPropertySource extends EnumerablePropertySource<Object> {

	private static final Logger LOGGER = LoggerFactory.getLogger(UserDataPropertySource.class);

	private static final String DEFAULT_USER_DATA_ATTRIBUTE_SEPARATOR = ";";

	private String userDataAttributeSeparator = DEFAULT_USER_DATA_ATTRIBUTE_SEPARATOR;

	private String userDataValueSeparator = PlaceholderConfigurerSupport.DEFAULT_VALUE_SEPARATOR;

	private volatile Map<String, String> cachedUserData;

	public UserDataPropertySource(String name) {
		super(name, new Object());
	}

	public void setUserDataAttributeSeparator(String userDataAttributeSeparator) {
		this.userDataAttributeSeparator = userDataAttributeSeparator;
	}

	public void setUserDataValueSeparator(String userDataValueSeparator) {
		this.userDataValueSeparator = userDataValueSeparator;
	}

	@Override
	public Object getProperty(String name) {
		Map<String, String> userData = getUserData();
		if (userData.containsKey(name)) {
			return userData.get(name);
		}
		return null;
	}

	private Map<String, String> getUserData() {
		if (this.cachedUserData == null) {
			Map<String, String> userDataMap = new LinkedHashMap<>();
			String userData = null;
			try {
				userData = EC2MetadataUtils.getUserData();
			}
			catch (AmazonClientException e) {
				// Suppress exception if we are not able to contact the service,
				// because that is quite often the case if we run in unit tests outside
				// the environment.
				LOGGER.warn("Error getting instance user-data error message is '{}'", e.getMessage());
			}
			if (StringUtils.hasText(userData)) {
				String[] userDataAttributes = userData.split(this.userDataAttributeSeparator);
				for (String userDataAttribute : userDataAttributes) {
					String[] userDataAttributesParts = StringUtils.split(userDataAttribute,
							this.userDataValueSeparator);
					if (userDataAttributesParts != null && userDataAttributesParts.length > 0) {
						String key = userDataAttributesParts[0];

						String value = null;
						if (userDataAttributesParts.length > 1) {
							value = userDataAttributesParts[1];
						}

						userDataMap.put(key, value);
					}
				}
			}
			this.cachedUserData = Collections.unmodifiableMap(userDataMap);
		}

		return this.cachedUserData;
	}

	@Override
	public String[] getPropertyNames() {
		Set<String> propertyKeys = getUserData().keySet();
		return propertyKeys.toArray(new String[propertyKeys.size()]);
	}

}
