package org.elasticspring.core.env.stack.config;

import org.elasticspring.core.support.documentation.RuntimeUse;

/**
 * Represents a provider for a statically configured stack name.
 *
 * @author Christian Stettler
 */
@RuntimeUse
class StaticStackNameProvider implements StackNameProvider {

	private final String stackName;

	StaticStackNameProvider(String stackName) {
		this.stackName = stackName;
	}

	@Override
	public String getStackName() {
		return this.stackName;
	}

}
