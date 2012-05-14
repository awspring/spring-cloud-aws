/*
 * Copyright [2011] [Agim Emruli]
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.elasticspring.core.env.ec2;

import com.amazonaws.services.ec2.AmazonEC2;
import com.amazonaws.services.ec2.model.DescribeInstanceAttributeRequest;
import com.amazonaws.services.ec2.model.DescribeInstanceAttributeResult;
import org.apache.commons.codec.binary.Base64;
import org.springframework.beans.factory.config.PlaceholderConfigurerSupport;
import org.springframework.util.StringUtils;

import java.nio.charset.Charset;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 *
 */
public class AmazonEC2UserDataPropertySource extends AbstractAmazonEC2PropertySource {

	public static final String USER_DATA_ATTRIBUTE_NAME = "userData";
	public static final String USER_DATA_ATTRIBUTE_SEPARATOR = ";";
	public static final Charset USER_DATA_ATTRIBUTE_ENCODING = Charset.forName("UTF-8");
	private Charset userDataAttributeEncoding = USER_DATA_ATTRIBUTE_ENCODING;
	private String userDataAttributeSeparator = USER_DATA_ATTRIBUTE_SEPARATOR;
	private String userDataValueSeparator = PlaceholderConfigurerSupport.DEFAULT_VALUE_SEPARATOR;

	public AmazonEC2UserDataPropertySource(String name, AmazonEC2 amazonEC2) {
		super(name, amazonEC2);
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
	protected Map<String, String> createValueMap(String instanceId) {
		LinkedHashMap<String, String> result = new LinkedHashMap<String, String>();
		DescribeInstanceAttributeResult attributes = this.source.describeInstanceAttribute(new DescribeInstanceAttributeRequest(instanceId, USER_DATA_ATTRIBUTE_NAME));
		if (attributes != null) {
			String encodedUserData = attributes.getInstanceAttribute().getUserData();
			if (StringUtils.hasText(encodedUserData)) {
				byte[] bytes = Base64.decodeBase64(encodedUserData);
				String userData = new String(bytes, this.userDataAttributeEncoding);
				String[] userDataAttributes = userData.split(this.userDataAttributeSeparator);
				for (String userDataAttribute : userDataAttributes) {
					String[] userDataAttributesParts = StringUtils.split(userDataAttribute, this.userDataValueSeparator);
					String key = userDataAttributesParts[0];
					String value = userDataAttributesParts[1];
					result.put(key, value);
				}
			}
		}
		return result;
	}
}
