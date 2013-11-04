package org.elasticspring.core.formation;

import org.springframework.beans.factory.config.AbstractFactoryBean;

/**
 * Exposes a fully configured and populated {@link AmazonStackResourceRegistry} instance representing the resources of
 * the specified stack.
 *
 * @author Christian Stettler
 */
public class AmazonStackResourceRegistryFactoryBean extends AbstractFactoryBean<AmazonStackResourceRegistry> {

	@Override
	public Class<?> getObjectType() {
		return AmazonStackResourceRegistry.class;
	}

	@Override
	protected AmazonStackResourceRegistry createInstance() throws Exception {
		return new AmazonStackResourceRegistry() {

		};
	}

}
