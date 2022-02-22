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

package io.awspring.cloud.messaging.endpoint.config;

import com.amazonaws.services.sns.AmazonSNS;
import com.amazonaws.services.sns.message.SnsMessageManager;
import org.junit.jupiter.api.Test;

import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.HandlerMethodArgumentResolverComposite;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

class NotificationHandlerMethodArgumentResolverFactoryBeanTest {

	@Test
	void getObjectType_defaultConfiguration_returnsHandlerMethodArgumentResolverType() throws Exception {
		// Arrange
		AmazonSNS amazonSns = mock(AmazonSNS.class);
		SnsMessageManager snsMessageManager = mock(SnsMessageManager.class);
		NotificationHandlerMethodArgumentResolverFactoryBean factoryBean;
		factoryBean = new NotificationHandlerMethodArgumentResolverFactoryBean(amazonSns, snsMessageManager);

		// Act
		Class<HandlerMethodArgumentResolver> type = factoryBean.getObjectType();

		// Assert
		assertThat(type).isSameAs(HandlerMethodArgumentResolver.class);
	}

	@Test
	void getObject_withDefaultConfiguration_createCompositeResolverWithAllDelegatedResolvers() throws Exception {
		// Arrange
		AmazonSNS amazonSns = mock(AmazonSNS.class);
		SnsMessageManager snsMessageManager = mock(SnsMessageManager.class);
		NotificationHandlerMethodArgumentResolverFactoryBean factoryBean;
		factoryBean = new NotificationHandlerMethodArgumentResolverFactoryBean(amazonSns, snsMessageManager);
		factoryBean.afterPropertiesSet();

		// Act
		HandlerMethodArgumentResolver argumentResolver = factoryBean.getObject();

		// Assert
		assertThat(argumentResolver).isNotNull();
		assertThat(((HandlerMethodArgumentResolverComposite) argumentResolver).getResolvers().size()).isEqualTo(3);
	}

	@Test
	void createInstance_withNullSnsClient_reportsError() throws Exception {
		// Assert
		assertThatThrownBy(() -> new NotificationHandlerMethodArgumentResolverFactoryBean(null, null))
				.isInstanceOf(IllegalArgumentException.class).hasMessageContaining("not be null");

	}

}
