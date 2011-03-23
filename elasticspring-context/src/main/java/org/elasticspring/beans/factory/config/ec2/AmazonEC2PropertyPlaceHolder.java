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

package org.elasticspring.beans.factory.config.ec2;

import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.ec2.AmazonEC2;
import com.amazonaws.services.ec2.AmazonEC2Client;
import com.amazonaws.services.ec2.model.DescribeInstanceAttributeRequest;
import com.amazonaws.services.ec2.model.DescribeInstanceAttributeResult;
import com.amazonaws.services.ec2.model.DescribeInstancesRequest;
import com.amazonaws.services.ec2.model.DescribeInstancesResult;
import com.amazonaws.services.ec2.model.Instance;
import com.amazonaws.services.ec2.model.Reservation;
import com.amazonaws.services.ec2.model.Tag;
import org.apache.commons.codec.binary.Base64;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.config.PropertyPlaceholderConfigurer;
import org.springframework.util.StringUtils;

import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

/**
 *
 */
public class AmazonEC2PropertyPlaceHolder extends PropertyPlaceholderConfigurer implements InitializingBean {

	public static final String USER_DATA_ATTRIBUTE_NAME = "userData";
	public static final String USER_DATA_ATTRIBUTE_SEPARATOR = ";";
	public static final Charset USER_DATA_ATTRIBUTE_ENCODING = Charset.forName("UTF-8");

	private final AmazonEC2 amazonEC2;
	private final InstanceIdProvider instanceIdProvider;
	private boolean resolveUserDataForInstance = true;
	private boolean resolveUserTagsForInstance = true;
	private String valueSeparator = DEFAULT_VALUE_SEPARATOR;
	private Charset userDataAttributeEncoding = USER_DATA_ATTRIBUTE_ENCODING;
	private String userDataAttributeSeparator = USER_DATA_ATTRIBUTE_SEPARATOR;

	private Map<String, String> instanceUserAttributes;
	private Map<String, String> instanceUserTags;


	public AmazonEC2PropertyPlaceHolder(String accessKey, String secretKey, InstanceIdProvider instanceIdProvider) {
		this.amazonEC2 = new AmazonEC2Client(new BasicAWSCredentials(accessKey, secretKey));
		this.instanceIdProvider = instanceIdProvider;
	}

	public void setResolveUserDataForInstance(boolean resolveUserDataForInstance) {
		this.resolveUserDataForInstance = resolveUserDataForInstance;
	}

	public void setResolveUserTagsForInstance(boolean resolveUserTagsForInstance) {
		this.resolveUserTagsForInstance = resolveUserTagsForInstance;
	}

	@Override
	public void setValueSeparator(String valueSeparator) {
		super.setValueSeparator(valueSeparator);
		this.valueSeparator = valueSeparator;
	}

	public void setUserDataAttributeEncoding(String userDataAttributeEncoding) {
		this.userDataAttributeEncoding = Charset.forName(userDataAttributeEncoding);
	}

	public void setUserDataAttributeSeparator(String userDataAttributeSeparator) {
		this.userDataAttributeSeparator = userDataAttributeSeparator;
	}

	public void afterPropertiesSet() throws Exception {
		String currentInstanceId = this.instanceIdProvider.getCurrentInstanceId();


		if (this.resolveUserDataForInstance) {
			this.instanceUserAttributes = new HashMap<String, String>();
			DescribeInstanceAttributeResult attributes = this.getAmazonEC2().describeInstanceAttribute(new DescribeInstanceAttributeRequest(currentInstanceId, USER_DATA_ATTRIBUTE_NAME));
			String encodedUserData = attributes.getInstanceAttribute().getUserData();
			if (StringUtils.hasText(encodedUserData)) {
				byte[] bytes = Base64.decodeBase64(encodedUserData);
				String userData = new String(bytes, this.userDataAttributeEncoding);
				String[] userDataAttributes = userData.split(this.userDataAttributeSeparator);
				for (String userDataAttribute : userDataAttributes) {
					String[] userDataAttributesParts = StringUtils.split(userDataAttribute, this.valueSeparator);
					String key = userDataAttributesParts[0];
					String value = userDataAttributesParts[1];
					instanceUserAttributes.put(key, value);
				}
			}
		}

		if (this.resolveUserTagsForInstance) {
			this.instanceUserTags = new HashMap<String, String>();
			DescribeInstancesResult describeInstancesResult = this.getAmazonEC2().describeInstances(new DescribeInstancesRequest().withInstanceIds(currentInstanceId));
			for (Reservation reservation : describeInstancesResult.getReservations()) {
				for (Instance instance : reservation.getInstances()) {
					if (instance.getInstanceId().equals(currentInstanceId)) {
						for (Tag tag : instance.getTags()) {
							this.instanceUserTags.put(tag.getKey(), tag.getValue());
						}
						break;
					}
				}
			}
		}
	}

	@Override
	protected String resolvePlaceholder(String placeholder, Properties props) {

		String result = null;

		if (this.resolveUserDataForInstance) {
			result = instanceUserAttributes.get(placeholder);
		}

		if (result == null && this.resolveUserTagsForInstance) {
			result = instanceUserTags.get(placeholder);
		}

		if (result == null) {
			result = super.resolvePlaceholder(placeholder, props);
		}
		return result;
	}

	protected AmazonEC2 getAmazonEC2() {
		return this.amazonEC2;
	}
}
