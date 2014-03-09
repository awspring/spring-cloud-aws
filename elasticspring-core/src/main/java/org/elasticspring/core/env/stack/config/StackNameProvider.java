package org.elasticspring.core.env.stack.config;

/**
 * Represents a strategy for resolving and providing the name of an amazon cloud formation stack.
 *
 * @author Christian Stettler
 */
interface StackNameProvider {

	/**
	 * Returns the name of the current stack.
	 *
	 * @return the name of the current stack.
	 */
	String getStackName();

}
