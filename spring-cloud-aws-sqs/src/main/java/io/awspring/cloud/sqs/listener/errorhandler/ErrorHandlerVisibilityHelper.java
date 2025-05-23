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
package io.awspring.cloud.sqs.listener.errorhandler;

import io.awspring.cloud.sqs.MessageHeaderUtils;
import io.awspring.cloud.sqs.listener.BatchVisibility;
import io.awspring.cloud.sqs.listener.QueueMessageVisibility;
import io.awspring.cloud.sqs.listener.SqsHeaders;
import io.awspring.cloud.sqs.listener.Visibility;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.messaging.Message;

/**
 * Utility methods for Error Handler.
 *
 * @author Bruno Garcia
 * @author Rafael Pavarini
 */
public class ErrorHandlerVisibilityHelper {
	public static <T> Map<Long, List<Message<T>>> groupMessagesByReceiveMessageCount(Collection<Message<T>> messages) {
		return messages.stream().collect(Collectors.groupingBy(ErrorHandlerVisibilityHelper::getReceiveMessageCount));
	}

	public static <T> Collection<Message<?>> castMessages(Collection<Message<T>> messages) {
		return new ArrayList<>(messages);
	}

	public static <T> Visibility getVisibility(Message<T> message) {
		return MessageHeaderUtils.getHeader(message, SqsHeaders.SQS_VISIBILITY_TIMEOUT_HEADER, Visibility.class);
	}

	public static <T> BatchVisibility getVisibility(Collection<Message<T>> messages) {
		Collection<Message<?>> castMessages = ErrorHandlerVisibilityHelper.castMessages(messages);
		QueueMessageVisibility firstVisibilityMessage = (QueueMessageVisibility) ErrorHandlerVisibilityHelper
				.getVisibility(messages.iterator().next());
		return firstVisibilityMessage.toBatchVisibility(castMessages);

	}

	public static <T> long getReceiveMessageCount(Message<T> message) {
		return Long.parseLong(MessageHeaderUtils.getHeaderAsString(message,
				SqsHeaders.MessageSystemAttributes.SQS_APPROXIMATE_RECEIVE_COUNT));
	}
}
