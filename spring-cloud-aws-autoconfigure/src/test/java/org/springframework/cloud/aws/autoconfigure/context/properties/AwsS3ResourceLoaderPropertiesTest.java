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
package org.springframework.cloud.aws.autoconfigure.context.properties;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

/**
 * Tests for {@link AwsS3ResourceLoaderProperties}.
 *
 * @author tgianos
 * @since 2.0.2
 */
public class AwsS3ResourceLoaderPropertiesTest {

	private AwsS3ResourceLoaderProperties properties;

	@Before
	public void setup() {
		this.properties = new AwsS3ResourceLoaderProperties();
	}

	@Test
	public void corePoolSizeCanBeSet() {
		Assert.assertEquals("Default value of the core size should be one", 1,
				this.properties.getCorePoolSize());

		int newSize = 138;
		this.properties.setCorePoolSize(newSize);
		Assert.assertEquals("Core size should have been reset", newSize,
				this.properties.getCorePoolSize());
	}

	@Test
	public void maxPoolSizeCanBeSet() {
		Assert.assertEquals(
				"Default value of the max pool size should be integer max value",
				Integer.MAX_VALUE, this.properties.getMaxPoolSize());

		int newSize = 11;
		this.properties.setMaxPoolSize(newSize);
		Assert.assertEquals("Max pool size should have been reset", newSize,
				this.properties.getMaxPoolSize());
	}

	@Test
	public void queueCapacityCanBeSet() {
		Assert.assertEquals(
				"Default value of the queue capacity size should be integer max value",
				Integer.MAX_VALUE, this.properties.getQueueCapacity());

		int newSize = 11;
		this.properties.setQueueCapacity(newSize);
		Assert.assertEquals("Queue capacity should have been reset", newSize,
				this.properties.getQueueCapacity());
	}
}
