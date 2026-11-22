/*
 * Copyright 2013-2026 the original author or authors.
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
package io.awspring.cloud.sqs.support.resolver;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Method;
import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.util.ReflectionUtils;
import software.amazon.awssdk.eventnotifications.s3.model.S3EventNotification;
import software.amazon.awssdk.eventnotifications.s3.model.S3EventNotificationRecord;

/**
 * Tests for {@link S3EventNotificationArgumentResolver}.
 *
 * @author Kassa
 */
class S3EventNotificationArgumentResolverTests {

	static final String OBJECT_CREATED_EVENT = """
			{"Records":[{"eventVersion":"2.1","eventSource":"aws:s3","awsRegion":"us-east-1",
			"eventTime":"2026-11-21T12:00:00.000Z","eventName":"ObjectCreated:Put",
			"s3":{"s3SchemaVersion":"1.0","configurationId":"config",
			"bucket":{"name":"my-bucket","arn":"arn:aws:s3:::my-bucket"},
			"object":{"key":"reports/report.csv","size":1024,"eTag":"etag"}}}]}""";

	static final String TEST_EVENT = """
			{"Service":"Amazon S3","Event":"s3:TestEvent","Time":"2026-11-21T12:00:00.000Z","Bucket":"my-bucket"}""";

	private final S3EventNotificationArgumentResolver resolver = new S3EventNotificationArgumentResolver();

	@Test
	void shouldSupportS3EventNotificationParameter() {
		assertThat(resolver.supportsParameter(parameter("listen", S3EventNotification.class))).isTrue();
	}

	@Test
	void shouldNotSupportOtherParameters() {
		assertThat(resolver.supportsParameter(parameter("listenToString", String.class))).isFalse();
	}

	@Test
	void shouldResolveObjectCreatedEvent() {
		Object resolved = resolver.resolveArgument(parameter("listen", S3EventNotification.class),
				MessageBuilder.withPayload(OBJECT_CREATED_EVENT).build());

		assertThat(resolved).isInstanceOfSatisfying(S3EventNotification.class, notification -> {
			assertThat(notification.getRecords()).hasSize(1);
			S3EventNotificationRecord record = notification.getRecords().get(0);
			assertThat(record.getEventName()).isEqualTo("ObjectCreated:Put");
			assertThat(record.getS3().getBucket().getName()).isEqualTo("my-bucket");
			assertThat(record.getS3().getObject().getKey()).isEqualTo("reports/report.csv");
		});
	}

	@Test
	void shouldResolveTestEventToEmptyRecords() {
		Object resolved = resolver.resolveArgument(parameter("listen", S3EventNotification.class),
				MessageBuilder.withPayload(TEST_EVENT).build());

		assertThat(resolved).isInstanceOfSatisfying(S3EventNotification.class,
				notification -> assertThat(notification.getRecords()).isEmpty());
	}

	private static MethodParameter parameter(String methodName, Class<?> parameterType) {
		Method method = ReflectionUtils.findMethod(Listener.class, methodName, parameterType);
		assertThat(method).isNotNull();
		return new MethodParameter(method, 0);
	}

	@SuppressWarnings("unused")
	static class Listener {

		void listen(S3EventNotification notification) {
		}

		void listenToString(String message) {
		}

	}

}
