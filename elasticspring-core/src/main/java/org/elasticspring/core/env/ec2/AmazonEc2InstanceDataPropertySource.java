/*
 * Copyright 2013 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.elasticspring.core.env.ec2;

import com.amazonaws.util.EC2MetadataUtils;
import org.apache.commons.codec.binary.Base64;
import org.springframework.beans.factory.config.PlaceholderConfigurerSupport;
import org.springframework.core.env.PropertySource;
import org.springframework.util.StringUtils;

import java.nio.charset.Charset;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * @author Agim Emruli
 */
public class AmazonEc2InstanceDataPropertySource extends PropertySource<Object> {

	private static final String EC2_METADATA_ROOT = "/latest/meta-data";

	private static final String DEFAULT_USER_DATA_ATTRIBUTE_SEPARATOR = ";";
	private static final Charset DEFAULT_USER_DATA_ATTRIBUTE_ENCODING = Charset.forName("UTF-8");

	private String userDataAttributeSeparator = DEFAULT_USER_DATA_ATTRIBUTE_SEPARATOR;
	private Charset userDataAttributeEncoding = DEFAULT_USER_DATA_ATTRIBUTE_ENCODING;
	private String userDataValueSeparator = PlaceholderConfigurerSupport.DEFAULT_VALUE_SEPARATOR;

	private volatile Map<String, String> cachedUserData;

	public AmazonEc2InstanceDataPropertySource(String name) {
		super(name, new Object());
	}

	public void setUserDataAttributeEncoding(String userDataAttributeEncoding) {
		this.userDataAttributeEncoding = Charset.forName(userDataAttributeEncoding);
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
		return EC2MetadataUtils.getData(EC2_METADATA_ROOT + "/" + name);
	}

	private Map<String, String> getUserData() {
		if (this.cachedUserData == null) {
 			Map<String,String> userDataMap = new LinkedHashMap<String, String>();
			String encodedUserData = EC2MetadataUtils.getUserData();
			if (StringUtils.hasText(encodedUserData)) {
				byte[] bytes = Base64.decodeBase64(encodedUserData);
				String userData = new String(bytes, this.userDataAttributeEncoding);
				String[] userDataAttributes = userData.split(this.userDataAttributeSeparator);
				for (String userDataAttribute : userDataAttributes) {
					String[] userDataAttributesParts = StringUtils.split(userDataAttribute, this.userDataValueSeparator);
					String key = userDataAttributesParts[0];
					String value = userDataAttributesParts[1];
					userDataMap.put(key, value);
				}
			}
			this.cachedUserData = Collections.unmodifiableMap(userDataMap);
		}

		return this.cachedUserData;
	}
}