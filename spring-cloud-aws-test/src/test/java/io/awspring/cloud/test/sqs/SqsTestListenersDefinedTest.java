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
package io.awspring.cloud.test.sqs;

import static io.awspring.cloud.test.sqs.SqsSampleListener.QUEUE_NAME;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.awaitility.Awaitility.await;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import software.amazon.awssdk.services.sqs.SqsAsyncClient;

@SqsTest(listeners = SqsSampleListener.class, properties = { "spring.cloud.aws.credentials.access-key=noop",
		"spring.cloud.aws.credentials.secret-key=noop", "spring.cloud.aws.region.static=eu-west-1" })
class SqsTestListenersDefinedTest extends BaseSqsIntegrationTest {

	@Autowired
	private ApplicationContext ctx;

	@Autowired
	private SqsAsyncClient sqsAsyncClient;

	@MockitoBean
	private SampleComponent sampleComponent;

	@Test
	void createsQueueMessagingTemplate() {
		assertThatNoException().isThrownBy(() -> this.ctx.getBean(SqsAsyncClient.class));
	}

	@Test
	void createsListener() {
		assertThatNoException().isThrownBy(() -> this.ctx.getBean(SqsSampleListener.class));
	}

	@Test
	void listenerHandlesMessage() {
		String queueUrl = sqsAsyncClient.getQueueUrl(r -> r.queueName(QUEUE_NAME)).join().queueUrl();
		sqsAsyncClient.sendMessage(r -> r.queueUrl(queueUrl).messageBody("message")).join();

		await().untilAsserted(() -> verify(sampleComponent).save("message"));
	}

}
