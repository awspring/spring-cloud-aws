package org.elasticspring.core.env;

import org.elasticspring.core.env.stack.StackResourceRegistry;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.ListableBeanFactory;

import java.util.Collection;

/**
 * Provides support for resolving logical resource ids to physical resource ids for stack resources exposed via {@link
 * StackResourceRegistry} instances. This implementation automatically detects the single, optional stack resource
 * registry bean and uses it for resolving logical resource ids to physical ones. If no stack resource registry bean
 * can be found in the bean factory, a pass-through stack resource registry is used instead, which always returns the
 * provided logical resource id as the physical one.
 *
 * @author Christian Stettler
 */
// TODO discuss whether to support more than one stack resource registry and how to deal with ordering/name conflicts
public class StackResourceRegistryDetectingResourceIdResolver implements ResourceIdResolver, BeanFactoryAware, InitializingBean {

	private StackResourceRegistry stackResourceRegistry;
	private ListableBeanFactory beanFactory;

	/**
	 * Resolves the provided logical resource id to the corresponding physical resource id. If the logical resource id
	 * refers to a resource part of any of the configured stacks, the corresponding physical resource id from the stack is
	 * returned. If none of the configured stacks contain a resource with the provided logical resource id, or no stacks
	 * are configured at all, the logical resource id as returned as the physical resource id.
	 *
	 * @param logicalResourceId
	 * 		the logical resource id to be resolved
	 * @return the physical resource id
	 */
	@Override
	public String resolveToPhysicalResourceId(String logicalResourceId) {
		if (this.stackResourceRegistry != null) {
			String physicalResourceId = this.stackResourceRegistry.lookupPhysicalResourceId(logicalResourceId);

			if (physicalResourceId != null) {
				return physicalResourceId;
			}
		}

		return logicalResourceId;
	}

	@Override
	public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
		if (!(beanFactory instanceof ListableBeanFactory)) {
			throw new IllegalStateException("Bean factory must be of type '" + ListableBeanFactory.class.getName() + "'");
		}

		this.beanFactory = (ListableBeanFactory) beanFactory;
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		this.stackResourceRegistry = findSingleOptionalStackResourceRegistry(this.beanFactory);
	}

	private static StackResourceRegistry findSingleOptionalStackResourceRegistry(ListableBeanFactory beanFactory) {
		Collection<StackResourceRegistry> stackResourceRegistries = beanFactory.getBeansOfType(StackResourceRegistry.class).values();

		if (stackResourceRegistries.size() > 1) {
			throw new IllegalStateException("Multiple stack resource registries found");
		} else if (stackResourceRegistries.size() == 1) {
			return stackResourceRegistries.iterator().next();
		} else {
			return null;
		}
	}

}
