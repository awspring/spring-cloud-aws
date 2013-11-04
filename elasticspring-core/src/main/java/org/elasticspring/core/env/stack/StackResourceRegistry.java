package org.elasticspring.core.env.stack;

/**
 * Represents a registry of logical stack resource ids mapped to physical resource ids.
 *
 * @author Christian Stettler
 */
public interface StackResourceRegistry {

	/**
	 * Returns the physical id of the resource identified by the provided logical resource id. If no resource with the
	 * provided logical id exists, null is returned.
	 *
	 * @param logicalResourceId the logical id of the resource
	 * @return the physical id of the resource, or null, if no resource for the logical id exists in this stack.
	 */
	String lookupPhysicalResourceId(String logicalResourceId);

}
