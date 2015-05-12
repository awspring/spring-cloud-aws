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

package org.springframework.cloud.aws.context.support.io;

import org.junit.Assert;
import org.junit.Test;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ApplicationObjectSupport;
import org.springframework.core.io.ResourceLoader;

import static org.mockito.Mockito.mock;

/**
 * @author Agim Emruli
 */
public class ResourceLoaderBeanPostProcessorTest {

	@Test
	public void postProcessBeforeInitialization_withApplicationObjectBean_doesNotSetApplicationContext() {

		ResourceLoader resourceLoader = mock(ResourceLoader.class);
		ResourceLoaderBeanPostProcessor processor = new ResourceLoaderBeanPostProcessor(resourceLoader);

		TestApplicationObjectSupport testApplicationObjectSupport = new TestApplicationObjectSupport();
		ApplicationContext mock = mock(ApplicationContext.class);
		testApplicationObjectSupport.setApplicationContext(mock);

		processor.postProcessAfterInitialization(new TestApplicationObjectSupport(), "test");

		Assert.assertSame(mock, testApplicationObjectSupport.getApplicationContext());

	}

	private static class TestApplicationObjectSupport extends ApplicationObjectSupport {

		@Override
		protected boolean isContextRequired() {
			return true;
		}
	}
}