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

package io.awspring.cloud.v3.messaging.endpoint.config;

import org.springframework.beans.factory.config.AbstractFactoryBean;
import org.springframework.util.Assert;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import software.amazon.awssdk.services.sns.SnsClient;

import static io.awspring.cloud.v3.messaging.endpoint.config.NotificationHandlerMethodArgumentResolverConfigurationUtils.getNotificationHandlerMethodArgumentResolver;

/**
 * @author Agim Emruli
 * @author Alain Sahli
 */
public class NotificationHandlerMethodArgumentResolverFactoryBean
		extends AbstractFactoryBean<HandlerMethodArgumentResolver> {

	private final SnsClient amazonSns;

	public NotificationHandlerMethodArgumentResolverFactoryBean(SnsClient amazonSns) {
		Assert.notNull(amazonSns, "AmazonSns must not be null");
		this.amazonSns = amazonSns;
	}

	@Override
	public Class<HandlerMethodArgumentResolver> getObjectType() {
		return HandlerMethodArgumentResolver.class;
	}

	@Override
	protected HandlerMethodArgumentResolver createInstance() throws Exception {
		return getNotificationHandlerMethodArgumentResolver(this.amazonSns);
	}

}
