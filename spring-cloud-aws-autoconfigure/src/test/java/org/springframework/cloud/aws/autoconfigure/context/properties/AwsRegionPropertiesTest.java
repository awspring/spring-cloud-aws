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

import com.amazonaws.regions.Regions;
import org.junit.Before;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link AwsRegionProperties}.
 *
 * @author Tom Gianos
 * @since 2.0.2
 */
public class AwsRegionPropertiesTest {

	private AwsRegionProperties properties;

	@Before
	public void setup() {
		this.properties = new AwsRegionProperties();
	}

	@Test
	public void autoCanBeSet() {
		assertThat(this.properties.isAuto()).as("Default value of auto should be true")
				.isTrue();

		this.properties.setAuto(false);
		assertThat(this.properties.isAuto())
				.as("Auto should have been reassigned as false").isFalse();
	}

	@Test
	public void staticRegionCanBeSet() {
		assertThat(this.properties.getStatic())
				.as("Static region value should have default of null").isNull();

		this.properties.setStatic(Regions.US_EAST_1.getName());
		assertThat(this.properties.getStatic())
				.as("Static region should have been assigned to us-east-1")
				.isEqualTo(Regions.US_EAST_1.getName());
	}

}
