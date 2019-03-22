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

package org.springframework.cloud.aws.core.env.stack.config;

import java.util.LinkedHashMap;
import java.util.Map;

import com.amazonaws.services.cloudformation.AmazonCloudFormation;
import com.amazonaws.services.cloudformation.model.DescribeStacksRequest;
import com.amazonaws.services.cloudformation.model.DescribeStacksResult;
import com.amazonaws.services.cloudformation.model.Stack;
import com.amazonaws.services.cloudformation.model.Tag;

import org.springframework.beans.factory.config.AbstractFactoryBean;

/**
 * @author Agim Emruli
 */
public class StackResourceUserTagsFactoryBean
		extends AbstractFactoryBean<Map<String, String>> {

	private final AmazonCloudFormation amazonCloudFormation;

	private final StackNameProvider stackNameProvider;

	public StackResourceUserTagsFactoryBean(AmazonCloudFormation amazonCloudFormation,
			StackNameProvider stackNameProvider) {
		this.amazonCloudFormation = amazonCloudFormation;
		this.stackNameProvider = stackNameProvider;
	}

	@Override
	public Class<?> getObjectType() {
		return Map.class;
	}

	@Override
	protected Map<String, String> createInstance() throws Exception {
		LinkedHashMap<String, String> userTags = new LinkedHashMap<>();
		DescribeStacksResult stacksResult = this.amazonCloudFormation
				.describeStacks(new DescribeStacksRequest()
						.withStackName(this.stackNameProvider.getStackName()));
		for (Stack stack : stacksResult.getStacks()) {
			for (Tag tag : stack.getTags()) {
				userTags.put(tag.getKey(), tag.getValue());
			}
		}
		return userTags;
	}

}
