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

import com.amazonaws.regions.Regions;

/**
 * Tests for {@link AwsRegionProperties}.
 *
 * @author tgianos
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
		Assert.assertTrue("Default value of auto should be true",
				this.properties.isAuto());

		this.properties.setAuto(false);
		Assert.assertFalse("Auto should have been reassigned as false",
				this.properties.isAuto());
	}

	@Test
	public void staticRegionCanBeSet() {
		Assert.assertNull("Static region value should have default of null",
				this.properties.getStatic());

		this.properties.setStatic(Regions.US_EAST_1.getName());
		Assert.assertEquals("Static region should have been assigned to us-east-1",
				Regions.US_EAST_1.getName(), this.properties.getStatic());
	}
}
