package org.elasticspring.core.formation.support;

import org.elasticspring.core.formation.StackNameProvider;

/**
 * Represents a provider for a statically configured stack name.
 *
 * @author Christian Stettler
 */
public class StaticStackNameProvider implements StackNameProvider {

	private final String stackName;

	public StaticStackNameProvider(String stackName) {
		this.stackName = stackName;
	}

	@Override
	public String getStackName() {
		return this.stackName;
	}

}
