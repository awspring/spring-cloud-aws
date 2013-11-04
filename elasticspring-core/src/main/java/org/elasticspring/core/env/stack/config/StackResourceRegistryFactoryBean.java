package org.elasticspring.core.env.stack.config;

import com.amazonaws.services.cloudformation.AmazonCloudFormationClient;
import com.amazonaws.services.cloudformation.model.ListStackResourcesRequest;
import com.amazonaws.services.cloudformation.model.ListStackResourcesResult;
import com.amazonaws.services.cloudformation.model.StackResourceSummary;
import org.elasticspring.core.env.stack.StackResourceRegistry;
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

		return new StaticStackResourceRegistry(convertToStackResourceMappings(stackResourceSummaries));
	}

	private static Map<String, String> convertToStackResourceMappings(List<StackResourceSummary> stackResourceSummaries) {
		Map<String, String> stackResourceMappings = new HashMap<String, String>();

		for (StackResourceSummary stackResourceSummary : stackResourceSummaries) {
			stackResourceMappings.put(stackResourceSummary.getLogicalResourceId(), stackResourceSummary.getPhysicalResourceId());
		}

		return stackResourceMappings;
	}


	private static class StaticStackResourceRegistry implements StackResourceRegistry {

		private final Map<String, String> physicalResourceIdsByLogicalResourceId;

		private StaticStackResourceRegistry(Map<String, String> physicalResourceIdsByLogicalResourceId) {
			this.physicalResourceIdsByLogicalResourceId = physicalResourceIdsByLogicalResourceId;
		}

		@Override
		public String lookupPhysicalResourceId(String logicalResourceId) {
			return this.physicalResourceIdsByLogicalResourceId.get(logicalResourceId);
		}

	}

}
