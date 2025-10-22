/*
 * Copyright 2013-2025 the original author or authors.
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
package io.awspring.cloud.kinesis.integration;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import org.springframework.core.AttributeAccessor;
import org.springframework.integration.support.ErrorMessageStrategy;
import org.springframework.integration.support.ErrorMessageUtils;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.ErrorMessage;

/**
 * The {@link ErrorMessageStrategy} implementation to build an {@link ErrorMessage} with the
 * {@link KinesisHeaders#RAW_RECORD} header as the value from the provided {@link AttributeAccessor}.
 *
 * @author Artem Bilan
 *
 * @since 4.0
 */
public class KinesisMessageHeaderErrorMessageStrategy implements ErrorMessageStrategy {

	@Override
	public ErrorMessage buildErrorMessage(Throwable throwable, AttributeAccessor context) {
		Object inputMessage = context == null ? null
				: context.getAttribute(ErrorMessageUtils.INPUT_MESSAGE_CONTEXT_KEY);

		Map<String, Object> headers = context == null ? new HashMap<>()
				: Collections.singletonMap(KinesisHeaders.RAW_RECORD, context.getAttribute(KinesisHeaders.RAW_RECORD));

		return new ErrorMessage(throwable, headers, inputMessage instanceof Message<?> message ? message : null);
	}

}
