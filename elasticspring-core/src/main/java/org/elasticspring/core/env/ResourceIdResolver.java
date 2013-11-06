package org.elasticspring.core.env;

import org.elasticspring.core.env.stack.StackResourceRegistry;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.ListableBeanFactory;

import java.util.Collection;

/**
 * Provides support for resolving logical resource ids to physical resource ids.
 */
// TODO discuss whether to support more than one stack resource registry and how to deal with ordering/name conflicts
// TODO extract interface / move implementation to separate class in order to hide unwanted api
public class ResourceIdResolver implements BeanFactoryAware, InitializingBean {

	private StackResourceRegistry stackResourceRegistry;
	private ListableBeanFactory beanFactory;

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

	// TODO if no registry can be found, return pass-through registry
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
