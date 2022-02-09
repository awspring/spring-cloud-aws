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

package io.awspring.cloud.sqs.sample;

import io.awspring.cloud.messaging.core.QueueMessagingTemplate;
import io.awspring.cloud.test.sqs.SqsTest;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.messaging.support.MessageBuilder;

import static org.awaitility.Awaitility.await;
import static org.mockito.Mockito.verify;

@SqsTest
class SqsSampleApplicationTests {

	@Autowired
	private QueueMessagingTemplate queueMessagingTemplate;

	@SpyBean
	private SampleListener sampleListener;

	@Test
	void foo() {
		queueMessagingTemplate.send("InfrastructureStack-spring-aws", MessageBuilder.withPayload("hello").build());

		await().untilAsserted(() -> verify(sampleListener).listenToMessage("hello"));
	}

}
