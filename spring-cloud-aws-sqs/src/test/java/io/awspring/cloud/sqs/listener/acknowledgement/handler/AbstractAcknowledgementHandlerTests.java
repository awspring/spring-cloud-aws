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
package io.awspring.cloud.sqs.listener.acknowledgement.handler;

import static org.mockito.BDDMockito.given;

import io.awspring.cloud.sqs.listener.SqsHeaders;
import io.awspring.cloud.sqs.listener.acknowledgement.AcknowledgementCallback;
import java.util.Collection;
import java.util.Collections;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;

/**
 * @author Tomaz Fernandes
 * @since 3.0
 */
@ExtendWith(MockitoExtension.class)
public class AbstractAcknowledgementHandlerTests {

	@Mock
	protected Message<String> message;

	protected Collection<Message<String>> messages;

	@Mock
	protected AcknowledgementCallback<String> callback;

	@Mock
	protected MessageHeaders headers;

	protected UUID id = UUID.randomUUID();

	@Mock
	protected Throwable throwable;

	@BeforeEach
	void beforeEach() {
		given(message.getHeaders()).willReturn(headers);
		given(headers.get(SqsHeaders.SQS_MESSAGE_ID_HEADER, UUID.class)).willReturn(id);
		messages = Collections.singletonList(message);
	}

}
