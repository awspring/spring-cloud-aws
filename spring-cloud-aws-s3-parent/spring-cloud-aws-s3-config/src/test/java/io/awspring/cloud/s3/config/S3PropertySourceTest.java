/*
 * Copyright 2013-2023 the original author or authors.
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
package io.awspring.cloud.s3.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mockito;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.S3Exception;

/**
 * Unit tests for {@link S3PropertySource}.
 *
 * @author Kunal Varpe
 */
class S3PropertySourceTest {

	private static final String YAML_TYPE = "application/x-yaml";
	private static final String YAML_TYPE_ALTERNATIVE = "text/yaml";
	private static final String TEXT_TYPE = "text/plain";
	private static final String JSON_TYPE = "application/json";
	private S3Client s3Client = Mockito.mock(S3Client.class);

	@ParameterizedTest
	@MethodSource("propertyFileContentSource")
	void shouldParseDifferentFilesFromBucket(String sourceFileName, String contents, String contentType) {
		S3PropertySource propertySource = new S3PropertySource("test-bucket/" + sourceFileName, s3Client);
		InputStream is = new ByteArrayInputStream(contents.getBytes(StandardCharsets.UTF_8));
		ResponseInputStream<GetObjectResponse> response = new ResponseInputStream<>(
				GetObjectResponse.builder().contentType(contentType).build(), is);

		when(s3Client.getObject(any(GetObjectRequest.class))).thenReturn(response);

		propertySource.init();

		assertThat(propertySource.getName()).isEqualTo("aws-s3:test-bucket/" + sourceFileName);
		assertThat(propertySource.getPropertyNames()).containsExactly("key1", "key2");
		assertThat(propertySource.getProperty("key2")).isEqualTo("value2");
	}

	private static Stream<Arguments> propertyFileContentSource() {
		String props = """
				key1=value1
				key2=value2""";
		String ymlProps = """
				key1: value1
				key2: value2""";
		String jsonProps = """
				{ "key1": "value1", "key2": "value2"}""";
		return Stream.of(Arguments.arguments("application.properties", props, TEXT_TYPE),
				Arguments.arguments("application.yml", ymlProps, YAML_TYPE),
				Arguments.arguments("application.yaml", ymlProps, YAML_TYPE_ALTERNATIVE),
				Arguments.arguments("application.json", jsonProps, JSON_TYPE));
	}

	@Test
	void shouldThrowNoSuchKeyExceptionWhenKeyDoesNotExists() {
		S3PropertySource propertySource = new S3PropertySource("test-bucket;", s3Client);

		when(s3Client.getObject(any(GetObjectRequest.class))).thenThrow(NoSuchKeyException.class);

		assertThatThrownBy(propertySource::init, "You must provide key", NoSuchKeyException.class);
	}

	@Test
	void shouldThrowS3ExceptionWhenBucketDoesNotExists() {
		S3PropertySource propertySource = new S3PropertySource("", s3Client);

		when(s3Client.getObject(any(GetObjectRequest.class))).thenThrow(S3Exception.class);

		assertThatThrownBy(propertySource::init, "You must provide key", S3Exception.class);
	}
}
