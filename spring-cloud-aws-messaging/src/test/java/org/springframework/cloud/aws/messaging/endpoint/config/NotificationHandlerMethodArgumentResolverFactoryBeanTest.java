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

package org.springframework.cloud.aws.messaging.endpoint.config;

import com.amazonaws.services.sns.AmazonSNS;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.HandlerMethodArgumentResolverComposite;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.mockito.Mockito.mock;

public class NotificationHandlerMethodArgumentResolverFactoryBeanTest {

	@Rule
	public ExpectedException expectedException = ExpectedException.none();

	@Test
	public void getObjectType_defaultConfiguration_returnsHandlerMethodArgumentResolverType() throws Exception {
		//Arrange
		AmazonSNS amazonSns = mock(AmazonSNS.class);
		NotificationHandlerMethodArgumentResolverFactoryBean factoryBean = new NotificationHandlerMethodArgumentResolverFactoryBean(amazonSns);

		//Act
		Class<HandlerMethodArgumentResolver> type = factoryBean.getObjectType();

		//Assert
		assertSame(HandlerMethodArgumentResolver.class, type);
	}

	@Test
	public void getObject_withDefaultConfiguration_createCompositeResolverWithAllDelegatedResolvers() throws Exception {
		//Arrange
		AmazonSNS amazonSns = mock(AmazonSNS.class);
		NotificationHandlerMethodArgumentResolverFactoryBean factoryBean = new NotificationHandlerMethodArgumentResolverFactoryBean(amazonSns);
		factoryBean.afterPropertiesSet();

		//Act
		HandlerMethodArgumentResolver argumentResolver = factoryBean.getObject();

		//Assert
		assertNotNull(argumentResolver);
		assertEquals(3, ((HandlerMethodArgumentResolverComposite) argumentResolver).getResolvers().size());
	}

	@Test
	public void createInstance_withNullSnsClient_reportsError() throws Exception {
		//Arrange
		this.expectedException.expect(IllegalArgumentException.class);
		this.expectedException.expectMessage("not be null");

		//Act
		//noinspection ResultOfObjectAllocationIgnored
		new NotificationHandlerMethodArgumentResolverFactoryBean(null);

		//Assert
	}
}