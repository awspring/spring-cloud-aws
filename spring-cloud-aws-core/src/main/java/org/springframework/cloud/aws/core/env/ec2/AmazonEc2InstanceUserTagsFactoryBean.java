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

package org.springframework.cloud.aws.core.env.ec2;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import com.amazonaws.services.ec2.AmazonEC2;
import com.amazonaws.services.ec2.model.DescribeTagsRequest;
import com.amazonaws.services.ec2.model.DescribeTagsResult;
import com.amazonaws.services.ec2.model.Filter;
import com.amazonaws.services.ec2.model.TagDescription;

import org.springframework.beans.factory.config.AbstractFactoryBean;
import org.springframework.cloud.aws.core.support.documentation.RuntimeUse;

/**
 * @author Agim Emruli
 */
public class AmazonEc2InstanceUserTagsFactoryBean
		extends AbstractFactoryBean<Map<String, String>> {

	private final AmazonEC2 amazonEc2;

	private final InstanceIdProvider idProvider;

	@RuntimeUse
	public AmazonEc2InstanceUserTagsFactoryBean(AmazonEC2 amazonEc2) {
		this(amazonEc2, new AmazonEc2InstanceIdProvider());
	}

	public AmazonEc2InstanceUserTagsFactoryBean(AmazonEC2 amazonEc2,
			InstanceIdProvider idProvider) {
		this.amazonEc2 = amazonEc2;
		this.idProvider = idProvider;
	}

	@Override
	public Class<?> getObjectType() {
		return Map.class;
	}

	@Override
	protected Map<String, String> createInstance() throws Exception {
		LinkedHashMap<String, String> properties = new LinkedHashMap<>();
		DescribeTagsResult tags = this.amazonEc2
				.describeTags(
						new DescribeTagsRequest()
								.withFilters(
										new Filter("resource-id",
												Collections.singletonList(this.idProvider
														.getCurrentInstanceId())),
										new Filter("resource-type",
												Collections.singletonList("instance"))));
		for (TagDescription tag : tags.getTags()) {
			properties.put(tag.getKey(), tag.getValue());
		}
		return properties;
	}

}
