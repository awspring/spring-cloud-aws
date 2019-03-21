/*
 * Copyright 2013-2019 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.cloud.aws.core.env;

import java.util.Collection;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.cloud.aws.core.env.stack.StackResourceRegistry;

/**
 * Provides support for resolving logical resource ids to physical resource ids for stack
 * resources exposed via {@link StackResourceRegistry} instances. This implementation
 * automatically detects the single, optional stack resource registry bean and uses it for
 * resolving logical resource ids to physical ones. If no stack resource registry bean can
 * be found in the bean factory, a pass-through stack resource registry is used instead,
 * which always returns the provided logical resource id as the physical one.
 *
 * @author Christian Stettler
 */
// TODO discuss whether to support more than one stack resource registry and how to deal
// with ordering/name conflicts
public class StackResourceRegistryDetectingResourceIdResolver
		implements ResourceIdResolver, BeanFactoryAware, InitializingBean {

	private StackResourceRegistry stackResourceRegistry;

	private ListableBeanFactory beanFactory;

	private static StackResourceRegistry findSingleOptionalStackResourceRegistry(
			ListableBeanFactory beanFactory) {
		Collection<StackResourceRegistry> stackResourceRegistries = beanFactory
				.getBeansOfType(StackResourceRegistry.class).values();

		if (stackResourceRegistries.size() > 1) {
			throw new IllegalStateException("Multiple stack resource registries found");
		}
		else if (stackResourceRegistries.size() == 1) {
			return stackResourceRegistries.iterator().next();
		}
		else {
			return null;
		}
	}

	/**
	 * Resolves the provided logical resource id to the corresponding physical resource
	 * id. If the logical resource id refers to a resource part of any of the configured
	 * stacks, the corresponding physical resource id from the stack is returned. If none
	 * of the configured stacks contain a resource with the provided logical resource id,
	 * or no stacks are configured at all, the logical resource id is returned as the
	 * physical resource id.
	 * @param logicalResourceId the logical resource id to be resolved
	 * @return the physical resource id
	 */
	@Override
	public String resolveToPhysicalResourceId(String logicalResourceId) {
		if (this.stackResourceRegistry != null) {
			String physicalResourceId = this.stackResourceRegistry
					.lookupPhysicalResourceId(logicalResourceId);

			if (physicalResourceId != null) {
				return physicalResourceId;
			}
		}

		return logicalResourceId;
	}

	@Override
	public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
		if (!(beanFactory instanceof ListableBeanFactory)) {
			throw new IllegalStateException("Bean factory must be of type '"
					+ ListableBeanFactory.class.getName() + "'");
		}

		this.beanFactory = (ListableBeanFactory) beanFactory;
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		this.stackResourceRegistry = findSingleOptionalStackResourceRegistry(
				this.beanFactory);
	}

}
