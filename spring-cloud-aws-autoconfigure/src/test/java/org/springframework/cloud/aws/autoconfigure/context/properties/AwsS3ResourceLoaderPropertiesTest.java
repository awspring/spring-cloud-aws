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

package org.springframework.cloud.aws.autoconfigure.context.properties;

import org.junit.Before;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link AwsS3ResourceLoaderProperties}.
 *
 * @author Tom Gianos
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
		assertThat(this.properties.getCorePoolSize())
				.as("Default value of the core size should be one").isEqualTo(1);

		int newSize = 138;
		this.properties.setCorePoolSize(newSize);
		assertThat(this.properties.getCorePoolSize())
				.as("Core size should have been reset").isEqualTo(newSize);
	}

	@Test
	public void maxPoolSizeCanBeSet() {
		assertThat(this.properties.getMaxPoolSize())
				.as("Default value of the max pool size should be integer max value")
				.isEqualTo(Integer.MAX_VALUE);

		int newSize = 11;
		this.properties.setMaxPoolSize(newSize);
		assertThat(this.properties.getMaxPoolSize())
				.as("Max pool size should have been reset").isEqualTo(newSize);
	}

	@Test
	public void queueCapacityCanBeSet() {
		assertThat(this.properties.getQueueCapacity()).as(
				"Default value of the queue capacity size should be integer max value")
				.isEqualTo(Integer.MAX_VALUE);

		int newSize = 11;
		this.properties.setQueueCapacity(newSize);
		assertThat(this.properties.getQueueCapacity())
				.as("Queue capacity should have been reset").isEqualTo(newSize);
	}

}
