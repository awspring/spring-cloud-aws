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
package io.awspring.cloud.sns.sms;

import io.awspring.cloud.sns.core.HeaderConverter;
import java.util.HashMap;
import java.util.Map;
import org.apache.commons.logging.LogFactory;
import org.springframework.util.Assert;
import software.amazon.awssdk.services.sns.SnsClient;
import software.amazon.awssdk.services.sns.model.MessageAttributeValue;
import software.amazon.awssdk.services.sns.model.PublishRequest;

/**
 * Helper class that simplifies synchronous sending of sms messages to Mobile Phones or Platforms such as GCM and APNS.
 * The only mandatory fields are {@link SnsClient}.
 *
 * @author Matej Nedic
 * @since 3.0
 */
public class SnsSmsTemplate implements SnsSmsOperations {

	private final SnsClient snsClient;
	// Think about supporting List<Converter> to support multiple conversions.
	private final Converter converter;
	private final HeaderConverter headerConverter = new HeaderConverter(LogFactory.getLog(SnsSmsTemplate.class));

	public SnsSmsTemplate(SnsClient snsClient, Converter converter) {
		Assert.notNull(snsClient, "SnsClient must not be null");
		Assert.notNull(converter, "Converter must not be null");
		this.snsClient = snsClient;
		this.converter = converter;
	}

	@Override
	public void sendMessage(String destination, Object payload) {
		sendMessage(destination, payload, new HashMap<>());
	}

	@Override
	public void sendMessage(String destination, Object payload, Map<String, Object> headers) {
		PublishRequest.Builder request = resolveRequestByDestination(destination);
		request.message(converter.convert(payload));
		Map<String, MessageAttributeValue> messageAttributes = headerConverter.toSnsMessageAttributes(headers);
		if (!messageAttributes.isEmpty()) {
			request.messageAttributes(messageAttributes);
		}
		this.snsClient.publish(request.build());
	}

	private PublishRequest.Builder resolveRequestByDestination(String destination) {
		if (destination.startsWith("+")) {
			return PublishRequest.builder().phoneNumber(destination);
		}
		return PublishRequest.builder().targetArn(destination);
	}

}
