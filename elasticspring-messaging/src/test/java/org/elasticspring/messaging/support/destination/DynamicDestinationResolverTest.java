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
public class DynamicDestinationResolverTest {

	@Test
	public void testAutoCreate() throws Exception {
		AmazonSQS amazonSQS = Mockito.mock(AmazonSQS.class);
		String queueUrl = "http://foo/bar";
		Mockito.when(amazonSQS.createQueue(new CreateQueueRequest("foo"))).thenReturn(new CreateQueueResult().withQueueUrl(queueUrl));

		DynamicDestinationResolver dynamicDestinationResolver = new DynamicDestinationResolver(amazonSQS);
		Assert.assertEquals(queueUrl, dynamicDestinationResolver.resolveDestinationName("foo"));
	}

	@Test
	public void testAbsoluteUrl() throws Exception {
		AmazonSQS amazonSQS = Mockito.mock(AmazonSQS.class);
		DynamicDestinationResolver dynamicDestinationResolver = new DynamicDestinationResolver(amazonSQS);
		String destination = "http://sqs-amazon.aws.com/123123123/myQueue";
		String queueUrl = dynamicDestinationResolver.resolveDestinationName(destination);
		Assert.assertEquals(destination, queueUrl);
	}

	@Test
	public void testNoAutoCreate() throws Exception {
		AmazonSQS amazonSQS = Mockito.mock(AmazonSQS.class);
		String queueUrl = "http://foo/bar";
		Mockito.when(amazonSQS.getQueueUrl(new GetQueueUrlRequest("foo"))).thenReturn(new GetQueueUrlResult().withQueueUrl(queueUrl));

		DynamicDestinationResolver dynamicDestinationResolver = new DynamicDestinationResolver(amazonSQS);
		dynamicDestinationResolver.setAutoCreate(false);
		Assert.assertEquals(queueUrl, dynamicDestinationResolver.resolveDestinationName("foo"));
	}

	@Test
	public void testInvalidDestinationName() throws Exception {
		AmazonSQS amazonSQS = Mockito.mock(AmazonSQS.class);
		String queueUrl = "invalidName";
		Mockito.when(amazonSQS.getQueueUrl(new GetQueueUrlRequest(queueUrl))).thenThrow(new AmazonServiceException("AWS.SimpleQueueService.NonExistentQueue"));
		DynamicDestinationResolver dynamicDestinationResolver = new DynamicDestinationResolver(amazonSQS);
		dynamicDestinationResolver.setAutoCreate(false);
		try {
			dynamicDestinationResolver.resolveDestinationName(queueUrl);
		} catch (InvalidDestinationException e) {
			Assert.assertEquals(queueUrl, e.getDestinationName());
		}
	}
}
