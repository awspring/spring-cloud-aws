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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.assertj.core.util.Arrays;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Test for {@link ImdsPropertySource} Attempts to acquire instance metadata should succeed on a best-effort basis. Any
 * exceptions or missing keys should be logged at debug level and ignored.
 *
 * @author Ken Krueger
 * @since 3.1.0
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

		assertThat(propertySource.getProperty("mac"))
				.describedAs("Resulting PropertySource should contain the test data").isEqualTo("mac");
	}

	@Test
	public void testNoMetadataAvailable() {
		when(mockUtils.isRunningOnCloudEnvironment()).thenReturn(false);
		ImdsPropertySource propertySource = new ImdsPropertySource("test", mockUtils);
		propertySource.init();

		assertThat(propertySource.getPropertyNames())
			.describedAs("Resulting PropertySource should be empty")
			.hasSize(0);
	}

	@Test
	public void testCopy() {
		ImdsPropertySource propertySource = new ImdsPropertySource("test", mockUtils);
		propertySource.init();

		ImdsPropertySource copy = propertySource.copy();

		// Loop through each property in copy's getPropertyNames
		// and make sure it's in the original propertySource
		for (String name : copy.getPropertyNames()) {
			assertThat(propertySource.containsProperty(name))
					.describedAs("The two PropertySources should contain the same keys").isTrue();
			assertThat(copy.getProperty(name)).describedAs("Each property should be equal")
					.isEqualTo(propertySource.getProperty(name));

		}
	}

	@Test
	public void testGetPropertyNames() {
		ImdsPropertySource propertySource = new ImdsPropertySource("test", mockUtils);
		propertySource.init();

		List<Object> keyList = Arrays.asList(propertySource.getPropertyNames());
		assertThat(keyList).describedAs("None of the keys from our test data should be missing.")
				.containsAll(testMap.keySet());
	}

}
