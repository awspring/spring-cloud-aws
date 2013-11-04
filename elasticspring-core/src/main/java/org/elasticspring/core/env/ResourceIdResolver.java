package org.elasticspring.core.env;

import org.elasticspring.core.env.stack.StackResourceRegistry;

/**
 * Provides support for resolving logical resource ids to physical resource ids.
 */
public class ResourceIdResolver {

	private final StackResourceRegistry stackResourceRegistry;

	public ResourceIdResolver(StackResourceRegistry stackResourceRegistry) {
		this.stackResourceRegistry = stackResourceRegistry;
	}

	/**
	 * Resolves the provided logical resource id to the corresponding physical resource id. If the logical resource id
	 * refers to a resource part of any of the configured stacks, the corresponding physical resource id from the stack is
	 * returned. If none of the configured stacks contain a resource with the provided logical resource id, or no stacks
	 * are configured at all, the logical resource id as returned as the physical resource id.
	 * <p/>
	 * This resolving mechanism provides no guarantees on existence of the resource denoted by the resolved physical
	 * resource id.
	 *
	 * @param logicalResourceId
	 * 		the logical resource id to be resolved
	 * @return the physical resource id
	 */
	public String resolveToPhysicalResourceId(String logicalResourceId) {
		if (this.stackResourceRegistry != null) {
			String physicalResourceId = this.stackResourceRegistry.lookupPhysicalResourceId(logicalResourceId);

			if (physicalResourceId != null) {
				return physicalResourceId;
			}
		}

		return logicalResourceId;
	}

}
