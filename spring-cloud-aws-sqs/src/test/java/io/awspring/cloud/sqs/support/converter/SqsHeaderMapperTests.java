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
package io.awspring.cloud.sqs.support.converter;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.messaging.MessageHeaders;
import software.amazon.awssdk.services.sqs.model.Message;

/**
 * @author Tomaz Fernandes
 * @since 3.0
 */
class SqsHeaderMapperTests {

	@Test
	void shouldAddExtraHeader() {
		SqsHeaderMapper headerMapper = new SqsHeaderMapper();
		String myHeader = "myHeader";
		String myValue = "myValue";
		headerMapper.setAdditionalHeadersFunction((message, accessor) -> {
			accessor.setHeader(myHeader, myValue);
			return accessor.toMessageHeaders();
		});
		Message message = Message.builder().body("payload").messageId(UUID.randomUUID().toString()).build();
		MessageHeaders headers = headerMapper.toHeaders(message);
		assertThat(headers.get(myHeader)).isEqualTo(myValue);
	}

}
