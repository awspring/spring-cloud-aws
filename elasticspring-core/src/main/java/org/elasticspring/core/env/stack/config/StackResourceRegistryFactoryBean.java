/*
 * Copyright 2013-2014 the original author or authors.
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

package org.elasticspring.core.env.stack.config;

import com.amazonaws.services.cloudformation.AmazonCloudFormationClient;
import com.amazonaws.services.cloudformation.model.ListStackResourcesRequest;
import com.amazonaws.services.cloudformation.model.ListStackResourcesResult;
import com.amazonaws.services.cloudformation.model.StackResourceSummary;
import org.elasticspring.core.env.stack.StackResourceRegistry;
import org.elasticspring.core.support.documentation.RuntimeUse;
import org.springframework.beans.factory.config.AbstractFactoryBean;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Exposes a fully populated {@link org.elasticspring.core.env.stack.StackResourceRegistry} instance representing the resources of
 * the specified stack.
 *
 * @author Christian Stettler
 */
@RuntimeUse
class StackResourceRegistryFactoryBean extends AbstractFactoryBean<StackResourceRegistry> {

	private final AmazonCloudFormationClient amazonCloudFormationClient;
	private final StackNameProvider stackNameProvider;

	StackResourceRegistryFactoryBean(AmazonCloudFormationClient amazonCloudFormationClient, StackNameProvider stackNameProvider) {
		this.amazonCloudFormationClient = amazonCloudFormationClient;
		this.stackNameProvider = stackNameProvider;
	}

	@Override
	public Class<?> getObjectType() {
		return StackResourceRegistry.class;
	}

	@Override
	protected StackResourceRegistry createInstance() throws Exception {
		String stackName = this.stackNameProvider.getStackName();
		ListStackResourcesResult listStackResourcesResult = this.amazonCloudFormationClient.listStackResources(new ListStackResourcesRequest().withStackName(stackName));
		List<StackResourceSummary> stackResourceSummaries = listStackResourcesResult.getStackResourceSummaries();

		return new StaticStackResourceRegistry(stackName, convertToStackResourceMappings(stackResourceSummaries));
	}

	private static Map<String,String> convertToStackResourceMappings(List<StackResourceSummary> stackResourceSummaries) {
		Map<String,String> stackResourceMappings = new HashMap<String,String>();

		for (StackResourceSummary stackResourceSummary : stackResourceSummaries) {
			stackResourceMappings.put(stackResourceSummary.getLogicalResourceId(), stackResourceSummary.getPhysicalResourceId());
		}

		return stackResourceMappings;
	}


	/**
	 * Stack resource registry containing a static mapping of logical resource ids to physical resource ids.
	 */
	private static class StaticStackResourceRegistry implements StackResourceRegistry {

		private final String stackName;
		private final Map<String,String> physicalResourceIdsByLogicalResourceId;

		private StaticStackResourceRegistry(String stackName, Map<String,String> physicalResourceIdsByLogicalResourceId) {
			this.stackName = stackName;
			this.physicalResourceIdsByLogicalResourceId = physicalResourceIdsByLogicalResourceId;
		}

		@Override
		public String getStackName() {
			return this.stackName;
		}

		@Override
		public String lookupPhysicalResourceId(String logicalResourceId) {
			return this.physicalResourceIdsByLogicalResourceId.get(logicalResourceId);
		}

	}

}
