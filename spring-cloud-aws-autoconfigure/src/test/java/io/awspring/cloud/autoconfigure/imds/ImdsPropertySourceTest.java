/*
 * Copyright 2013-2022 the original author or authors.
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
package io.awspring.cloud.autoconfigure.imds;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.assertj.core.util.Arrays;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * test for {@link ImdsPropertySource} Attempts to acquire instance metadata should succeed on a best-effort basis. Any
 * exceptions or missing keys should be logged at debug level and ignored.
 *
 * @author Ken Krueger
 * @since 3.0.4
 */
public class ImdsPropertySourceTest {

	private Map<String, String> testMap;

	private ImdsUtils mockUtils;

	@BeforeEach
	public void setup() {
		testMap = new HashMap<>();
		testMap.put("mac", "mac");
		testMap.put("ami-id", "ami");
		testMap.put("instance-id", "instance-id");

		mockUtils = mock(ImdsUtils.class);
		when(mockUtils.isRunningOnCloudEnvironment()).thenReturn(true);
		when(mockUtils.getEc2InstanceMetadata()).thenReturn(testMap);
	}

	@Test
	public void testInit() {
		ImdsPropertySource propertySource = new ImdsPropertySource("test", mockUtils);
		propertySource.init();

		assertTrue("Resulting PropertySource should contain the test data",
				propertySource.getProperty("mac").equals("mac"));
	}

	@Test
	public void testNoMetadataAvailable() {
		when(mockUtils.isRunningOnCloudEnvironment()).thenReturn(false);
		ImdsPropertySource propertySource = new ImdsPropertySource("test", mockUtils);
		propertySource.init();

		assertTrue("Resulting PropertySource should be empty", propertySource.getPropertyNames().length == 0);
	}

	@Test
	public void testCopy() {
		ImdsPropertySource propertySource = new ImdsPropertySource("test", mockUtils);
		propertySource.init();

		ImdsPropertySource copy = propertySource.copy();

		// Loop through each property in copy's getPropertyNames
		// and make sure it's in the original propertySource
		for (String name : copy.getPropertyNames()) {
			assertTrue("The two PropertySources should contain the same keys", propertySource.containsProperty(name));
			assertEquals("Each property should be equal", copy.getProperty(name), propertySource.getProperty(name));
		}
	}

	@Test
	public void testGetPropertyNames() {
		ImdsPropertySource propertySource = new ImdsPropertySource("test", mockUtils);
		propertySource.init();

		List<Object> keyList = Arrays.asList(propertySource.getPropertyNames());
		assertTrue("None of the keys from our test data should be missing.", keyList.containsAll(testMap.keySet()));
	}

}
