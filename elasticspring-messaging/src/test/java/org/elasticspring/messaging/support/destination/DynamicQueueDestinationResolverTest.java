/*
 * Copyright 2013 the original author or authors.
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

package org.elasticspring.messaging.support.destination;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.model.CreateQueueRequest;
import com.amazonaws.services.sqs.model.CreateQueueResult;
import com.amazonaws.services.sqs.model.GetQueueUrlRequest;
import com.amazonaws.services.sqs.model.GetQueueUrlResult;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

/**
 *
 */
public class DynamicQueueDestinationResolverTest {

	@Test
	public void testAutoCreate() throws Exception {
		AmazonSQS amazonSqs = Mockito.mock(AmazonSQS.class);
		String queueUrl = "http://foo/bar";
		Mockito.when(amazonSqs.createQueue(new CreateQueueRequest("foo"))).thenReturn(new CreateQueueResult().withQueueUrl(queueUrl));

		DynamicQueueDestinationResolver dynamicQueueDestinationResolver = new DynamicQueueDestinationResolver(amazonSqs);
		dynamicQueueDestinationResolver.setAutoCreate(true);
		Assert.assertEquals(queueUrl, dynamicQueueDestinationResolver.resolveDestinationName("foo"));
	}

	@Test
	public void testAbsoluteUrl() throws Exception {
		AmazonSQS amazonSqs = Mockito.mock(AmazonSQS.class);
		DynamicQueueDestinationResolver dynamicQueueDestinationResolver = new DynamicQueueDestinationResolver(amazonSqs);
		String destination = "http://sqs-amazon.aws.com/123123123/myQueue";
		String queueUrl = dynamicQueueDestinationResolver.resolveDestinationName(destination);
		Assert.assertEquals(destination, queueUrl);
	}

	@Test
	public void testNoAutoCreate() throws Exception {
		AmazonSQS amazonSqs = Mockito.mock(AmazonSQS.class);
		String queueUrl = "http://foo/bar";
		Mockito.when(amazonSqs.getQueueUrl(new GetQueueUrlRequest("foo"))).thenReturn(new GetQueueUrlResult().withQueueUrl(queueUrl));

		DynamicQueueDestinationResolver dynamicQueueDestinationResolver = new DynamicQueueDestinationResolver(amazonSqs);
		dynamicQueueDestinationResolver.setAutoCreate(false);
		Assert.assertEquals(queueUrl, dynamicQueueDestinationResolver.resolveDestinationName("foo"));
	}

	@Test
	public void testInvalidDestinationName() throws Exception {
		AmazonSQS amazonSqs = Mockito.mock(AmazonSQS.class);
		String queueUrl = "invalidName";
		Mockito.when(amazonSqs.getQueueUrl(new GetQueueUrlRequest(queueUrl))).thenThrow(new AmazonServiceException("AWS.SimpleQueueService.NonExistentQueue"));
		DynamicQueueDestinationResolver dynamicQueueDestinationResolver = new DynamicQueueDestinationResolver(amazonSqs);
		dynamicQueueDestinationResolver.setAutoCreate(false);
		try {
			dynamicQueueDestinationResolver.resolveDestinationName(queueUrl);
		} catch (InvalidDestinationException e) {
			Assert.assertEquals(queueUrl, e.getDestinationName());
		}
	}
}
