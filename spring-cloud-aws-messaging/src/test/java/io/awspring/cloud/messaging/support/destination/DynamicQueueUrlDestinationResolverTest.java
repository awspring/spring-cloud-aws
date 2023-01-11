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

package io.awspring.cloud.messaging.support.destination;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.model.CreateQueueRequest;
import com.amazonaws.services.sqs.model.CreateQueueResult;
import com.amazonaws.services.sqs.model.GetQueueUrlRequest;
import com.amazonaws.services.sqs.model.GetQueueUrlResult;
import com.amazonaws.services.sqs.model.QueueDoesNotExistException;
import io.awspring.cloud.core.env.ResourceIdResolver;
import org.junit.jupiter.api.Test;

import org.springframework.messaging.core.DestinationResolutionException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class DynamicQueueUrlDestinationResolverTest {

	@Test
	void testAutoCreate() {
		AmazonSQS amazonSqs = mock(AmazonSQS.class);
		String queueUrl = "https://foo/bar";
		when(amazonSqs.createQueue(new CreateQueueRequest("foo")))
				.thenReturn(new CreateQueueResult().withQueueUrl(queueUrl));

		DynamicQueueUrlDestinationResolver dynamicQueueDestinationResolver = new DynamicQueueUrlDestinationResolver(
				amazonSqs);
		dynamicQueueDestinationResolver.setAutoCreate(true);
		assertThat(dynamicQueueDestinationResolver.resolveDestination("foo")).isEqualTo(queueUrl);
	}

	@Test
	void testAbsoluteUrl() {
		AmazonSQS amazonSqs = mock(AmazonSQS.class);
		DynamicQueueUrlDestinationResolver dynamicQueueDestinationResolver = new DynamicQueueUrlDestinationResolver(
				amazonSqs);
		String destination = "https://sqs-amazon.aws.com/123123123/myQueue";
		assertThat(dynamicQueueDestinationResolver.resolveDestination(destination)).isEqualTo(destination);
	}

	@Test
	void resolveDestination_shouldResolveArnToUrl() {
		AmazonSQS amazonSqs = mock(AmazonSQS.class);
		String expectedQueueUrl = "https://sqs-amazon.aws.com/123123123/myQueue";
		DynamicQueueUrlDestinationResolver dynamicQueueDestinationResolver = new DynamicQueueUrlDestinationResolver(
				amazonSqs);
		when(amazonSqs.getQueueUrl(new GetQueueUrlRequest("myQueue").withQueueOwnerAWSAccountId("123123123")))
				.thenReturn(new GetQueueUrlResult().withQueueUrl(expectedQueueUrl));
		String arn = "arn:aws:sqs:eu-central-1:123123123:myQueue";
		assertThat(dynamicQueueDestinationResolver.resolveDestination(arn)).isEqualTo(expectedQueueUrl);
	}

	@Test
	void resolveDestination_shouldThrowError_IfQueueDoesNotExist() {
		AmazonSQS amazonSqs = mock(AmazonSQS.class);
		AmazonServiceException exception = new QueueDoesNotExistException("AWS.SimpleQueueService.NonExistentQueue");
		exception.setErrorCode("AWS.SimpleQueueService.NonExistentQueue");
		String arn = "arn:aws:sqs:eu-central-1:123123123:myQueue";
		when(amazonSqs.getQueueUrl(new GetQueueUrlRequest("myQueue").withQueueOwnerAWSAccountId("123123123")))
				.thenThrow(exception);
		DynamicQueueUrlDestinationResolver dynamicQueueDestinationResolver = new DynamicQueueUrlDestinationResolver(
				amazonSqs);
		try {
			dynamicQueueDestinationResolver.resolveDestination(arn);
		}
		catch (DestinationResolutionException e) {
			assertThat(e.getMessage()).startsWith("The queue does not exist.");
		}
	}

	@Test
	void testNoAutoCreate() {
		AmazonSQS amazonSqs = mock(AmazonSQS.class);
		String queueUrl = "https://foo/bar";
		when(amazonSqs.getQueueUrl(new GetQueueUrlRequest("foo")))
				.thenReturn(new GetQueueUrlResult().withQueueUrl(queueUrl));

		DynamicQueueUrlDestinationResolver dynamicQueueDestinationResolver = new DynamicQueueUrlDestinationResolver(
				amazonSqs);
		assertThat(dynamicQueueDestinationResolver.resolveDestination("foo")).isEqualTo(queueUrl);
	}

	@Test
	void testInvalidDestinationName() {
		AmazonSQS amazonSqs = mock(AmazonSQS.class);
		AmazonServiceException exception = new QueueDoesNotExistException("AWS.SimpleQueueService.NonExistentQueue");
		exception.setErrorCode("AWS.SimpleQueueService.NonExistentQueue");
		String queueUrl = "invalidName";
		when(amazonSqs.getQueueUrl(new GetQueueUrlRequest(queueUrl))).thenThrow(exception);
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
	void testPotentiallyNoAccessToPerformGetQueueUrl() {
		AmazonSQS amazonSqs = mock(AmazonSQS.class);
		AmazonServiceException exception = new QueueDoesNotExistException("AWS.SimpleQueueService.NonExistentQueue");
		exception.setErrorCode("AWS.SimpleQueueService.NonExistentQueue");
		exception.setErrorMessage("The specified queue does not exist or you do not have access to it.");
		String queueUrl = "noAccessGetQueueUrlName";
		when(amazonSqs.getQueueUrl(new GetQueueUrlRequest(queueUrl))).thenThrow(exception);
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
	void resolveDestination_withResourceIdResolver_shouldUseIt() {
		AmazonSQS amazonSqs = mock(AmazonSQS.class);
		ResourceIdResolver resourceIdResolver = mock(ResourceIdResolver.class);
		when(resourceIdResolver.resolveToPhysicalResourceId(anyString())).thenReturn("http://queue.com");
		DynamicQueueUrlDestinationResolver dynamicQueueUrlDestinationResolver = new DynamicQueueUrlDestinationResolver(
				amazonSqs, resourceIdResolver);

		String physicalResourceId = dynamicQueueUrlDestinationResolver.resolveDestination("testQueue");

		assertThat(physicalResourceId).isEqualTo("http://queue.com");

	}

	@Test
	void resolveDestination_withResourceIdResolver_nonUrlId_shouldGetUrlByResolvedName() {
		String queueUrl = "http://queue.com";
		String resolvedQueueName = "some-queue-name";
		AmazonSQS amazonSqs = mock(AmazonSQS.class);
		when(amazonSqs.getQueueUrl(new GetQueueUrlRequest(resolvedQueueName)))
				.thenReturn(new GetQueueUrlResult().withQueueUrl(queueUrl));
		ResourceIdResolver resourceIdResolver = mock(ResourceIdResolver.class);
		when(resourceIdResolver.resolveToPhysicalResourceId(anyString())).thenReturn(resolvedQueueName);
		DynamicQueueUrlDestinationResolver dynamicQueueUrlDestinationResolver = new DynamicQueueUrlDestinationResolver(
				amazonSqs, resourceIdResolver);

		String physicalResourceId = dynamicQueueUrlDestinationResolver.resolveDestination("testQueue");

		assertThat(physicalResourceId).isEqualTo("http://queue.com");
	}

	@Test
	void instantiation_withNullAmazonClient_shouldThrowAnError() {
		assertThatThrownBy(() -> new DynamicQueueUrlDestinationResolver(null, null))
				.isInstanceOf(IllegalArgumentException.class);
	}

}
