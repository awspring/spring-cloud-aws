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
package io.awspring.cloud.sqs.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.awspring.cloud.sqs.QueueAttributesResolvingException;
import io.awspring.cloud.sqs.config.SqsBootstrapConfiguration;
import io.awspring.cloud.sqs.listener.QueueAttributes;
import io.awspring.cloud.sqs.listener.QueueAttributesResolver;
import io.awspring.cloud.sqs.listener.QueueNotFoundStrategy;
import java.util.Collections;
import java.util.UUID;
import java.util.concurrent.CompletionException;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import software.amazon.awssdk.services.sqs.SqsAsyncClient;
import software.amazon.awssdk.services.sqs.model.CreateQueueRequest;
import software.amazon.awssdk.services.sqs.model.DeleteQueueRequest;
import software.amazon.awssdk.services.sqs.model.GetQueueUrlRequest;
import software.amazon.awssdk.services.sqs.model.QueueAttributeName;
import software.amazon.awssdk.services.sqs.model.QueueDoesNotExistException;

/**
 * Integration tests for {@link QueueAttributesResolver}.
 *
 * @author Tomaz Fernandes
 */
@SpringBootTest(classes = SqsBootstrapConfiguration.class)
class QueueAttributesResolverIntegrationTests extends BaseSqsIntegrationTest {

	// @formatter:off
	@Test
	void shouldCreateQueue() {
		String queueName = "testQueueName-" + UUID.randomUUID();
		SqsAsyncClient client = createAsyncClient();
		QueueAttributesResolver resolver = QueueAttributesResolver
			.builder()
			.queueAttributeNames(Collections.emptyList())
			.sqsAsyncClient(client)
			.queueName(queueName)
			.queueNotFoundStrategy(QueueNotFoundStrategy.CREATE)
			.build();
		try {
			QueueAttributes attributes = resolver.resolveQueueAttributes().join();
			assertThat(attributes.getQueueName()).isEqualTo(queueName);
			assertThat(attributes.getQueueAttribute(QueueAttributeName.QUEUE_ARN)).isNull();
		}
		finally {
			String queueUrl = client.getQueueUrl(GetQueueUrlRequest.builder().queueName(queueName).build()).join().queueUrl();
			client.deleteQueue(DeleteQueueRequest.builder().queueUrl(queueUrl).build()).join();
		}
	}

	@Test
	void shouldNotCreateQueue() {
		String queueName = "testQueueName-" + UUID.randomUUID();
		SqsAsyncClient client = createAsyncClient();
		QueueAttributesResolver resolver = QueueAttributesResolver
			.builder()
			.queueAttributeNames(Collections.emptyList())
			.sqsAsyncClient(client)
			.queueName(queueName)
			.queueNotFoundStrategy(QueueNotFoundStrategy.FAIL)
			.build();
		assertThatThrownBy(() -> resolver.resolveQueueAttributes().join())
			.isInstanceOf(CompletionException.class)
			.extracting(Throwable::getCause)
			.isInstanceOf(QueueAttributesResolvingException.class)
			.extracting(Throwable::getCause)
			.isInstanceOf(QueueDoesNotExistException.class);
	}

	@Test
	void shouldGetQueueAttributes() {
		String queueName = "should-get-queue-attributes";
		SqsAsyncClient client = createAsyncClient();
		QueueAttributesResolver resolver = QueueAttributesResolver
			.builder()
			.queueAttributeNames(Collections.singletonList(QueueAttributeName.QUEUE_ARN))
			.sqsAsyncClient(client)
			.queueName(queueName)
			.queueNotFoundStrategy(QueueNotFoundStrategy.CREATE)
			.build();
		try {
			QueueAttributes attributes = resolver.resolveQueueAttributes().join();
			assertThat(attributes.getQueueAttribute(QueueAttributeName.QUEUE_ARN)).isNotNull();
		}
		finally {
			String queueUrl = client.getQueueUrl(GetQueueUrlRequest.builder().queueName(queueName).build()).join().queueUrl();
			client.deleteQueue(DeleteQueueRequest.builder().queueUrl(queueUrl).build()).join();
		}
	}

	@Test
	void shouldResolveFromUri() {
		String queueName = "should-resolve-from-uri";
		SqsAsyncClient client = createAsyncClient();
		String queueUrl = client.createQueue(CreateQueueRequest.builder().queueName(queueName).build()).join().queueUrl();
		QueueAttributesResolver resolver = QueueAttributesResolver
			.builder()
			.sqsAsyncClient(client)
			.queueName(queueUrl)
			.queueAttributeNames(Collections.emptyList())
			.queueNotFoundStrategy(QueueNotFoundStrategy.CREATE)
			.build();
		try {
			QueueAttributes attributes = resolver.resolveQueueAttributes().join();
			assertThat(attributes.getQueueUrl()).isEqualTo(queueUrl);
			assertThat(attributes.getQueueName()).isEqualTo(queueUrl);
			assertThat(attributes.getQueueAttributes()).isEmpty();
		}
		finally {
			client.deleteQueue(DeleteQueueRequest.builder().queueUrl(queueUrl).build()).join();
		}
	}
	// @formatter:on

}
