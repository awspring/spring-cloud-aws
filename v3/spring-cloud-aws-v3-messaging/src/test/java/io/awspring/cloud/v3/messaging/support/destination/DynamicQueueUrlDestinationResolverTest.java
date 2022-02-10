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

package io.awspring.cloud.v3.messaging.support.destination;

import io.awspring.cloud.v3.core.env.ResourceIdResolver;
import org.junit.jupiter.api.Test;

import org.springframework.messaging.core.DestinationResolutionException;
import software.amazon.awssdk.awscore.exception.AwsErrorDetails;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.CreateQueueRequest;
import software.amazon.awssdk.services.sqs.model.CreateQueueResponse;
import software.amazon.awssdk.services.sqs.model.GetQueueUrlRequest;
import software.amazon.awssdk.services.sqs.model.GetQueueUrlResponse;
import software.amazon.awssdk.services.sqs.model.QueueDoesNotExistException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class DynamicQueueUrlDestinationResolverTest {

	@Test
	void testAutoCreate() throws Exception {
		SqsClient amazonSqs = mock(SqsClient.class);
		String queueUrl = "https://foo/bar";
		when(amazonSqs.createQueue(CreateQueueRequest.builder().queueName("foo").build()))
			.thenReturn(CreateQueueResponse.builder().queueUrl(queueUrl).build());

		DynamicQueueUrlDestinationResolver dynamicQueueDestinationResolver = new DynamicQueueUrlDestinationResolver(
				amazonSqs);
		dynamicQueueDestinationResolver.setAutoCreate(true);
		assertThat(dynamicQueueDestinationResolver.resolveDestination("foo")).isEqualTo(queueUrl);
	}

	@Test
	void testAbsoluteUrl() throws Exception {
		SqsClient amazonSqs = mock(SqsClient.class);
		DynamicQueueUrlDestinationResolver dynamicQueueDestinationResolver = new DynamicQueueUrlDestinationResolver(
				amazonSqs);
		String destination = "https://sqs-amazon.aws.com/123123123/myQueue";
		assertThat(dynamicQueueDestinationResolver.resolveDestination(destination)).isEqualTo(destination);
	}

	@Test
	void testNoAutoCreate() throws Exception {
		SqsClient amazonSqs = mock(SqsClient.class);
		String queueUrl = "https://foo/bar";
		when(eq(amazonSqs.createQueue(CreateQueueRequest.builder()
			.queueName("foo")
			.build())))
			.thenReturn(CreateQueueResponse.builder().queueUrl(queueUrl).build());

		DynamicQueueUrlDestinationResolver dynamicQueueDestinationResolver = new DynamicQueueUrlDestinationResolver(
				amazonSqs);
		assertThat(dynamicQueueDestinationResolver.resolveDestination("foo")).isEqualTo(queueUrl);
	}

	@Test
	void testInvalidDestinationName() throws Exception {
		SqsClient amazonSqs = mock(SqsClient.class);
		QueueDoesNotExistException exception = QueueDoesNotExistException.builder()
			.message("AWS.SimpleQueueService.NonExistentQueue")
			.build();
		String queueUrl = "invalidName";
		when(amazonSqs.getQueueUrl(GetQueueUrlRequest.builder().
			queueName(queueUrl)
			.build())).thenThrow(exception);
		DynamicQueueUrlDestinationResolver dynamicQueueDestinationResolver = new DynamicQueueUrlDestinationResolver(
				amazonSqs);

		try {
			dynamicQueueDestinationResolver.resolveDestination(queueUrl);
		}
		catch (DestinationResolutionException e) {
			assertThat(e.getMessage()).startsWith("The queue does not exist.");
		}
	}

	@Test
	void testPotentiallyNoAccessToPerformGetQueueUrl() throws Exception {
		SqsClient amazonSqs = mock(SqsClient.class);
		QueueDoesNotExistException exception = QueueDoesNotExistException.builder()
			.awsErrorDetails(AwsErrorDetails.builder()
				.serviceName("SimpleQueueService")
				.errorMessage("NonExistentQueue")
				.build())
			.message("The specified queue does not exist or you do not have access to it.")
			.build();
		String queueUrl = "noAccessGetQueueUrlName";
		when(amazonSqs.getQueueUrl(GetQueueUrlRequest.builder().queueName(queueUrl).build())).thenThrow(exception);
		DynamicQueueUrlDestinationResolver dynamicQueueDestinationResolver = new DynamicQueueUrlDestinationResolver(
				amazonSqs);
		try {
			dynamicQueueDestinationResolver.resolveDestination(queueUrl);
		}
		catch (DestinationResolutionException e) {
			assertThat(e.getMessage())
					.startsWith("The queue does not exist or no access to perform action sqs:GetQueueUrl.");
		}
	}

	@Test
	void resolveDestination_withResourceIdResolver_shouldUseIt() throws Exception {
		SqsClient amazonSqs = mock(SqsClient.class);
		ResourceIdResolver resourceIdResolver = mock(ResourceIdResolver.class);
		when(resourceIdResolver.resolveToPhysicalResourceId(anyString())).thenReturn("http://queue.com");
		DynamicQueueUrlDestinationResolver dynamicQueueUrlDestinationResolver = new DynamicQueueUrlDestinationResolver(
				amazonSqs, resourceIdResolver);

		String physicalResourceId = dynamicQueueUrlDestinationResolver.resolveDestination("testQueue");

		assertThat(physicalResourceId).isEqualTo("http://queue.com");

	}

	@Test
	void resolveDestination_withResourceIdResolver_nonUrlId_shouldGetUrlByResolvedName() throws Exception {
		String queueUrl = "http://queue.com";
		String resolvedQueueName = "some-queue-name";
		SqsClient amazonSqs = mock(SqsClient.class);
		when(amazonSqs.getQueueUrl(GetQueueUrlRequest.builder().queueName(resolvedQueueName).build()))
				.thenReturn(GetQueueUrlResponse.builder().queueUrl(queueUrl).build());
		ResourceIdResolver resourceIdResolver = mock(ResourceIdResolver.class);
		when(resourceIdResolver.resolveToPhysicalResourceId(anyString())).thenReturn(resolvedQueueName);
		DynamicQueueUrlDestinationResolver dynamicQueueUrlDestinationResolver = new DynamicQueueUrlDestinationResolver(
				amazonSqs, resourceIdResolver);

		String physicalResourceId = dynamicQueueUrlDestinationResolver.resolveDestination("testQueue");

		assertThat(physicalResourceId).isEqualTo("http://queue.com");
	}

	@Test
	void instantiation_withNullAmazonClient_shouldThrowAnError() throws Exception {
		assertThatThrownBy(() -> new DynamicQueueUrlDestinationResolver(null, null))
				.isInstanceOf(IllegalArgumentException.class);
	}

}
