/*
 * Copyright 2013-2014 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.cloud.aws.messaging.config.annotation;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.handler.invocation.HandlerMethodArgumentResolver;
import org.springframework.messaging.handler.invocation.HandlerMethodReturnValueHandler;
import org.springframework.util.CollectionUtils;

import java.util.List;

/**
 * @author Alain Sahli
 * @since 1.0
 */
@Configuration
public class DelegatingSqsConfiguration extends SqsConfigurationSupport {

	private final QueueMessageHandlerConfigurerComposite configurers = new QueueMessageHandlerConfigurerComposite();

	@Autowired(required = false)
	public void setConfigurers(List<QueueMessageHandlerConfigurer> configurers) {
		if (CollectionUtils.isEmpty(configurers)) {
			return;
		}
		this.configurers.addQueueMessageHandlerConfigurers(configurers);
	}

	@Override
	public void addReturnValueHandlers(List<HandlerMethodReturnValueHandler> returnValueHandlers) {
		this.configurers.addReturnValueHandlers(returnValueHandlers);
	}

	@Override
	public void addArgumentResolvers(List<HandlerMethodArgumentResolver> argumentResolvers) {
		this.configurers.addArgumentResolvers(argumentResolvers);
	}

}
