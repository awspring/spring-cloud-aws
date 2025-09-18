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
package io.awspring.cloud.sns.integration;

import java.io.Serial;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessagingException;
import software.amazon.awssdk.services.sns.model.PublishRequest;

/**
 * An exception that is the payload of an {@code ErrorMessage} when an SNS publish fails.
 *
 * @author Jacob Severson
 * @author Artem Bilan
 *
 * @since 4.0
 */
public class SnsRequestFailureException extends MessagingException {

	@Serial
	private static final long serialVersionUID = 1L;

	private final transient PublishRequest request;

	public SnsRequestFailureException(Message<?> message, PublishRequest request, Throwable cause) {
		super(message, cause);
		this.request = request;
	}

	public PublishRequest getRequest() {
		return this.request;
	}

	@Override
	public String toString() {
		return super.toString() + " [request=" + this.request + "]";
	}

}
