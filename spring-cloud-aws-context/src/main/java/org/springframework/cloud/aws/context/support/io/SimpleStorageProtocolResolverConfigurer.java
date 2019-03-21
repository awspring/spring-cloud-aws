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

package org.springframework.cloud.aws.context.support.io;

import org.springframework.beans.BeansException;
import org.springframework.cloud.aws.core.io.s3.SimpleStorageProtocolResolver;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.Ordered;
import org.springframework.core.io.ProtocolResolver;

/**
 * Configurer that register the {@link SimpleStorageProtocolResolver} to the resource
 * resolver to allow resolving s3 based resources in Spring.
 *
 * @author Agim Emruli
 * @author Alain Sahli
 * @since 1.0
 */
public class SimpleStorageProtocolResolverConfigurer
		implements ApplicationContextAware, Ordered {

	private final ProtocolResolver resourceLoader;

	public SimpleStorageProtocolResolverConfigurer(
			SimpleStorageProtocolResolver simpleStorageProtocolResolver) {
		this.resourceLoader = simpleStorageProtocolResolver;
	}

	@Override
	public int getOrder() {
		return Ordered.HIGHEST_PRECEDENCE;
	}

	@Override
	public void setApplicationContext(ApplicationContext applicationContext)
			throws BeansException {
		if (applicationContext instanceof ConfigurableApplicationContext) {
			ConfigurableApplicationContext configurableApplicationContext = (ConfigurableApplicationContext) applicationContext;
			configurableApplicationContext.addProtocolResolver(this.resourceLoader);
		}
	}

}
