package org.elasticspring.core.formation;

/**
 * Represents a registry of logical stack resource ids mapped to physical resource ids.
 *
 * @author Christian Stettler
 */
public interface AmazonStackResourceRegistry {

	String lookupPhysicalResourceId(String logicalResourceId);

}
