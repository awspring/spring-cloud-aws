package org.elasticspring.core.formation;

/**
 * Represents a strategy for resolving and providing the name of an amazon cloud formation stack.
 *
 * @author Christian Stettler
 */
public interface StackNameProvider {

	/**
	 * Returns the name of the current stack.
	 *
	 * @return the name of the current stack.
	 */
	String getStackName();

}
