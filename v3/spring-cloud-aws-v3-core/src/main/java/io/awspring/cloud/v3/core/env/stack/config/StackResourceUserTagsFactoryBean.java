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

package io.awspring.cloud.v3.core.env.stack.config;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.beans.factory.config.AbstractFactoryBean;
import software.amazon.awssdk.services.cloudformation.CloudFormationClient;
import software.amazon.awssdk.services.cloudformation.model.DescribeStacksRequest;
import software.amazon.awssdk.services.cloudformation.model.DescribeStacksResponse;
import software.amazon.awssdk.services.cloudformation.model.Tag;

/**
 * @author Agim Emruli
 */
public class StackResourceUserTagsFactoryBean extends AbstractFactoryBean<Map<String, String>> {

	private final CloudFormationClient amazonCloudFormation;

	private final StackNameProvider stackNameProvider;

	public StackResourceUserTagsFactoryBean(CloudFormationClient amazonCloudFormation,
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
		DescribeStacksResponse stacksResult = this.amazonCloudFormation
				.describeStacks(DescribeStacksRequest.builder()
					.stackName(this.stackNameProvider.getStackName())
					.build());

		return stacksResult
			.stacks().stream()
			.flatMap(stack -> stack.tags().stream())
			.collect(Collectors.toMap(Tag::key, Tag::value, (o1, o2) -> o1, LinkedHashMap::new));
	}

}
