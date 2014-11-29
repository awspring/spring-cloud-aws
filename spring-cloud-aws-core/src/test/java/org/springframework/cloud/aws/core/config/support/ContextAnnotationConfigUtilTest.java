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

package org.springframework.cloud.aws.core.config.support;

import org.junit.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;
import org.springframework.core.env.MapPropertySource;

import java.util.Collections;

import static org.junit.Assert.assertEquals;

public class ContextAnnotationConfigUtilTest {

	@Test
	public void resolveStringValue_withExpression_resolvesStringValue() throws Exception {
		//Arrange
		AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
		context.getEnvironment().getPropertySources().addLast(
				new MapPropertySource("test", Collections.<String, Object>singletonMap("foo", "bar")));
		context.refresh();

		//Act
		String resolvedStringValue = ContextAnnotationConfigUtil.resolveStringValue(context.getBeanFactory(), "#{environment.foo}");

		//Assert
		assertEquals("bar", resolvedStringValue);
	}

	@Test
	public void resolveStringValue_withPlaceHolder_resolvesStringValue() throws Exception {
		//Arrange
		AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
		context.getEnvironment().getPropertySources().addLast(
				new MapPropertySource("test", Collections.<String, Object>singletonMap("foo", "baz")));
		PropertySourcesPlaceholderConfigurer configurer = new PropertySourcesPlaceholderConfigurer();
		configurer.setPropertySources(context.getEnvironment().getPropertySources());
		context.getBeanFactory().registerSingleton("configurer", configurer);

		context.refresh();

		//Act
		String resolvedStringValue = ContextAnnotationConfigUtil.resolveStringValue(context.getBeanFactory(), "${foo}");

		//Assert
		assertEquals("baz", resolvedStringValue);
	}

	@Test
	public void resolveStringValue_withExpressionThatReturnsNull_returnsNull() throws Exception {
		//Arrange
		AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
		context.getEnvironment().getPropertySources().addLast(
				new MapPropertySource("test", Collections.singletonMap("foo", null)));
		context.refresh();

		//Act
		String resolvedStringValue = ContextAnnotationConfigUtil.resolveStringValue(context.getBeanFactory(), "#{environment.foo}");

		//Assert
		assertEquals("#{environment.foo}", resolvedStringValue);
	}
}