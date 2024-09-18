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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.lang.reflect.Field;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.imds.Ec2MetadataClient;
import software.amazon.awssdk.imds.Ec2MetadataResponse;

/**
 * Test for the {@link ImdsUtils}.
 *
 * ImdsUtils heavily utilizes the AWS SDK's Ec2MetadataClient class for metadata lookup. A main goal is to fail silently
 * when instance metadata is unavailable.
 *
 * @author Ken Krueger
 * @since 3.1.0
 */
public class ImdsUtilsTest {

	/**
	 * Positive test for {@link ImdsUtils#isRunningOnCloudEnvironment()} If instance metadata is available, a call to
	 * retrieve the key "/latest/meta-data/ami-id" will be successful. We also expect to log a message indicating that
	 * the instance is running on a cloud environment.
	 */
	@Test
	public void isRunningOnCloudEnvironment() throws Exception {

		Ec2MetadataClient mockClient = mock(Ec2MetadataClient.class);
		when(mockClient.get("/latest/meta-data/ami-id")).thenReturn(Ec2MetadataResponse.create("sample"));

		ImdsUtils utils = new ImdsUtils(mockClient);

		// Use reflection to set the mock logger in the private field
		Logger mockLogger = mock(Logger.class);
		Field loggerField = ImdsUtils.class.getDeclaredField("logger");
		loggerField.setAccessible(true);
		loggerField.set(utils, mockLogger);

		assertThat(utils.isRunningOnCloudEnvironment()).isTrue();

		verify(mockLogger).info(contains("application is running within an EC2 instance"));
	}

	/**
	 * Negative test for {@link ImdsUtils#isRunningOnCloudEnvironment()} If instance metadata is not available, we
	 * should receive an error about too many retries.
	 */
	@Test
	public void notRunningOnCloudEnvironment() throws Exception {

		SdkClientException exception = SdkClientException.create("too many retries");
		Ec2MetadataClient mockClient = mock(Ec2MetadataClient.class);
		when(mockClient.get("/latest/meta-data/ami-id")).thenThrow(exception);

		ImdsUtils utils = new ImdsUtils(mockClient);

		// Use reflection to set the mock logger in the private field
		Logger mockLogger = mock(Logger.class);
		Field loggerField = ImdsUtils.class.getDeclaredField("logger");
		loggerField.setAccessible(true);
		loggerField.set(utils, mockLogger);

		assertThat(utils.isRunningOnCloudEnvironment()).isFalse();

		verify(mockLogger).info(contains("application is NOT running"));

	}

	/**
	 * Negative test for {@link ImdsUtils#isRunningOnCloudEnvironment()} Any unknown exception should be handled by
	 * logging and continuing. An application should not fail due to the unavailability of instance metadata. Any code
	 * that relies on instance metadata should check for its existance.
	 */
	@Test
	public void unknownExceptionHandled() throws Exception {

		RuntimeException exception = new RuntimeException("unknown");
		Ec2MetadataClient mockClient = mock(Ec2MetadataClient.class);
		when(mockClient.get("/latest/meta-data/ami-id")).thenThrow(exception);

		ImdsUtils utils = new ImdsUtils(mockClient);

		// Use reflection to set the mock logger in the private field
		Logger mockLogger = mock(Logger.class);
		Field loggerField = ImdsUtils.class.getDeclaredField("logger");
		loggerField.setAccessible(true);
		loggerField.set(utils, mockLogger);

		assertThat(utils.isRunningOnCloudEnvironment()).isFalse();

		verify(mockLogger).error(contains("Error occurred when"), any(Exception.class));

	}

	/**
	 * test for {@link ImdsUtils#getEc2InstanceMetadata()} Attempts to acquire instance metadata should succeed on a
	 * best-effort basis. Any exceptions or missing keys should be logged at debug level and ignored.
	 */
	@Test
	public void testGetEc2InstanceMetadata() {

		SdkClientException sdkException = SdkClientException.create("too many retries");
		RuntimeException runtimeException = new RuntimeException("unknown");

		Ec2MetadataClient mockClient = mock(Ec2MetadataClient.class);
		when(mockClient.get("/latest/meta-data/ami-id")).thenReturn(Ec2MetadataResponse.create("ami"));
		when(mockClient.get("/latest/meta-data/instance-id")).thenReturn(Ec2MetadataResponse.create("instance-id"));
		when(mockClient.get("/latest/meta-data/mac")).thenReturn(Ec2MetadataResponse.create("mac"));
		when(mockClient.get("/latest/meta-data/public-hostname")).thenThrow(runtimeException);
		when(mockClient.get("/latest/meta-data/local-hostname")).thenThrow(sdkException);

		ImdsUtils utils = new ImdsUtils(mockClient);

		Map<String, String> results = utils.getEc2InstanceMetadata();

		assertThat(results).hasSize(3).containsKeys("ami-id", "instance-id", "mac").containsValue("mac")
				.doesNotContainKey("public-hostname");
	}

}
