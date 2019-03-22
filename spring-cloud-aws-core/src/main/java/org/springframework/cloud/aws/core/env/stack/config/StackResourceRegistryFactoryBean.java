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

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.amazonaws.services.cloudformation.AmazonCloudFormation;
import com.amazonaws.services.cloudformation.model.ListStackResourcesRequest;
import com.amazonaws.services.cloudformation.model.ListStackResourcesResult;
import com.amazonaws.services.cloudformation.model.StackResourceSummary;

import org.springframework.beans.factory.config.AbstractFactoryBean;
import org.springframework.cloud.aws.core.env.stack.ListableStackResourceFactory;
import org.springframework.cloud.aws.core.env.stack.StackResource;
import org.springframework.cloud.aws.core.support.documentation.RuntimeUse;
import org.springframework.util.StringUtils;

/**
 * Exposes a fully populated
 * {@link org.springframework.cloud.aws.core.env.stack.StackResourceRegistry} instance
 * representing the resources of the specified stack.
 *
 * @author Christian Stettler
 * @author Agim Emruli
 */
@RuntimeUse
public class StackResourceRegistryFactoryBean
		extends AbstractFactoryBean<ListableStackResourceFactory> {

	private final AmazonCloudFormation amazonCloudFormationClient;

	private final StackNameProvider stackNameProvider;

	public StackResourceRegistryFactoryBean(
			AmazonCloudFormation amazonCloudFormationClient,
			StackNameProvider stackNameProvider) {
		this.amazonCloudFormationClient = amazonCloudFormationClient;
		this.stackNameProvider = stackNameProvider;
	}

	@Override
	public Class<?> getObjectType() {
		return ListableStackResourceFactory.class;
	}

	@Override
	protected ListableStackResourceFactory createInstance() {
		String stackName = this.stackNameProvider.getStackName();
		return new StaticStackResourceRegistry(stackName,
				getResourceMappings("", stackName));
	}

	private Map<String, StackResource> getResourceMappings(String prefix,
			String stackName) {

		List<StackResourceSummary> stackResourceSummaries = getStackResourceSummaries(
				stackName);

		Map<String, StackResource> current = convertToStackResourceMappings(prefix,
				stackResourceSummaries);
		Map<String, StackResource> stackResourceMappings = new HashMap<>(current);

		for (Map.Entry<String, StackResource> e : current.entrySet()) {
			StackResource resource = e.getValue();

			if ("AWS::CloudFormation::Stack".equals(resource.getType())) {
				stackResourceMappings.putAll(
						getResourceMappings(e.getKey(), resource.getPhysicalId()));
			}
		}

		return stackResourceMappings;
	}

	private List<StackResourceSummary> getStackResourceSummaries(String stackName) {
		ListStackResourcesResult listStackResourcesResult = this.amazonCloudFormationClient
				.listStackResources(
						new ListStackResourcesRequest().withStackName(stackName));
		if (!StringUtils.hasText(listStackResourcesResult.getNextToken())) {
			return listStackResourcesResult.getStackResourceSummaries();
		}
		else {
			List<StackResourceSummary> result = new ArrayList<>(
					listStackResourcesResult.getStackResourceSummaries());
			while (StringUtils.hasText(listStackResourcesResult.getNextToken())) {
				listStackResourcesResult = this.amazonCloudFormationClient
						.listStackResources(new ListStackResourcesRequest()
								.withStackName(stackName)
								.withNextToken(listStackResourcesResult.getNextToken()));
				result.addAll(listStackResourcesResult.getStackResourceSummaries());
			}
			return result;
		}
	}

	private Map<String, StackResource> convertToStackResourceMappings(String prefix,
			List<StackResourceSummary> stackResourceSummaries) {
		Map<String, StackResource> stackResourceMappings = new HashMap<>();

		for (StackResourceSummary stackResourceSummary : stackResourceSummaries) {
			String logicalResourceId = toNestedResourceId(prefix,
					stackResourceSummary.getLogicalResourceId());
			stackResourceMappings.put(logicalResourceId,
					new StackResource(logicalResourceId,
							stackResourceSummary.getPhysicalResourceId(),
							stackResourceSummary.getResourceType()));
		}

		return stackResourceMappings;
	}

	private String toNestedResourceId(String prefix, String logicalResourceId) {
		return StringUtils.isEmpty(prefix) ? logicalResourceId
				: prefix + "." + logicalResourceId;
	}

	/**
	 * Stack resource registry containing a static mapping of logical resource ids to
	 * physical resource ids.
	 */
	private static final class StaticStackResourceRegistry
			implements ListableStackResourceFactory {

		private final String stackName;

		private final Map<String, StackResource> stackResourceByLogicalId;

		private StaticStackResourceRegistry(String stackName,
				Map<String, StackResource> stackResourceByLogicalId) {
			this.stackName = stackName;
			this.stackResourceByLogicalId = stackResourceByLogicalId;
		}

		@Override
		public String getStackName() {
			return this.stackName;
		}

		@Override
		public String lookupPhysicalResourceId(String logicalResourceId) {
			if (this.stackResourceByLogicalId.containsKey(logicalResourceId)) {
				return this.stackResourceByLogicalId.get(logicalResourceId)
						.getPhysicalId();
			}
			else if (!logicalResourceId.contains(".")) {
				String prefix = "." + logicalResourceId;

				String physicalId = null;
				for (Map.Entry<String, StackResource> entry : this.stackResourceByLogicalId
						.entrySet()) {
					if (entry.getKey() != null && entry.getKey().endsWith(prefix)) {
						if (physicalId == null) {
							physicalId = entry.getValue().getPhysicalId();
						}
						else {
							// unqualified resourceId is not unique
							return null;
						}
					}
				}
				return physicalId;
			}
			else {
				return null;
			}
		}

		@Override
		public Collection<StackResource> getAllResources() {
			return this.stackResourceByLogicalId.values();
		}

		@Override
		public Collection<StackResource> resourcesByType(String type) {
			List<StackResource> result = new ArrayList<>();
			for (StackResource stackResource : this.stackResourceByLogicalId.values()) {
				if (stackResource.getType().equals(type)) {
					result.add(stackResource);
				}
			}
			return result;
		}

	}

}
