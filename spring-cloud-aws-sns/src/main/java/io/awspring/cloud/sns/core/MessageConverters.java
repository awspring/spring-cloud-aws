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
package io.awspring.cloud.sns.core;

import java.util.ArrayList;
import java.util.List;
import org.springframework.lang.Nullable;
import org.springframework.messaging.converter.CompositeMessageConverter;
import org.springframework.messaging.converter.MessageConverter;
import org.springframework.messaging.converter.StringMessageConverter;

/**
 * Configuration for Converters used by {@link SnsTemplate} and {@link io.awspring.cloud.sns.sms.SnsSmsTemplate}.
 *
 * @author Matej Nedic
 * @since 3.0
 */
public class MessageConverters {

	private MessageConverters() {
	}

	public static CompositeMessageConverter initMessageConverter(@Nullable MessageConverter messageConverter) {
		List<MessageConverter> converters = new ArrayList<>();

		StringMessageConverter stringMessageConverter = new StringMessageConverter();
		stringMessageConverter.setSerializedPayloadClass(String.class);
		converters.add(stringMessageConverter);

		if (messageConverter != null) {
			converters.add(messageConverter);
		}

		return new CompositeMessageConverter(converters);
	}
}
